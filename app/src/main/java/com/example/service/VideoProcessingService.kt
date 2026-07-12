package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ConversionHistory
import com.example.data.ConversionRepository
import com.example.data.QueueItem
import com.example.processor.VideoProcessorEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VideoProcessingService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var conversionJob: Job? = null
    private var isQueueLoopRunning = false

    private lateinit var processorEngine: VideoProcessorEngine
    private lateinit var repository: ConversionRepository
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "offline_cartoon_ai_processing_channel"
        
        const val ACTION_START = "ACTION_START_CONVERSION"
        const val ACTION_CANCEL = "ACTION_CANCEL_CONVERSION"
        
        const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
        const val EXTRA_STYLE_NAME = "EXTRA_STYLE_NAME"
        const val EXTRA_QUALITY_MODE = "EXTRA_QUALITY_MODE"

        // Reactive progress variables observable from the UI ViewModels
        private val _processingProgress = MutableStateFlow(0f)
        val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

        private val _processingStatus = MutableStateFlow("Idle")
        val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

        private val _isProcessing = MutableStateFlow(false)
        val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

        private val _estimatedTimeRemainingSec = MutableStateFlow(0L)
        val estimatedTimeRemainingSec: StateFlow<Long> = _estimatedTimeRemainingSec.asStateFlow()

        private val _currentFrame = MutableStateFlow(0)
        val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

        private val _totalFrames = MutableStateFlow(0)
        val totalFrames: StateFlow<Int> = _totalFrames.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        processorEngine = VideoProcessorEngine(this)
        
        val database = AppDatabase.getDatabase(this)
        repository = ConversionRepository(database.conversionDao(), database.queueDao())

        createNotificationChannel()

        // Acquire a partial wake lock to keep the CPU running during render
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OfflineCartoonAI::ProcessorWakeLock").apply {
            acquire(30 * 60 * 1000L) // 30 mins max timeout safety
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START -> {
                val videoUriStr = intent.getStringExtra(EXTRA_VIDEO_URI)
                val styleName = intent.getStringExtra(EXTRA_STYLE_NAME) ?: "3D Cartoon"
                val qualityMode = intent.getStringExtra(EXTRA_QUALITY_MODE) ?: "BALANCED"

                serviceScope.launch {
                    if (videoUriStr != null) {
                        val videoUri = Uri.parse(videoUriStr)
                        val name = videoUri.lastPathSegment ?: "video.mp4"
                        val item = QueueItem(
                            videoUri = videoUriStr,
                            videoName = name,
                            styleName = styleName,
                            qualityMode = qualityMode,
                            status = "PENDING"
                        )
                        repository.insertQueueItem(item)
                    }
                    startForeground(NOTIFICATION_ID, createNotification("Starting conversion queue...", 0))
                    startQueueProcessingLoop()
                }
            }
            ACTION_CANCEL -> {
                cancelConversion()
            }
        }

        return START_STICKY
    }

    private fun startQueueProcessingLoop() {
        if (isQueueLoopRunning) return
        isQueueLoopRunning = true
        _isProcessing.value = true

        conversionJob = serviceScope.launch {
            while (isQueueLoopRunning) {
                val nextItem = repository.getNextPendingQueueItem()
                if (nextItem == null) {
                    _isProcessing.value = false
                    isQueueLoopRunning = false
                    stopForegroundAndComplete("All videos cartoonized successfully!")
                    break
                }
                processQueueItem(nextItem)
            }
        }
    }

    private suspend fun processQueueItem(item: QueueItem) {
        val processingItem = item.copy(status = "PROCESSING", progress = 0f)
        repository.updateQueueItem(processingItem)

        _processingProgress.value = 0f
        _processingStatus.value = "Processing: ${item.videoName}"
        _currentFrame.value = 0
        _totalFrames.value = 0
        _estimatedTimeRemainingSec.value = 0L

        updateNotification("Processing: ${item.videoName} (0%)", 0)

        val videoUri = Uri.parse(item.videoUri)
        val deferredResult = CompletableDeferred<String?>()

        processorEngine.convertVideoOffline(
            videoUri = videoUri,
            styleName = item.styleName,
            qualityMode = item.qualityMode,
            trimStartMs = item.trimStartMs,
            trimEndMs = item.trimEndMs,
            listener = object : VideoProcessorEngine.ProgressListener {
                override fun onProgress(current: Int, total: Int, percent: Float, estRemainingSec: Long) {
                    _currentFrame.value = current
                    _totalFrames.value = total
                    _processingProgress.value = percent
                    _estimatedTimeRemainingSec.value = estRemainingSec

                    serviceScope.launch(Dispatchers.IO) {
                        repository.updateQueueItem(processingItem.copy(progress = percent))
                    }

                    updateNotification(
                        "${item.videoName}: ${percent.toInt()}% (${current}/${total} frames)",
                        percent.toInt()
                    )
                }

                override fun onStatusUpdate(status: String) {
                    _processingStatus.value = "${item.videoName}: $status"
                }

                override fun onCompleted(outputPath: String) {
                    deferredResult.complete(outputPath)
                }

                override fun onError(error: String) {
                    deferredResult.completeExceptionally(Exception(error))
                }
            }
        )

        try {
            val outputPath = deferredResult.await()
            if (outputPath != null) {
                val file = if (outputPath.startsWith("content://")) null else File(outputPath)
                val sizeBytes = file?.length() ?: 0L
                
                val history = ConversionHistory(
                    originalUri = item.videoUri,
                    processedPath = outputPath,
                    styleName = item.styleName,
                    resolution = item.qualityMode,
                    durationMs = (_totalFrames.value * 33L).coerceAtLeast(1000L),
                    sizeBytes = sizeBytes
                )
                repository.insert(history)

                repository.updateQueueItem(processingItem.copy(status = "COMPLETED", progress = 100f))
            } else {
                repository.updateQueueItem(processingItem.copy(status = "FAILED", progress = 0f))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("VideoProcessingService", "Failed to convert item ${item.id}", e)
            repository.updateQueueItem(processingItem.copy(status = "FAILED", progress = 0f))
            kotlinx.coroutines.delay(2000)
        }
    }

    private fun cancelConversion() {
        _processingStatus.value = "Cancelled"
        _isProcessing.value = false
        isQueueLoopRunning = false
        conversionJob?.cancel()

        // Reset progress trackers
        _processingProgress.value = 0f
        _currentFrame.value = 0
        _totalFrames.value = 0
        _estimatedTimeRemainingSec.value = 0L

        serviceScope.launch(Dispatchers.IO) {
            val processingItem = repository.getProcessingQueueItem()
            if (processingItem != null) {
                repository.updateQueueItem(processingItem.copy(status = "FAILED"))
            }
            repository.clearQueue()
        }

        stopForegroundAndComplete("Conversion was cancelled.")
    }

    private fun stopForegroundAndComplete(message: String) {
        // Release wake lock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Offline Cartoon AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        manager.notify(1002, notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(content: String, progress: Int): Notification {
        val cancelIntent = Intent(this, VideoProcessingService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converting Video completely offline")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun updateNotification(content: String, progress: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(content, progress))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Cartoonizer Core Processor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress for offline video styles conversions"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
    }
}
