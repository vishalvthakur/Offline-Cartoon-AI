package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VideoViewModel
import com.example.ui.components.ProcessingProgressBar
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex

@Composable
fun ProcessingScreen(
    viewModel: VideoViewModel,
    onCompleted: (String) -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val status by viewModel.status.collectAsState()
    val estTimeRemainingSec by viewModel.estTimeRemainingSec.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val totalFrames by viewModel.totalFrames.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val queueList by viewModel.queueList.collectAsState()

    val selectedStyle by viewModel.selectedStyle.collectAsState()

    val context = LocalContext.current
    var showSuccessOverlay by remember { mutableStateOf(false) }

    // Trigger redirection to ResultScreen upon successful rendering completion
    LaunchedEffect(isProcessing, status) {
        if (!isProcessing && status == "Completed!") {
            showSuccessOverlay = true
            // Native Toast notification
            Toast.makeText(context, "Styling completed successfully offline!", Toast.LENGTH_LONG).show()

            // Display success animation for 2.2 seconds before navigating
            kotlinx.coroutines.delay(2200)

            val latestItem = historyList.firstOrNull()
            if (latestItem != null) {
                onCompleted(latestItem.processedPath)
            } else {
                onCancelled()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Offline Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = "Offline AI Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PROCESSING COMPLETELY OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animating between Processing State and Success Completion State
            AnimatedContent(
                targetState = showSuccessOverlay,
                transitionSpec = {
                    (scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))) togetherWith
                    (scaleOut(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)))
                },
                label = "ProgressSuccessTransition"
            ) { isSuccess ->
                if (isSuccess) {
                    // Success Completion Animation View
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Outer glowing / breathing halo ring
                        var pulseScale by remember { mutableStateOf(0.8f) }
                        LaunchedEffect(Unit) {
                            pulseScale = 1.0f
                        }
                        val animatedHaloScale by animateFloatAsState(
                            targetValue = pulseScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "HaloSpring"
                        )

                        // Outer success ring
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .size(200.dp)
                                .scale(animatedHaloScale),
                            strokeWidth = 10.dp,
                            color = Color(0xFF10B981), // Beautiful emerald green
                            trackColor = Color(0xFF10B981).copy(alpha = 0.08f)
                        )

                        // Center content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.scale(animatedHaloScale)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier
                                    .size(84.dp)
                                    .testTag("completion_success_icon")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "STYLIZED!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                    // Dual Progress Visualization
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(200.dp),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${progress.toInt()}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedStyle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Linear Progress Bar Feedback Card
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProcessingProgressBar(
                        progress = progress,
                        statusText = status,
                        currentFrame = currentFrame,
                        totalFrames = totalFrames
                    )

                    // Time estimate calculation
                    val timeStr = remember(estTimeRemainingSec) {
                        if (estTimeRemainingSec <= 0) {
                            "Estimating remaining time..."
                        } else if (estTimeRemainingSec < 60) {
                            "Est. Time Remaining: ${estTimeRemainingSec}s"
                        } else {
                            val mins = estTimeRemainingSec / 60
                            val secs = estTimeRemainingSec % 60
                            "Est. Time Remaining: ${mins}m ${secs}s"
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Estimated time remaining",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Secure Device Guarantee Note
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Safe On-Device Processing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Because processing runs locally on your CPU/GPU delegate, this can cause the device to heat up slightly. You can minimize the app or turn off the screen; conversion will continue safely in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (queueList.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Conversion Queue (${queueList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        queueList.forEach { queueItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = queueItem.videoName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${queueItem.styleName} • ${queueItem.qualityMode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (queueItem.status) {
                                        "PROCESSING" -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${queueItem.progress.toInt()}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        "PENDING" -> {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Pending",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "QUEUED",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        "COMPLETED" -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "DONE",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        "FAILED" -> {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = "Failed",
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "FAILED",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFF87171),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Let users remove items from queue if pending/completed/failed
                                    if (queueItem.status == "PENDING" || queueItem.status == "COMPLETED" || queueItem.status == "FAILED") {
                                        IconButton(
                                            onClick = { viewModel.deleteQueueItem(queueItem) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel Button
            Button(
                onClick = {
                    viewModel.cancelConversion()
                    onCancelled()
                },
                shape = RoundedCornerShape(50.dp), // Pill shaped button for Geometric Balance
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_button")
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel")
                    Text(
                        text = "CANCEL CONVERSION",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Floating Custom In-App Notification Toast Banner
        AnimatedVisibility(
            visible = showSuccessOverlay,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(10f)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981) // Emerald Green
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("success_toast_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conversion Complete!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Your cartoon video is ready for preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
