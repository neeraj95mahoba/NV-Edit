package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CanvasBackgroundSettings
import com.example.model.CanvasBgMode
import com.example.model.MusicVisualizerStyle
import com.example.model.TimelineClip
import com.example.model.VideoAnimCombo
import com.example.model.VideoAnimIn
import com.example.model.VideoAnimOut
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProGold
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VideoAnimationProDrawer(
    selectedClip: TimelineClip?,
    onUpdateAnimation: (VideoAnimIn?, VideoAnimOut?, VideoAnimCombo?, Long?) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var subTab by remember { mutableIntStateOf(0) } // 0 = In, 1 = Out, 2 = Combo

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Animation, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Video Animation Engine", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(palette.background)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("In", "Out", "Combo").forEachIndexed { idx, label ->
                val active = subTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (active) ElectricPurple else Color.Transparent)
                        .clickable { subTab = idx }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("video_anim_subtab_$label")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Color.White else palette.textSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    if (selectedClip == null) {
        Text("Add or select a video/image clip on the timeline to animate it.", style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
        return
    }

    when (subTab) {
        0 -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(VideoAnimIn.entries) { anim ->
                    val active = selectedClip.animIn == anim
                    AnimChip(
                        label = anim.label,
                        active = active,
                        accent = NeonCyan,
                        tag = "video_anim_in_${anim.name}"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateAnimation(anim, null, null, null)
                    }
                }
            }
        }
        1 -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(VideoAnimOut.entries) { anim ->
                    val active = selectedClip.animOut == anim
                    AnimChip(
                        label = anim.label,
                        active = active,
                        accent = ProGold,
                        tag = "video_anim_out_${anim.name}"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateAnimation(null, anim, null, null)
                    }
                }
            }
        }
        else -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(VideoAnimCombo.entries) { anim ->
                    val active = selectedClip.animCombo == anim
                    AnimChip(
                        label = anim.label,
                        active = active,
                        accent = ElectricPurple,
                        tag = "video_anim_combo_${anim.name}"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateAnimation(null, null, anim, null)
                    }
                }
            }
        }
    }

    val durSec = selectedClip.animDurationMs / 1000f
    CompactProSlider(
        label = "Animation Duration",
        value = durSec,
        range = 0.2f..3.0f,
        valueLabel = String.format(Locale.US, "%.1fs", durSec)
    ) { sec ->
        onUpdateAnimation(null, null, null, (sec * 1000f).toLong())
    }
}

@Composable
fun CanvasAndClipToolsDrawer(
    selectedClip: TimelineClip?,
    canvasBg: CanvasBackgroundSettings,
    musicVisualizer: MusicVisualizerStyle,
    onFreezeFrame: () -> Unit,
    onToggleReverse: () -> Unit,
    onRotate90: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onUpdateCropZoom: (Float) -> Unit,
    onUpdateCanvasBg: ((CanvasBackgroundSettings) -> CanvasBackgroundSettings) -> Unit,
    onPickCustomCanvasBg: () -> Unit,
    onUpdateMusicVisualizer: (MusicVisualizerStyle) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Clip Transform, Freeze & Canvas BG", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
    }

    // Essential 1-Tap Clip Operations (Freeze 3s, Reverse, Rotate 90, Flip H, Flip V)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        item {
            QuickClipToolPill(
                label = "Freeze 3s",
                icon = Icons.Filled.AcUnit,
                active = selectedClip?.isFreezeFrame == true,
                accent = NeonCyan,
                tag = "clip_tool_freeze"
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFreezeFrame()
            }
        }
        item {
            QuickClipToolPill(
                label = if (selectedClip?.isReversed == true) "Reversed" else "Reverse",
                icon = Icons.Filled.History,
                active = selectedClip?.isReversed == true,
                accent = ProGold,
                tag = "clip_tool_reverse"
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleReverse()
            }
        }
        item {
            val deg = (selectedClip?.rotationQuarterTurns ?: 0) * 90
            QuickClipToolPill(
                label = if (deg > 0) "Rot ${deg}°" else "Rotate 90°",
                icon = Icons.Filled.Rotate90DegreesCw,
                active = deg > 0,
                accent = ElectricPurple,
                tag = "clip_tool_rotate"
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRotate90()
            }
        }
        item {
            QuickClipToolPill(
                label = "Mirror H",
                icon = Icons.Filled.Flip,
                active = selectedClip?.flipHorizontal == true,
                accent = AcidLime,
                tag = "clip_tool_flip_h"
            ) {
                onToggleFlipH()
            }
        }
        item {
            QuickClipToolPill(
                label = "Mirror V",
                icon = Icons.Filled.Flip,
                active = selectedClip?.flipVertical == true,
                accent = AcidLime,
                tag = "clip_tool_flip_v"
            ) {
                onToggleFlipV()
            }
        }
    }

    if (selectedClip != null) {
        CompactProSlider(
            label = "Crop & Frame Scale (Reveal Canvas BG < 1.0x)",
            value = selectedClip.cropZoom,
            range = 0.6f..2.2f,
            valueLabel = "${(selectedClip.cropZoom * 100).roundToInt()}%",
            onChange = onUpdateCropZoom
        )
    }

    Text("Canvas Background", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(CanvasBgMode.entries) { mode ->
            val active = canvasBg.mode == mode
            AnimChip(
                label = mode.label,
                active = active,
                accent = NeonCyan,
                tag = "canvas_bg_${mode.name}"
            ) {
                onUpdateCanvasBg { it.copy(mode = mode) }
                if (mode == CanvasBgMode.CUSTOM_IMAGE) onPickCustomCanvasBg()
            }
        }
    }

    if (canvasBg.mode == CanvasBgMode.SOLID) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0xFF0A0A0DL, 0xFFF5F6FAL, 0xFF1E1038L, 0xFF042A2BL, 0xFF3A0CA3L, 0xFFFF3366L).forEach { hex ->
                val selected = canvasBg.solidColorHex == hex
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(if (selected) 2.dp else 1.dp, if (selected) ProGold else palette.border, CircleShape)
                        .clickable { onUpdateCanvasBg { it.copy(solidColorHex = hex) } }
                )
            }
        }
    } else if (canvasBg.mode == CanvasBgMode.GRADIENT) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Studio", "Cyber", "Sunset", "Emerald").forEachIndexed { idx, name ->
                val active = canvasBg.gradientIndex == idx
                AnimChip(name, active, ProGold, "canvas_grad_$idx") {
                    onUpdateCanvasBg { it.copy(gradientIndex = idx) }
                }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = AcidLime, modifier = Modifier.size(15.dp))
        Text("Music Visualizer Overlay", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(MusicVisualizerStyle.entries) { viz ->
            AnimChip(
                label = viz.label,
                active = musicVisualizer == viz,
                accent = AcidLime,
                tag = "music_viz_${viz.name}"
            ) {
                onUpdateMusicVisualizer(viz)
            }
        }
    }
}

@Composable
private fun AnimChip(
    label: String,
    active: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (active) accent.copy(alpha = 0.24f) else palette.surfaceElevated,
        border = BorderStroke(if (active) 1.4.dp else 1.dp, if (active) accent else palette.border),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) palette.textPrimary else palette.textSecondary,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun QuickClipToolPill(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent.copy(alpha = 0.24f) else palette.surfaceElevated,
        border = BorderStroke(1.dp, if (active) accent else palette.border),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (active) accent else palette.textSecondary, modifier = Modifier.size(15.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) palette.textPrimary else palette.textSecondary,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
