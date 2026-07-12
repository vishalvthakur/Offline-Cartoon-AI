package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimRangeSliderComponent(
    videoDurationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimRangeChanged: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videoDurationMs <= 0) return

    val maxVal = videoDurationMs.toFloat()
    
    // Local slider state to ensure ultra-smooth dragging
    var sliderRange by remember(videoDurationMs, trimStartMs, trimEndMs) {
        val start = trimStartMs.toFloat().coerceIn(0f, maxVal)
        val end = if (trimEndMs <= 0f) maxVal else trimEndMs.toFloat().coerceIn(0f, maxVal)
        mutableStateOf(start..end)
    }

    val trimmedDurationMs = remember(sliderRange.start, sliderRange.endInclusive) {
        (sliderRange.endInclusive - sliderRange.start).toLong().coerceAtLeast(0L)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trim_range_slider_card")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon, Title, and Trimmed Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Cut",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Trim Video Length",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Trimmed duration label
                Text(
                    text = "Trimmed: ${"%.1f".format(trimmedDurationMs / 1000f)}s",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Time Marker Indicators
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "START TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMs(sliderRange.start.toLong()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "END TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMs(sliderRange.endInclusive.toLong()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Material 3 Range Slider
            RangeSlider(
                value = sliderRange,
                onValueChange = { range ->
                    // Prevent start/end from being identical (minimum 1 second trim width)
                    val minGapMs = 1000f
                    if (range.endInclusive - range.start >= minGapMs) {
                        sliderRange = range
                        onTrimRangeChanged(range.start.toLong(), range.endInclusive.toLong())
                    }
                },
                valueRange = 0f..maxVal,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trim_range_slider")
            )

            // Slider drag hints
            Text(
                text = "Drag the handles above to adjust the start and end points of your video to trim before rendering.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 13.sp
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val msRemainder = (ms % 1000) / 100
    return "%02d:%02d.%d".format(mins, secs, msRemainder)
}
