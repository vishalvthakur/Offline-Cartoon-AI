package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VideoViewModel
import com.example.ui.components.VideoPreviewComponent
import com.example.ui.components.TrimRangeSliderComponent

data class StyleItem(val name: String, val icon: ImageVector, val desc: String, val tint: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: VideoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedVideoUri by viewModel.selectedVideoUri.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val trimStartMs by viewModel.trimStartMs.collectAsState()
    val trimEndMs by viewModel.trimEndMs.collectAsState()

    val videoName by viewModel.videoName.collectAsState()
    val videoResolution by viewModel.videoResolution.collectAsState()
    val videoSizeMb by viewModel.videoSizeMb.collectAsState()
    val videoDurationMs by viewModel.videoDurationMs.collectAsState()

    val scrollState = rememberScrollState()

    val styles = remember {
        listOf(
            StyleItem("3D Cartoon", Icons.Default.Face, "Bouncy 3D cartoon contours", Color(0xFFE040FB)),
            StyleItem("Pixar", Icons.Default.MovieFilter, "Cinematic 3D animation look", Color(0xFFFF5252)),
            StyleItem("Anime", Icons.Default.AutoAwesome, "Vibrant soft anime look", Color(0xFF00E676)),
            StyleItem("Comic", Icons.Default.MenuBook, "Heavily posterized pop art lines", Color(0xFF29B6F6)),
            StyleItem("Sketch", Icons.Default.Gesture, "Monochrome charcoal outline", Color(0xFF78909C)),
            StyleItem("Oil Painting", Icons.Default.Brush, "Classical painterly stroke brushwork", Color(0xFFFFB74D)),
            StyleItem("Watercolor", Icons.Default.Palette, "Soft faded bleed colors", Color(0xFFAB47BC)),
            StyleItem("Cel Shading", Icons.Default.Layers, "Retro vector game outlines", Color(0xFFFFD54F))
        )
    }

    val durationSec = remember(videoDurationMs) { videoDurationMs / 1000 }
    val sizeMbStr = remember(videoSizeMb) { "%.1f MB".format(videoSizeMb) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configure Style",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetSelection()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4ADE80), shape = RoundedCornerShape(50.dp))
                        )
                        Text(
                            text = "OFFLINE AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Video Source Info Box with integrated Offline Player Preview
            selectedVideoUri?.let { uri ->
                VideoPreviewComponent(
                    videoUri = uri,
                    videoName = videoName,
                    videoResolution = videoResolution,
                    sizeMbStr = sizeMbStr,
                    durationSec = durationSec
                )

                Spacer(modifier = Modifier.height(12.dp))

                TrimRangeSliderComponent(
                    videoDurationMs = videoDurationMs,
                    trimStartMs = trimStartMs,
                    trimEndMs = trimEndMs,
                    onTrimRangeChanged = { start, end ->
                        viewModel.setTrimRange(start, end)
                    }
                )
            }

            // Section 1: Style Swatches Carousel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Select Cartoon Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(styles) { item ->
                        val isSelected = selectedStyle == item.name
                        StyleSwatchCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = { viewModel.selectStyle(item.name) }
                        )
                    }
                }
            }

            // Section 2: Quality Modes
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. Render Quality Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                QualitySelectCard(
                    mode = "FAST",
                    title = "FAST MODE",
                    desc = "480p processing • Optimized for speed and low-end hardware.",
                    isSelected = selectedQuality == "FAST",
                    onClick = { viewModel.selectQuality("FAST") }
                )

                QualitySelectCard(
                    mode = "BALANCED",
                    title = "BALANCED MODE",
                    desc = "720p processing • Smooth balanced textures and standard timing.",
                    isSelected = selectedQuality == "BALANCED",
                    onClick = { viewModel.selectQuality("BALANCED") }
                )

                QualitySelectCard(
                    mode = "HIGH QUALITY",
                    title = "HIGH QUALITY MODE",
                    desc = "1080p processing • Ultra precise outlines and deep contrast canvas. Requires substantial device RAM.",
                    isSelected = selectedQuality == "HIGH QUALITY",
                    onClick = { viewModel.selectQuality("HIGH QUALITY") }
                )
            }

            // Long video warnings
            if (durationSec > 15) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Long video warning: Processing videos longer than 15s completely on-device takes multiple minutes depending on device hardware.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Conversion Trigger Action Button
            Button(
                onClick = {
                    viewModel.startConversion()
                    onNavigateToProcessing()
                },
                shape = RoundedCornerShape(50.dp), // Pill shaped button for Geometric Balance
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("convert_button")
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = "Convert Offline")
                    Text(
                        text = "CONVERT VIDEO OFFLINE",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StyleSwatchCard(
    item: StyleItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .width(130.dp)
            .height(145.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = item.tint,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.desc,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun QualitySelectCard(
    mode: String,
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
