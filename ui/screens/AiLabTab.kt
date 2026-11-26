package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StoryboardScene
import com.example.network.NetworkStatus
import com.example.ui.components.AiLabMode
import com.example.ui.components.NetworkResilienceBanner
import com.example.ui.components.springPressClickable
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProGold
import com.example.ui.theme.WarmAmber

private data class AiGeneratorCardSpec(
    val mode: AiLabMode,
    val title: String,
    val subtitle: String,
    val tag: String,
    val icon: ImageVector,
    val accent: Color,
    val adBadge: String? = null
)

private val aiGeneratorCards = listOf(
    AiGeneratorCardSpec(
        mode = AiLabMode.TEXT_TO_VIDEO,
        title = "Text → Video",
        subtitle = "Prompt to multi-scene animated 4K storyboard timeline with camera motion",
        tag = "DIRECTOR",
        icon = Icons.Filled.MovieCreation,
        accent = NeonCyan,
        adBadge = "3 Ads"
    ),
    AiGeneratorCardSpec(
        mode = AiLabMode.TEXT_TO_IMAGE,
        title = "Text → Photo / Image",
        subtitle = "Generate ultra-crisp AI studio artwork, covers & B-roll frames",
        tag = "IMAGEN",
        icon = Icons.Filled.Image,
        accent = ElectricPurple,
        adBadge = "2 Ads"
    ),
    AiGeneratorCardSpec(
        mode = AiLabMode.SCRIPT_TO_VIDEO,
        title = "Script → Video",
        subtitle = "Turn Hindi or English scripts into timed captioned Reel scenes",
        tag = "HI / EN",
        icon = Icons.Filled.Subtitles,
        accent = ProGold
    ),
    AiGeneratorCardSpec(
        mode = AiLabMode.PHOTO_ANIMATE,
        title = "Photo Animate",
        subtitle = "Bring any still photo to life with AI Ken Burns & dolly motion",
        tag = "MOTION",
        icon = Icons.Filled.PlayCircleFilled,
        accent = AcidLime
    ),
    AiGeneratorCardSpec(
        mode = AiLabMode.LYRICS_VISUALIZER,
        title = "Lyrics Visualizer",
        subtitle = "Beat-synced kinetic typography & audio spectrum music reels",
        tag = "AUDIO FX",
        icon = Icons.Filled.MusicNote,
        accent = MagentaSpark
    ),
    AiGeneratorCardSpec(
        mode = AiLabMode.TALKING_PROMO,
        title = "Talking & Promo Ad",
        subtitle = "High-converting product ad & talking presenter video builder",
        tag = "ADS PRO",
        icon = Icons.Filled.Campaign,
        accent = WarmAmber
    )
)

@Composable
fun AiLabTab(
    networkStatus: NetworkStatus,
    scenes: List<StoryboardScene>,
    onToggleLowDataMode: () -> Unit,
    onOpenAiLab: (AiLabMode) -> Unit,
    onSaveScene: (StoryboardScene) -> Unit,
    onToggleFilmed: (StoryboardScene) -> Unit,
    onMoveScene: (index: Int, delta: Int) -> Unit,
    onDeleteScene: (String) -> Unit,
    onConvertToTimeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var activeSegment by rememberSaveable { mutableIntStateOf(0) } // 0 = AI Generators, 1 = Storyboards

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Sleek Segmented Glass Switcher: AI Lab Generators vs Shot Storyboards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeSegment == 0) "AI Studio Lab" else "Shot Storyboard",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeSegment == 0) {
                        "6 live generative tools • Works on low-network Lite Mode"
                    } else {
                        "Plan shots & build multi-track timelines in 1 tap"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Frosted-Glass Segmented Pill Toggle
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.glassBorderBrush),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("AI Generators (6)", "Shot Storyboards (${scenes.size})").forEachIndexed { idx, label ->
                    val selected = activeSegment == idx
                    val bg by animateColorAsState(
                        targetValue = if (selected) palette.accentPrimary.copy(alpha = 0.24f) else Color.Transparent,
                        label = "aiLabSegBg"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = if (selected) BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.55f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeSegment = idx }
                            .testTag("ai_lab_segment_$idx")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) palette.accentSecondary else palette.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (activeSegment == 0) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ai_lab_generators_grid"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (networkStatus.isOffline || networkStatus.useLiteOptimization) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NetworkResilienceBanner(
                            networkStatus = networkStatus,
                            onToggleLowDataMode = onToggleLowDataMode
                        )
                    }
                }

                // Featured Hero AI Prompt Banner
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.glassBorderBrush),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            ElectricPurple.copy(alpha = 0.24f),
                                            NeonCyan.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.85f, size.height * 0.4f),
                                        radius = size.width * 0.6f
                                    )
                                )
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .springPressClickable(hapticType = HapticFeedbackType.LongPress) {
                                onOpenAiLab(AiLabMode.TEXT_TO_VIDEO)
                            }
                            .testTag("ai_lab_featured_hero")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ElectricPurple.copy(alpha = 0.35f), NeonCyan.copy(alpha = 0.28f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Create with AI Director",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Generate scenes, images, voice scripts & beat cuts right onto your timeline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = palette.accentSecondary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "Launch AI Director",
                                    tint = palette.accentSecondary,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }
                }

                items(aiGeneratorCards, key = { it.mode.name }) { spec ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.glassBorderBrush),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .springPressClickable { onOpenAiLab(spec.mode) }
                            .testTag("ai_lab_card_${spec.mode.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(spec.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = spec.icon,
                                        contentDescription = spec.title,
                                        tint = spec.accent,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    spec.adBadge?.let { badgeText ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ProGold.copy(alpha = 0.20f),
                                            border = BorderStroke(1.dp, ProGold.copy(alpha = 0.7f)),
                                            modifier = Modifier.testTag("ai_lab_card_ad_badge_${spec.mode.name}")
                                        ) {
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ProGold,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = spec.accent.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, spec.accent.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = spec.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = spec.accent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = spec.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            StoryboardsTab(
                scenes = scenes,
                onSaveScene = onSaveScene,
                onToggleFilmed = onToggleFilmed,
                onMoveScene = onMoveScene,
                onDeleteScene = onDeleteScene,
                onConvertToTimeline = onConvertToTimeline,
                embedded = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
