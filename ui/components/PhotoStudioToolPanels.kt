package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorGrading
import com.example.model.DslrApertureStop
import com.example.model.LutPreset
import com.example.model.PhotoCanvasInteractionTool
import com.example.model.PhotoStudioSettings
import com.example.model.PhotoStudioToolTab
import com.example.model.PhotoStudioUiState
import com.example.model.PhotoUpscaleMultiplier
import com.example.model.RadialMaskSettings
import com.example.model.SkyReplacementPreset
import com.example.model.defaultHslBands
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PhotoStudioToolPanelContent(
    uiState: PhotoStudioUiState,
    isAdSequenceRunning: Boolean,
    onSelectMultiplier: (PhotoUpscaleMultiplier) -> Unit,
    onApplyDslrProLook: () -> Unit,
    onApplyAutoRawTone: () -> Unit,
    onUpdateSettings: ((PhotoStudioSettings) -> PhotoStudioSettings) -> Unit,
    onUpdateColorGrading: ((ColorGrading) -> ColorGrading) -> Unit,
    onSelectInteractionTool: (PhotoCanvasInteractionTool) -> Unit,
    onClearBrushAndHeals: () -> Unit
) {
    when (uiState.activeToolTab) {
        PhotoStudioToolTab.AI_UPSCALE_DSLR -> AiDslrToolPanel(
            uiState = uiState,
            isAdSequenceRunning = isAdSequenceRunning,
            onSelectMultiplier = onSelectMultiplier,
            onApplyDslrProLook = onApplyDslrProLook,
            onUpdateSettings = onUpdateSettings,
            onSelectFocusTool = { onSelectInteractionTool(PhotoCanvasInteractionTool.REFOCUS_OR_SPLIT) }
        )
        PhotoStudioToolTab.RAW_TONE -> RawToneToolPanel(
            settings = uiState.settings,
            onApplyAutoRawTone = onApplyAutoRawTone,
            onUpdateSettings = onUpdateSettings,
            onUpdateColorGrading = onUpdateColorGrading
        )
        PhotoStudioToolTab.CURVES_HSL -> CurvesHslToolPanel(
            grading = uiState.settings.colorGrading,
            onUpdateColorGrading = onUpdateColorGrading
        )
        PhotoStudioToolTab.RETOUCH_SKY -> RetouchSkyToolPanel(
            uiState = uiState,
            onUpdateSettings = onUpdateSettings,
            onUpdateColorGrading = onUpdateColorGrading,
            onSelectInteractionTool = onSelectInteractionTool,
            onClearBrushAndHeals = onClearBrushAndHeals
        )
        PhotoStudioToolTab.SELECTIVE_MASK -> SelectiveMaskToolPanel(
            uiState = uiState,
            onUpdateSettings = onUpdateSettings,
            onSelectInteractionTool = onSelectInteractionTool,
            onClearBrushAndHeals = onClearBrushAndHeals
        )
    }
}

@Composable
private fun AiDslrToolPanel(
    uiState: PhotoStudioUiState,
    isAdSequenceRunning: Boolean,
    onSelectMultiplier: (PhotoUpscaleMultiplier) -> Unit,
    onApplyDslrProLook: () -> Unit,
    onUpdateSettings: ((PhotoStudioSettings) -> PhotoStudioSettings) -> Unit,
    onSelectFocusTool: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    val settings = uiState.settings
    var pendingLockedMultiplier by remember { mutableStateOf<PhotoUpscaleMultiplier?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1-Tap DSLR Pro Look Hero Action
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onApplyDslrProLook()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricPurple,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("photo_dslr_pro_look_button")
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "1-Tap DSLR Pro Look (Face Restore + f/1.8 + HDR)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // AI Super-Resolution Upscaler (1x, 2x, 4x, 8x [1 Ad], 10x [2 Ads])
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.Hd, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = "AI Super-Resolution Upscaler",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = settings.upscaleMultiplier.detailLabel,
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(PhotoUpscaleMultiplier.entries) { mult ->
                val active = settings.upscaleMultiplier == mult
                val isUnlocked = mult in uiState.unlockedMultipliers
                val badge = if (!isUnlocked) mult.adBadgeLabel else null
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) ElectricPurple.copy(alpha = 0.36f) else palette.surfaceElevated,
                    border = BorderStroke(
                        width = if (active) 1.5.dp else 1.dp,
                        color = when {
                            active -> NeonCyan
                            badge != null -> ProGold.copy(alpha = 0.6f)
                            else -> palette.border
                        }
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAdSequenceRunning) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isUnlocked) {
                                pendingLockedMultiplier = null
                                onSelectMultiplier(mult)
                            } else {
                                pendingLockedMultiplier = mult
                            }
                        }
                        .testTag("photo_upscale_${mult.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = mult.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) NeonCyan else if (badge != null) ProGold else palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = ProGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, ProGold.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("photo_upscale_ad_badge_${mult.name}")
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rewarded Ad CTA when user selects locked 8x (1 Ad) or 10x (2 Ads)
        pendingLockedMultiplier?.takeIf { it !in uiState.unlockedMultipliers }?.let { locked ->
            Button(
                onClick = { onSelectMultiplier(locked) },
                enabled = !isAdSequenceRunning,
                colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = OledBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photo_unlock_upscale_cta_${locked.name}")
            ) {
                Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(locked.ctaLabel, fontWeight = FontWeight.Bold)
            }
        }

        // DSLR Optical Aperture Bokeh (f/1.2 to f/8.0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.Camera, contentDescription = null, tint = ProGold, modifier = Modifier.size(15.dp))
                Text(
                    text = "DSLR Optical Bokeh",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Tap photo to move focus",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier.clickable(onClick = onSelectFocusTool)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(DslrApertureStop.entries) { stop ->
                val active = settings.apertureStop == stop
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (active) ProGold.copy(alpha = 0.22f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateSettings { it.copy(apertureStop = stop) }
                        }
                        .testTag("photo_aperture_${stop.name}")
                ) {
                    Text(
                        text = stop.fStopLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) ProGold else palette.textSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        CompactProSlider(
            label = "AI Face & Skin Restoration",
            value = settings.faceRestoration,
            range = 0f..1f,
            valueLabel = "${(settings.faceRestoration * 100).roundToInt()}%"
        ) { v -> onUpdateSettings { it.copy(faceRestoration = v) } }

        CompactProSlider(
            label = "AI Auto-Bright & Dynamic HDR",
            value = settings.autoBrightHdr,
            range = 0f..1f,
            valueLabel = "${(settings.autoBrightHdr * 100).roundToInt()}%"
        ) { v -> onUpdateSettings { it.copy(autoBrightHdr = v) } }

        CompactProSlider(
            label = "AI Denoise & Micro-Sharpen",
            value = settings.denoiseMicroSharpen,
            range = 0f..1f,
            valueLabel = "${(settings.denoiseMicroSharpen * 100).roundToInt()}%"
        ) { v -> onUpdateSettings { it.copy(denoiseMicroSharpen = v) } }
    }
}

@Composable
private fun RawToneToolPanel(
    settings: PhotoStudioSettings,
    onApplyAutoRawTone: () -> Unit,
    onUpdateSettings: ((PhotoStudioSettings) -> PhotoStudioSettings) -> Unit,
    onUpdateColorGrading: ((ColorGrading) -> ColorGrading) -> Unit
) {
    val palette = LocalStudioPalette.current
    val g = settings.colorGrading

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Tonality, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = "RAW 16-Bit Color Depth",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeonCyan.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onApplyAutoRawTone)
                    .testTag("photo_ai_auto_raw_button")
            ) {
                Text(
                    text = "AI Auto Tone",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        CompactProSlider("Exposure", g.exposure, -1f..1f, formatSigned(g.exposure)) { v -> onUpdateColorGrading { it.copy(exposure = v) } }
        CompactProSlider("Contrast", g.contrast, -1f..1f, formatSigned(g.contrast)) { v -> onUpdateColorGrading { it.copy(contrast = v) } }
        CompactProSlider("Highlights", g.highlights, -1f..1f, formatSigned(g.highlights)) { v -> onUpdateColorGrading { it.copy(highlights = v) } }
        CompactProSlider("Shadows", g.shadows, -1f..1f, formatSigned(g.shadows)) { v -> onUpdateColorGrading { it.copy(shadows = v) } }
        CompactProSlider("Whites", settings.whites, -1f..1f, formatSigned(settings.whites)) { v -> onUpdateSettings { it.copy(whites = v) } }
        CompactProSlider("Blacks", settings.blacks, -1f..1f, formatSigned(settings.blacks)) { v -> onUpdateSettings { it.copy(blacks = v) } }
        CompactProSlider("Texture", settings.texture, 0f..1f, "${(settings.texture * 100).roundToInt()}%") { v -> onUpdateSettings { it.copy(texture = v) } }
        CompactProSlider("Clarity", settings.clarity, 0f..1f, "${(settings.clarity * 100).roundToInt()}%") { v -> onUpdateSettings { it.copy(clarity = v) } }
        CompactProSlider("Dehaze", settings.dehaze, 0f..1f, "${(settings.dehaze * 100).roundToInt()}%") { v -> onUpdateSettings { it.copy(dehaze = v) } }
        CompactProSlider("Vibrance", settings.vibrance, -1f..1f, formatSigned(settings.vibrance)) { v -> onUpdateSettings { it.copy(vibrance = v) } }
        CompactProSlider("Saturation", g.saturation, -1f..1f, formatSigned(g.saturation)) { v -> onUpdateColorGrading { it.copy(saturation = v) } }
        CompactProSlider("Temperature (Warm / Cool)", g.temperature, -1f..1f, formatSigned(g.temperature)) { v -> onUpdateColorGrading { it.copy(temperature = v) } }
        CompactProSlider("Tint (Green / Magenta)", g.tint, -1f..1f, formatSigned(g.tint)) { v -> onUpdateColorGrading { it.copy(tint = v) } }
    }
}

@Composable
private fun CurvesHslToolPanel(
    grading: ColorGrading,
    onUpdateColorGrading: ((ColorGrading) -> ColorGrading) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Cinema Film & Pro Photo LUTs (${LutPreset.entries.size})",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(LutPreset.entries) { lut ->
                val active = lut == grading.lutPreset
                val swatch = Color(lut.badgeColorHex)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) swatch.copy(alpha = 0.25f) else palette.surfaceElevated,
                    border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateColorGrading { it.copy(lutPreset = lut) } }
                        .testTag("photo_lut_${lut.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(swatch))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = lut.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) palette.textPrimary else palette.textSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (grading.lutPreset != LutPreset.ORIGINAL) {
            CompactProSlider(
                label = "LUT Strength",
                value = grading.filterIntensity,
                range = 0f..1f,
                valueLabel = "${(grading.filterIntensity * 100).roundToInt()}%"
            ) { v -> onUpdateColorGrading { it.copy(filterIntensity = v) } }
        }

        RgbToneCurveEditor(
            grading = grading,
            onUpdate = onUpdateColorGrading
        )

        HslColorMixerPanel(
            bands = grading.hslBands,
            onUpdateBand = { bandIdx, transform ->
                onUpdateColorGrading { g ->
                    val base = (if (g.hslBands.size >= 8) g.hslBands else defaultHslBands()).toMutableList()
                    if (bandIdx in base.indices) base[bandIdx] = transform(base[bandIdx])
                    g.copy(hslBands = base)
                }
            }
        )
    }
}

@Composable
private fun RetouchSkyToolPanel(
    uiState: PhotoStudioUiState,
    onUpdateSettings: ((PhotoStudioSettings) -> PhotoStudioSettings) -> Unit,
    onUpdateColorGrading: ((ColorGrading) -> ColorGrading) -> Unit,
    onSelectInteractionTool: (PhotoCanvasInteractionTool) -> Unit,
    onClearBrushAndHeals: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val settings = uiState.settings

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Interactive Canvas Retouch Brushes: Blemish Remover + Magic Object Eraser
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.FaceRetouchingNatural, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp))
                Text(
                    text = "Pro Portrait Retouch & Magic Eraser",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (settings.blemishSpots.isNotEmpty() || settings.magicEraseStrokes.isNotEmpty()) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onClearBrushAndHeals)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val blemishActive = uiState.activeInteractionTool == PhotoCanvasInteractionTool.BLEMISH_HEAL
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (blemishActive) AcidLime.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (blemishActive) AcidLime else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectInteractionTool(PhotoCanvasInteractionTool.BLEMISH_HEAL) }
                    .testTag("photo_tool_blemish_heal")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = if (blemishActive) AcidLime else palette.textSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Blemish Tap (${settings.blemishSpots.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (blemishActive) AcidLime else palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val eraserActive = uiState.activeInteractionTool == PhotoCanvasInteractionTool.MAGIC_ERASER
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (eraserActive) CoralRed.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (eraserActive) CoralRed else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectInteractionTool(PhotoCanvasInteractionTool.MAGIC_ERASER) }
                    .testTag("photo_tool_magic_eraser")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Brush, contentDescription = null, tint = if (eraserActive) CoralRed else palette.textSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Magic Eraser (${settings.magicEraseStrokes.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (eraserActive) CoralRed else palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        CompactProSlider("Teeth Whitener", settings.teethWhiten, 0f..1f, "${(settings.teethWhiten * 100).roundToInt()}%") { v ->
            onUpdateSettings { it.copy(teethWhiten = v) }
        }
        CompactProSlider("Skin Smoother", settings.colorGrading.beauty.skinSmooth, 0f..1f, "${(settings.colorGrading.beauty.skinSmooth * 100).roundToInt()}%") { v ->
            onUpdateColorGrading { it.copy(beauty = it.beauty.copy(skinSmooth = v)) }
        }
        CompactProSlider("Face Glow", settings.colorGrading.beauty.faceGlow, 0f..1f, "${(settings.colorGrading.beauty.faceGlow * 100).roundToInt()}%") { v ->
            onUpdateColorGrading { it.copy(beauty = it.beauty.copy(faceGlow = v)) }
        }

        // AI Sky Replacement
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
            Text(
                text = "AI Sky Replacement",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(SkyReplacementPreset.entries) { preset ->
                val active = settings.skyPreset == preset
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateSettings { it.copy(skyPreset = preset) } }
                        .testTag("photo_sky_${preset.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(preset.topColorHex), Color(preset.horizonColorHex))
                                    )
                                )
                        )
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) NeonCyan else palette.textSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (settings.skyPreset != SkyReplacementPreset.NONE) {
            CompactProSlider("Sky Blend Intensity", settings.skyIntensity, 0f..1f, "${(settings.skyIntensity * 100).roundToInt()}%") { v ->
                onUpdateSettings { it.copy(skyIntensity = v) }
            }
        }
    }
}

@Composable
private fun SelectiveMaskToolPanel(
    uiState: PhotoStudioUiState,
    onUpdateSettings: ((PhotoStudioSettings) -> PhotoStudioSettings) -> Unit,
    onSelectInteractionTool: (PhotoCanvasInteractionTool) -> Unit,
    onClearBrushAndHeals: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val settings = uiState.settings
    val radial = settings.radialMask

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selective Brush Masking (Paint on Photo)",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (settings.selectiveStrokes.isNotEmpty()) {
                Text(
                    text = "Clear (${settings.selectiveStrokes.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onClearBrushAndHeals)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val faceActive = uiState.activeInteractionTool == PhotoCanvasInteractionTool.BRUSH_BRIGHTEN_FACE
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (faceActive) ProGold.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (faceActive) ProGold else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectInteractionTool(PhotoCanvasInteractionTool.BRUSH_BRIGHTEN_FACE) }
                    .testTag("photo_brush_brighten_face")
            ) {
                Text(
                    text = "☀ Brighten Face",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (faceActive) ProGold else palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                )
            }

            val bgActive = uiState.activeInteractionTool == PhotoCanvasInteractionTool.BRUSH_DARKEN_BG
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (bgActive) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (bgActive) NeonCyan else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectInteractionTool(PhotoCanvasInteractionTool.BRUSH_DARKEN_BG) }
                    .testTag("photo_brush_darken_bg")
            ) {
                Text(
                    text = "☾ Darken Background",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bgActive) NeonCyan else palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                )
            }
        }

        CompactProSlider("Face Brighten Strength", settings.selectiveFaceBrightenStrength, 0f..1f, "${(settings.selectiveFaceBrightenStrength * 100).roundToInt()}%") { v ->
            onUpdateSettings { it.copy(selectiveFaceBrightenStrength = v) }
        }
        CompactProSlider("Background Darken Strength", settings.selectiveBgDarkenStrength, 0f..1f, "${(settings.selectiveBgDarkenStrength * 100).roundToInt()}%") { v ->
            onUpdateSettings { it.copy(selectiveBgDarkenStrength = v) }
        }

        // Radial Masking Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated,
            border = BorderStroke(1.dp, if (radial.enabled) NeonCyan.copy(alpha = 0.65f) else palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Radial Spotlight Mask",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (radial.invertMask) "Inverted: Affecting Background" else "Inside: Affecting Subject/Face",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = radial.enabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(radialMask = it.radialMask.copy(enabled = enabled)) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = OledBlack, checkedTrackColor = NeonCyan),
                        modifier = Modifier.testTag("photo_radial_mask_switch")
                    )
                }

                if (radial.enabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!radial.invertMask) ProGold.copy(alpha = 0.22f) else palette.surface,
                            border = BorderStroke(1.dp, if (!radial.invertMask) ProGold else palette.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdateSettings {
                                        it.copy(radialMask = it.radialMask.copy(invertMask = false, exposureDelta = kotlin.math.abs(it.radialMask.exposureDelta)))
                                    }
                                }
                        ) {
                            Text(
                                text = "Brighten Subject",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!radial.invertMask) ProGold else palette.textSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (radial.invertMask) ElectricPurple.copy(alpha = 0.28f) else palette.surface,
                            border = BorderStroke(1.dp, if (radial.invertMask) NeonCyan else palette.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdateSettings {
                                        it.copy(radialMask = it.radialMask.copy(invertMask = true, exposureDelta = -kotlin.math.abs(it.radialMask.exposureDelta).coerceAtLeast(0.30f)))
                                    }
                                }
                        ) {
                            Text(
                                text = "Darken BG (Invert)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (radial.invertMask) NeonCyan else palette.textSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    CompactProSlider("Mask Exposure", radial.exposureDelta, -1f..1f, formatSigned(radial.exposureDelta)) { v ->
                        onUpdateSettings { it.copy(radialMask = it.radialMask.copy(exposureDelta = v)) }
                    }
                    CompactProSlider("Mask Warmth", radial.warmthDelta, -1f..1f, formatSigned(radial.warmthDelta)) { v ->
                        onUpdateSettings { it.copy(radialMask = it.radialMask.copy(warmthDelta = v)) }
                    }
                    CompactProSlider("Mask Radius", radial.radius, 0.15f..0.75f, "${(radial.radius * 100).roundToInt()}%") { v ->
                        onUpdateSettings { it.copy(radialMask = it.radialMask.copy(radius = v)) }
                    }
                    CompactProSlider("Feather Softness", radial.feather, 0.05f..0.85f, "${(radial.feather * 100).roundToInt()}%") { v ->
                        onUpdateSettings { it.copy(radialMask = it.radialMask.copy(feather = v)) }
                    }
                }
            }
        }
    }
}

private fun formatSigned(value: Float): String {
    val pct = (value * 100f).roundToInt()
    return if (pct > 0) "+$pct%" else "$pct%"
}
