package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberVideoThumbnail(videoUri: Uri?): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, videoUri) {
        if (videoUri != null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, videoUri)
                    // Retrieve frame around 1 sec or fallback to 0
                    val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } else {
            value = null
        }
    }.value
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewComponent(
    videoUri: Uri,
    videoName: String,
    videoResolution: String,
    sizeMbStr: String,
    durationSec: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbnail = rememberVideoThumbnail(videoUri)
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }

    // ExoPlayer state
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Clean up or re-initialize player
    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Video Player/Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    // Lazy-load ExoPlayer on demand when User hits Play
                    val player = remember(videoUri) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(videoUri))
                            prepare()
                            playWhenReady = true
                            repeatMode = Player.REPEAT_MODE_ONE
                            volume = if (isMuted) 0f else 1f
                        }
                    }
                    exoPlayer = player

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false // Use our custom beautiful UI overlays
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Control Overlays
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Top row controllers (Mute/Unmute)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    player.volume = if (isMuted) 0f else 1f
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Center Play/Pause button
                        IconButton(
                            onClick = {
                                isPlaying = false
                                player.pause()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Preview",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Bottom Info Pill
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = "Preview Playing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Show Thumbnail with custom Play button
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "Source Video Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Elegant Placeholder Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Source Video",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Play button overlay
                    IconButton(
                        onClick = { isPlaying = true },
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Black.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            .testTag("preview_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Preview",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Info details bottom row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Source Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = videoName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "$videoResolution • $sizeMbStr • ${durationSec}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
