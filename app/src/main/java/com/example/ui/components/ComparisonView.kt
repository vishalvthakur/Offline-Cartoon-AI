package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun ComparisonView(
    originalUri: Uri,
    processedUri: Uri,
    modifier: Modifier = Modifier,
    onShowOriginalChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    // Media3 ExoPlayer Instance 1: Original
    val originalPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    // Media3 ExoPlayer Instance 2: Processed
    val processedPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    var showOriginal by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0.5f) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    // Reactively update player sources when URIs change
    LaunchedEffect(originalUri) {
        originalPlayer.setMediaItem(MediaItem.fromUri(originalUri))
        originalPlayer.prepare()
    }

    LaunchedEffect(processedUri) {
        processedPlayer.setMediaItem(MediaItem.fromUri(processedUri))
        processedPlayer.prepare()
    }

    // Keep the two players perfectly in sync
    LaunchedEffect(originalPlayer, processedPlayer, isPlaying) {
        while (isPlaying) {
            val origPos = originalPlayer.currentPosition
            val procPos = processedPlayer.currentPosition
            if (kotlin.math.abs(origPos - procPos) > 100) {
                processedPlayer.seekTo(origPos)
            }
            currentPositionMs = origPos
            val d = originalPlayer.duration
            if (d > 0) {
                durationMs = d
            }
            kotlinx.coroutines.delay(100)
        }
    }

    // Release ExoPlayers on disposal
    DisposableEffect(Unit) {
        onDispose {
            originalPlayer.release()
            processedPlayer.release()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Draggable Split Video Player Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val widthPx = size.width.toFloat()
                            if (widthPx > 0f) {
                                sliderPosition = (sliderPosition + dragAmount.x / widthPx).coerceIn(0f, 1f)
                                val origVal = sliderPosition == 1f
                                if (showOriginal != origVal) {
                                    showOriginal = origVal
                                    onShowOriginalChanged(origVal)
                                }
                            }
                        }
                    }
            ) {
                val width = maxWidth

                // 1. Base Layer: Processed (Cartoon) video (Full Width)
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = processedPlayer
                            useController = false
                            setShowSubtitleButton(false)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Overlapping Layer: Original video (Sized to sliderPosition * width, clipToBounds)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(width * sliderPosition)
                        .clipToBounds()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = originalPlayer
                                useController = false
                                setShowSubtitleButton(false)
                            }
                        },
                        modifier = Modifier
                            .requiredWidth(width)
                            .fillMaxHeight()
                    )
                }

                // 3. Draggable Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = width * sliderPosition - 1.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )

                // 4. Draggable Divider Circular Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = width * sliderPosition - 20.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⬌",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // 5. Comparison Labels Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ORIGINAL",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CARTOON",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Custom Playback Controls Row
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play/Pause Button
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        originalPlayer.playWhenReady = isPlaying
                        processedPlayer.playWhenReady = isPlaying
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Progress Slider
                Slider(
                    value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                    onValueChange = { ratio ->
                        val targetMs = (ratio * durationMs).toLong()
                        originalPlayer.seekTo(targetMs)
                        processedPlayer.seekTo(targetMs)
                        currentPositionMs = targetMs
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Position / Duration Text
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 80.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        // Interactive Before/After Toggle Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    showOriginal = !showOriginal
                    sliderPosition = if (showOriginal) 1f else 0.5f
                    onShowOriginalChanged(showOriginal)
                },
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showOriginal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (showOriginal) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("before_after_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (showOriginal) Icons.Default.VisibilityOff else Icons.Default.Compare,
                        contentDescription = "Compare"
                    )
                    Text(
                        text = if (showOriginal) "SHOW SPLIT COMPARISON" else "VIEW FULL ORIGINAL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return String.format("%02d:%02d", minutes, seconds)
}
