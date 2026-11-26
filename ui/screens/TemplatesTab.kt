package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.LocalStudioAdController
import com.example.model.StudioCatalog
import com.example.model.TrackType
import com.example.model.VideoTemplate
import com.example.ui.components.NativeMediumRectangleAdCard
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TemplatesTab(
    onUseTemplate: (VideoTemplate) -> Unit,
    onUseTemplateWithMedia: (VideoTemplate, List<Uri>) -> Unit = { tpl, _ -> onUseTemplate(tpl) },
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val adController = LocalStudioAdController.current
    val adState by adController.state.collectAsStateWithLifecycle()
    val categories = remember { listOf("All", "Trending", "Cinematic", "Lyrical", "3D Zoom", "Vlog", "Retro") }
    var selectedCategory by remember { mutableStateOf("All") }
    var customizingTemplate by remember { mutableStateOf<VideoTemplate?>(null) }

    val templateMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        val target = customizingTemplate
        if (target != null && uris.isNotEmpty()) {
            customizingTemplate = null
            onUseTemplateWithMedia(target, uris)
            adController.showMediaImportInterstitialOncePerSession("Template Photo-to-Video")
        }
    }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == "All") StudioCatalog.templates
        else StudioCatalog.templates.filter { it.category == selectedCategory }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 166.dp),
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Viral Templates Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = ProGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ProGold.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = "100% FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap any template, pick your photos/videos & auto-sync to the beat instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val active = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (active) palette.accentPrimary.copy(alpha = 0.22f) else palette.surface,
                            border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("template_filter_$cat")
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) palette.accentSecondary else palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Render Templates with a 300x250 Native / Medium Rectangle Ad Card after every 4 templates
        val adInterval = adState.config.nativeCardIntervalInTemplates.coerceAtLeast(1)
        val templateChunks = filtered.chunked(adInterval)
        templateChunks.forEachIndexed { chunkIndex, chunk ->
            items(chunk, key = { it.id }) { template ->
                TemplateShowcaseCard(
                    template = template,
                    onUse = { customizingTemplate = template }
                )
            }
            if (chunk.size == adInterval) {
                item(
                    key = "templates_native_ad_$chunkIndex",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    NativeMediumRectangleAdCard(
                        placementTag = "templates_feed_${chunkIndex + 1}"
                    )
                }
            }
        }
    }

    customizingTemplate?.let { template ->
        val slotCount = remember(template.id) {
            template.createDocument().clips.count {
                it.trackType == TrackType.MAIN_VIDEO || it.trackType == TrackType.OVERLAY_PIP
            }.coerceAtLeast(1)
        }
        Dialog(onDismissRequest = { customizingTemplate = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.glassBorderBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_customizer_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$slotCount Photo/Video Slot${if (slotCount > 1) "s" else ""} • ${template.bpm} BPM • ${template.aspectRatio.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                        IconButton(
                            onClick = { customizingTemplate = null },
                            modifier = Modifier.testTag("template_dialog_close")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close template dialog", tint = palette.textSecondary)
                        }
                    }

                    Text(
                        text = template.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonStudioGradient)
                            .clickable {
                                templateMediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .testTag("template_pick_my_media_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = OledBlack,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "Pick My Photos/Videos & Auto-Sync",
                                style = MaterialTheme.typography.labelLarge,
                                color = OledBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                customizingTemplate = null
                                onUseTemplate(template)
                            }
                            .testTag("template_open_demo_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Open Demo Template First",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateShowcaseCard(
    template: VideoTemplate,
    onUse: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val accent = Color(template.accentHex)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.glassBorderBrush),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_card_${template.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clickable(onClick = onUse)
            ) {
                Image(
                    painter = painterResource(id = StudioCatalog.drawableForKey(template.drawableKey)),
                    contentDescription = stringResource(R.string.cd_template_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    OledBlack.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Top Row: Aspect + BPM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OledBlack.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = template.aspectRatio.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OledBlack.copy(alpha = 0.78f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${template.bpm} BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Bottom Overlay Metadata
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = template.usesLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = template.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tags + One-Tap "Use Template" Button
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    template.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.surfaceElevated
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonStudioGradient)
                        .clickable(onClick = onUse)
                        .testTag("use_template_${template.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = OledBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_use_template),
                            style = MaterialTheme.typography.labelLarge,
                            color = OledBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
