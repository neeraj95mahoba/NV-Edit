package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AutoCaptionStyle
import com.example.model.ClipTransition
import com.example.model.ColorWheelTint
import com.example.model.CutoutBackgroundType
import com.example.model.CutoutMode
import com.example.model.CutoutSettings
import com.example.model.PipBlendMode
import com.example.model.PipMaskShape
import com.example.model.TimelineClip
import com.example.model.TransitionType
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AutoCaptionsProDrawer(
    speechTranscript: String,
    onSpeechTranscriptChange: (String) -> Unit,
    onLaunchVoiceDictation: (localeTag: String) -> Unit,
    onGenerateCaptions: (script: String, style: AutoCaptionStyle, wordsPerClip: Int) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var selectedStyle by remember { mutableStateOf(AutoCaptionStyle.HORMOZI) }
    var wordsPerClip by remember { mutableIntStateOf(3) }
    var dictationLocale by remember { mutableStateOf("en-US") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Subtitles, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Auto-Captions (EN/हिन्दी)", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clickable {
                        dictationLocale = if (dictationLocale == "en-US") "hi-IN" else "en-US"
                    }
                    .testTag("auto_captions_lang_toggle")
            ) {
                Text(
                    text = if (dictationLocale == "hi-IN") "हिन्दी" else "EN",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricPurple.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunchVoiceDictation(dictationLocale)
                    }
                    .testTag("auto_captions_mic_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Dictate speech", tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dictate", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }
    }

    // 4 Pro Caption Presets
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AutoCaptionStyle.entries) { style ->
            val active = style == selectedStyle
            val badge = Color(style.badgeHex)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) badge.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) badge else palette.border),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedStyle = style
                    }
                    .testTag("caption_style_${style.name}")
            ) {
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }

    OutlinedTextField(
        value = speechTranscript,
        onValueChange = onSpeechTranscriptChange,
        placeholder = { Text("Paste English or Hindi script, or tap Dictate…", style = MaterialTheme.typography.bodySmall) },
        maxLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProGold,
            unfocusedBorderColor = palette.border,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auto_captions_script_input")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Words/Cut:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            listOf(2, 3, 4).forEach { count ->
                val active = wordsPerClip == count
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (active) ProGold.copy(alpha = 0.25f) else StudioSurfaceElevated,
                    border = BorderStroke(1.dp, if (active) ProGold else StudioBorder),
                    modifier = Modifier.clickable { wordsPerClip = count }
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) ProGold else TextSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ProGoldGradient)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGenerateCaptions(speechTranscript, selectedStyle, wordsPerClip)
                }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("generate_auto_captions_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OledBlack, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Generate Timed Subtitles",
                    style = MaterialTheme.typography.labelSmall,
                    color = OledBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CutoutAndChromaDrawer(
    cutout: CutoutSettings,
    onUpdateCutout: ((CutoutSettings) -> CutoutSettings) -> Unit,
    onPickCustomBackground: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Colorize, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Smart BG Remove & Chroma Key", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
    }

    // Mode Selector (Off / Chroma Key / Smart Auto-Cutout)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CutoutMode.entries.forEach { mode ->
            val active = cutout.mode == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) ElectricPurple.copy(alpha = 0.28f) else StudioSurfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateCutout { it.copy(mode = mode) }
                    }
                    .testTag("cutout_mode_${mode.name}")
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) NeonCyan else TextSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    if (cutout.mode == CutoutMode.CHROMA_KEY) {
        Text("Eyedropper Key Color", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        val eyedropperSwatches = listOf(
            0xFF00B140L to "Green",
            0xFF0047BBL to "Blue",
            0xFF0E0E14L to "Black",
            0xFFFFFFFFL to "White",
            0xFFD90368L to "Magenta",
            0xFF00C8FFL to "Cyan"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(eyedropperSwatches) { (hex, name) ->
                val selected = cutout.keyColorHex == hex
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) ProGold else StudioBorder),
                    modifier = Modifier
                        .clickable { onUpdateCutout { it.copy(keyColorHex = hex) } }
                        .testTag("chroma_swatch_$name")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
            }
        }
    }

    if (cutout.mode != CutoutMode.OFF) {
        CompactProSlider("Intensity / Tolerance", cutout.intensity, 0.05f..1f, "${(cutout.intensity * 100).roundToInt()}%") { v ->
            onUpdateCutout { it.copy(intensity = v) }
        }
        CompactProSlider("Shadow & Edge Feather", cutout.shadowEdge, 0f..1f, "${(cutout.shadowEdge * 100).roundToInt()}%") { v ->
            onUpdateCutout { it.copy(shadowEdge = v) }
        }

        Text("Replace Background", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CutoutBackgroundType.entries) { bg ->
                val active = cutout.bgType == bg
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) NeonCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                    modifier = Modifier
                        .clickable {
                            onUpdateCutout { it.copy(bgType = bg) }
                            if (bg == CutoutBackgroundType.CUSTOM_MEDIA) onPickCustomBackground()
                        }
                        .testTag("cutout_bg_${bg.name}")
                ) {
                    Text(
                        text = bg.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) NeonCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (cutout.bgType == CutoutBackgroundType.SOLID_COLOR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0xFF1A1038L, 0xFF002B36L, 0xFF3A0CA3L, 0xFF0A0A0AL, 0xFFF6F6FAL, 0xFF00B140L).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (cutout.bgSolidColorHex == hex) 2.dp else 1.dp,
                                color = if (cutout.bgSolidColorHex == hex) ProGold else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { onUpdateCutout { it.copy(bgSolidColorHex = hex) } }
                    )
                }
            }
        } else if (cutout.bgType == CutoutBackgroundType.NEON_GRADIENT) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cyber", "Vapor", "Sunset", "Emerald").forEachIndexed { idx, label ->
                    val active = cutout.bgGradientIndex == idx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) ElectricPurple.copy(alpha = 0.28f) else StudioSurfaceElevated,
                        border = BorderStroke(1.dp, if (active) ProGold else StudioBorder),
                        modifier = Modifier.clickable { onUpdateCutout { it.copy(bgGradientIndex = idx) } }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProTransitionsDrawer(
    activeTransition: ClipTransition,
    hasMultipleClips: Boolean,
    onSelectTransition: (TransitionType) -> Unit,
    onDurationChange: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val categories = remember { listOf("All", "Camera", "Glitch", "Light", "Slide", "Spin", "Mask", "Basic") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == "All") TransitionType.entries
        else TransitionType.entries.filter { it.category == selectedCategory || it == TransitionType.NONE }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pro Transitions (${TransitionType.entries.size - 1}+)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Text(
            text = activeTransition.type.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan
        )
    }

    if (!hasMultipleClips) {
        Text(
            text = "Tip: Split a video clip or add a 2nd scene on the Video track to preview transitions between cuts.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(categories) { cat ->
            val active = cat == selectedCategory
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) ElectricPurple.copy(alpha = 0.26f) else StudioSurfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                modifier = Modifier.clickable { selectedCategory = cat }
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) NeonCyan else TextSecondary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered) { trans ->
            val active = activeTransition.type == trans
            val accent = Color(trans.accentHex)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) accent.copy(alpha = 0.25f) else StudioSurfaceElevated,
                border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) ProGold else StudioBorder),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectTransition(trans)
                    }
                    .testTag("transition_item_${trans.name}")
            ) {
                Text(
                    text = trans.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) Color.White else TextSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }

    val durSec = activeTransition.durationMs / 1000f
    CompactProSlider(
        label = "Transition Duration",
        value = durSec,
        range = 0.2f..2.0f,
        valueLabel = String.format(Locale.US, "%.1fs", durSec)
    ) { sec ->
        onDurationChange((sec * 1000f).toLong())
    }
}

@Composable
fun PipAndMaskingProDrawer(
    activePip: TimelineClip?,
    onImportPipMedia: () -> Unit,
    onUpdatePip: ((TimelineClip) -> TimelineClip) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Layers, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("PIP Blend Modes & Masks", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ElectricPurple.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, NeonCyan),
            modifier = Modifier
                .clickable(onClick = onImportPipMedia)
                .testTag("pip_drawer_import_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Overlay", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            }
        }
    }

    val currentBlend = activePip?.pipBlendMode ?: PipBlendMode.NORMAL
    val currentMask = activePip?.pipMaskShape ?: PipMaskShape.RECTANGLE

    Text("Blend Mode", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PipBlendMode.entries) { blend ->
            val active = currentBlend == blend
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) MagentaSpark.copy(alpha = 0.24f) else StudioSurfaceElevated,
                border = BorderStroke(1.dp, if (active) MagentaSpark else StudioBorder),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdatePip { it.copy(pipBlendMode = blend) }
                    }
                    .testTag("pip_blend_${blend.name}")
            ) {
                Text(
                    text = blend.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    Text("Shape Mask & Split-Screen", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PipMaskShape.entries) { mask ->
            val active = currentMask == mask
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) NeonCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdatePip { it.copy(pipMaskShape = mask) }
                    }
                    .testTag("pip_mask_${mask.name}")
            ) {
                Text(
                    text = mask.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) NeonCyan else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    CompactProSlider("Overlay Size", activePip?.pipScale ?: 0.42f, 0.18f..0.95f, "${((activePip?.pipScale ?: 0.42f) * 100).roundToInt()}%") { v ->
        onUpdatePip { it.copy(pipScale = v) }
    }
    CompactProSlider("Overlay Opacity", activePip?.pipOpacity ?: 1f, 0.1f..1f, "${((activePip?.pipOpacity ?: 1f) * 100).roundToInt()}%") { v ->
        onUpdatePip { it.copy(pipOpacity = v) }
    }
    CompactProSlider("Position X", activePip?.pipOffsetX ?: 0.22f, -0.45f..0.45f, "${((activePip?.pipOffsetX ?: 0.22f) * 100).roundToInt()}%") { v ->
        onUpdatePip { it.copy(pipOffsetX = v) }
    }
    CompactProSlider("Position Y", activePip?.pipOffsetY ?: -0.22f, -0.45f..0.45f, "${((activePip?.pipOffsetY ?: -0.22f) * 100).roundToInt()}%") { v ->
        onUpdatePip { it.copy(pipOffsetY = v) }
    }
}

@Composable
fun InteractiveColorWheel(
    label: String,
    tint: ColorWheelTint,
    onChange: (ColorWheelTint) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.dp, StudioBorder, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChange(offsetToWheelTint(offset, size.width.toFloat(), size.height.toFloat()))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onChange(offsetToWheelTint(change.position, size.width.toFloat(), size.height.toFloat()))
                    }
                }
                .testTag("color_wheel_$label")
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                alpha = 0.72f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A22), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            val rad = Math.toRadians(tint.hueDeg.toDouble())
            val puckR = (tint.strength.coerceIn(0f, 1f)) * (radius * 0.82f)
            val puck = Offset(
                x = center.x + (cos(rad) * puckR).toFloat(),
                y = center.y + (sin(rad) * puckR).toFloat()
            )
            drawCircle(color = OledBlack, radius = 6.dp.toPx(), center = puck)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = puck)
        }
    }
}

private fun offsetToWheelTint(pos: Offset, width: Float, height: Float): ColorWheelTint {
    val cx = width / 2f
    val cy = height / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val maxR = (minOf(width, height) / 2f).coerceAtLeast(1f)
    val dist = (hypot(dx.toDouble(), dy.toDouble()).toFloat() / maxR).coerceIn(0f, 1f)
    val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
    return ColorWheelTint(hueDeg = hue, strength = if (dist < 0.12f) 0f else dist)
}

@Composable
internal fun CompactProSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = palette.accentSecondary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentSecondary,
                activeTrackColor = palette.accentPrimary,
                inactiveTrackColor = palette.surfaceElevated
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
