package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ConversionHistory
import com.example.data.ConversionRepository
import com.example.data.QueueItem
import com.example.service.VideoProcessingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ConversionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ConversionRepository(database.conversionDao(), database.queueDao())
    }

    // Recent conversions reactively fetched from Room database
    val historyList: StateFlow<List<ConversionHistory>> = repository.allConversions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current conversion queue reactively fetched from Room database
    val queueList: StateFlow<List<QueueItem>> = repository.allQueueItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selection state
    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _selectedStyle = MutableStateFlow("3D Cartoon")
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _selectedQuality = MutableStateFlow("BALANCED")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    // Video metadata state
    private val _videoDurationMs = MutableStateFlow(0L)
    val videoDurationMs: StateFlow<Long> = _videoDurationMs.asStateFlow()

    private val _videoSizeMb = MutableStateFlow(0.0)
    val videoSizeMb: StateFlow<Double> = _videoSizeMb.asStateFlow()

    private val _videoResolution = MutableStateFlow("")
    val videoResolution: StateFlow<String> = _videoResolution.asStateFlow()

    private val _videoName = MutableStateFlow("")
    val videoName: StateFlow<String> = _videoName.asStateFlow()

    // Active conversion session metadata bridged from VideoProcessingService
    val isProcessing: StateFlow<Boolean> = VideoProcessingService.isProcessing
    val progress: StateFlow<Float> = VideoProcessingService.processingProgress
    val status: StateFlow<String> = VideoProcessingService.processingStatus
    val estTimeRemainingSec: StateFlow<Long> = VideoProcessingService.estimatedTimeRemainingSec
    val currentFrame: StateFlow<Int> = VideoProcessingService.currentFrame
    val totalFrames: StateFlow<Int> = VideoProcessingService.totalFrames

    fun selectVideo(uri: Uri) {
        _selectedVideoUri.value = uri
        loadVideoMetadata(uri)
    }

    fun selectStyle(style: String) {
        _selectedStyle.value = style
    }

    fun selectQuality(quality: String) {
        _selectedQuality.value = quality
    }

    private fun loadVideoMetadata(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                _videoDurationMs.value = duration

                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                _videoResolution.value = if (width != null && height != null) "${width}x${height}" else "Unknown"

                // Extract file size using content resolver
                var sizeBytes = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                        if (nameIndex != -1) _videoName.value = cursor.getString(nameIndex)
                    }
                }

                if (_videoName.value.isEmpty()) {
                    _videoName.value = uri.lastPathSegment ?: "video.mp4"
                }

                _videoSizeMb.value = sizeBytes / (1024.0 * 1024.0)
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Failed to extract video metadata", e)
                _videoName.value = "Selected Video"
                _videoResolution.value = "720x1280"
                _videoSizeMb.value = 0.0
                _videoDurationMs.value = 0L
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }

    fun startConversion() {
        val uri = _selectedVideoUri.value ?: return
        val style = _selectedStyle.value
        val quality = _selectedQuality.value
        val name = _videoName.value.ifEmpty { "Selected Video" }

        viewModelScope.launch {
            val item = QueueItem(
                videoUri = uri.toString(),
                videoName = name,
                styleName = style,
                qualityMode = quality,
                status = "PENDING"
            )
            repository.insertQueueItem(item)

            val context = getApplication<Application>()
            val serviceIntent = Intent(context, VideoProcessingService::class.java).apply {
                action = VideoProcessingService.ACTION_START
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    fun deleteQueueItem(item: QueueItem) {
        viewModelScope.launch {
            repository.deleteQueueItem(item.id)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun clearCompletedQueue() {
        viewModelScope.launch {
            repository.clearCompletedQueue()
        }
    }

    fun cancelConversion() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, VideoProcessingService::class.java).apply {
            action = VideoProcessingService.ACTION_CANCEL
        }
        context.startService(serviceIntent)
    }

    fun deleteHistoryItem(item: ConversionHistory) {
        viewModelScope.launch {
            // Delete actual file if stored locally (as a cache/temp file)
            if (!item.processedPath.startsWith("content://")) {
                try {
                    val file = File(item.processedPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("VideoViewModel", "Failed to delete physical video file", e)
                }
            }
            repository.delete(item.id)
        }
    }

    fun resetSelection() {
        _selectedVideoUri.value = null
        _videoName.value = ""
        _videoResolution.value = ""
        _videoSizeMb.value = 0.0
        _videoDurationMs.value = 0L
    }
}
