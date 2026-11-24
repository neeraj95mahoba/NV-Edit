package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.network.NetworkStatus
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioThemeMode
import com.example.ui.theme.WarmAmber

enum class HomeAiToolType(
    val title: String,
    val badge: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    TEXT_TO_VIDEO("Text to Video", "AI", NeonCyan, ElectricPurple),
    TEXT_TO_IMAGE("Text to Image", "PRO", ElectricPurple, MagentaSpark),
    AUTO_CAPTIONS("Auto Captions", "HI/EN", ProGold, WarmAmber),
    BG_REMOVER("BG Remover", "CHROMA", Color(0xFF10E896), NeonCyan),
    BEAT_SYNC("Beat Sync", "BPM", MagentaSpark, ElectricPurple),
    AI_ENHANCER("AI Enhancer", "4K LUT", Color(0xFF38BDF8), AcidLime)
}

/**
 * Tactile spring-scale press modifier with optional subtle haptic feedback.
 */
fun Modifier.springPressClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    hapticType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springPressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (hapticType != null) haptic.performHapticFeedback(hapticType)
                onClick()
            }
        )
}

/**
 * Sleek custom vector brand emblem for NV Edit (obsidian squircle with dual cyan-violet film cut blade).
 */
@Composable
fun NvEditBrandEmblem(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = w * 0.28f

        // Obsidian frosted squircle base
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1A1D2B), Color(0xFF0E1017)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle ambient cyan-violet corner aura
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(ElectricPurple.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(w * 0.25f, h * 0.25f),
                radius = w * 0.78f
            ),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.32f), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.82f),
                radius = w * 0.65f
            ),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Crisp 1.1dp glass border
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    ElectricPurple.copy(alpha = 0.45f),
                    NeonCyan.copy(alpha = 0.55f)
                )
            ),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = w * 0.04f)
        )

        // Stylized "NV" film-cut ribbon glyph
        val strokeW = w * 0.105f
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.72f)
            lineTo(w * 0.24f, h * 0.30f)
            lineTo(w * 0.52f, h * 0.70f)
            lineTo(w * 0.76f, h * 0.28f)
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color.White, NeonCyan, ElectricPurple),
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.82f, h * 0.8f)
            ),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Precision cyan playhead spark dot
        drawCircle(
            color = NeonCyan,
            radius = w * 0.068f,
            center = Offset(w * 0.76f, h * 0.70f)
        )
    }
}

/**
 * Custom minimal vector camera / film-cut studio icon for the Hero "+ New Project" action card.
 */
@Composable
fun HeroFilmCutIcon(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Frosted inner lens plate
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    ElectricPurple.copy(alpha = 0.30f),
                    NeonCyan.copy(alpha = 0.18f)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(w * 0.30f, w * 0.30f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.75f), ElectricPurple.copy(alpha = 0.65f))
            ),
            cornerRadius = CornerRadius(w * 0.30f, w * 0.30f),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // Film frame rectangle
        val frameL = w * 0.20f
        val frameT = h * 0.28f
        val frameW = w * 0.44f
        val frameH = h * 0.44f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(frameL, frameT),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
            style = Stroke(width = w * 0.06f)
        )

        // Cinema lens cone on the right
        val lensPath = Path().apply {
            moveTo(frameL + frameW + w * 0.04f, h * 0.43f)
            lineTo(w * 0.82f, h * 0.32f)
            lineTo(w * 0.82f, h * 0.68f)
            lineTo(frameL + frameW + w * 0.04f, h * 0.57f)
            close()
        }
        drawPath(
            path = lensPath,
            brush = Brush.verticalGradient(listOf(NeonCyan, ElectricPurple))
        )

        // Precision "+" cut cross inside frame
        val cx = frameL + frameW * 0.5f
        val cy = frameT + frameH * 0.5f
        val arm = w * 0.095f
        drawLine(
            color = NeonCyan,
            start = Offset(cx - arm, cy),
            end = Offset(cx + arm, cy),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = NeonCyan,
            start = Offset(cx, cy - arm),
            end = Offset(cx, cy + arm),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Custom minimal duo-tone vector icons for the 6 Home AI Studio Tools Strip tiles.
 */
@Composable
fun StudioToolGlyph(
    type: HomeAiToolType,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    val primary = type.primaryColor
    val secondary = type.secondaryColor

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        when (type) {
            HomeAiToolType.TEXT_TO_VIDEO -> {
                // Rounded film frame with play triangle + top-right AI spark
                drawRoundRect(
                    color = secondary.copy(alpha = 0.24f),
                    topLeft = Offset(w * 0.10f, h * 0.20f),
                    size = Size(w * 0.72f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.10f, h * 0.20f),
                    size = Size(w * 0.72f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = Stroke(width = stroke)
                )
                val play = Path().apply {
                    moveTo(w * 0.39f, h * 0.38f)
                    lineTo(w * 0.59f, h * 0.51f)
                    lineTo(w * 0.39f, h * 0.64f)
                    close()
                }
                drawPath(play, color = Color.White)
                // 4-point AI spark at top-right
                drawFourPointSpark(Offset(w * 0.84f, h * 0.18f), w * 0.14f, primary)
            }

            HomeAiToolType.TEXT_TO_IMAGE -> {
                // Gallery frame + mountain silhouette + glowing sun orb
                drawRoundRect(
                    color = primary.copy(alpha = 0.22f),
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = secondary,
                    radius = w * 0.09f,
                    center = Offset(w * 0.66f, h * 0.36f)
                )
                val hills = Path().apply {
                    moveTo(w * 0.20f, h * 0.74f)
                    lineTo(w * 0.42f, h * 0.48f)
                    lineTo(w * 0.58f, h * 0.64f)
                    lineTo(w * 0.69f, h * 0.54f)
                    lineTo(w * 0.80f, h * 0.74f)
                    close()
                }
                drawPath(hills, color = Color.White.copy(alpha = 0.92f))
            }

            HomeAiToolType.AUTO_CAPTIONS -> {
                // Rounded subtitle card with duo-tone text bars
                drawRoundRect(
                    color = primary.copy(alpha = 0.20f),
                    topLeft = Offset(w * 0.10f, h * 0.20f),
                    size = Size(w * 0.80f, h * 0.60f),
                    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.10f, h * 0.20f),
                    size = Size(w * 0.80f, h * 0.60f),
                    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
                    style = Stroke(width = stroke)
                )
                drawLine(Color.White, Offset(w * 0.24f, h * 0.42f), Offset(w * 0.54f, h * 0.42f), stroke, StrokeCap.Round)
                drawLine(primary, Offset(w * 0.64f, h * 0.42f), Offset(w * 0.76f, h * 0.42f), stroke, StrokeCap.Round)
                drawLine(primary, Offset(w * 0.24f, h * 0.60f), Offset(w * 0.44f, h * 0.60f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.54f, h * 0.60f), Offset(w * 0.76f, h * 0.60f), stroke, StrokeCap.Round)
            }

            HomeAiToolType.BG_REMOVER -> {
                // Checkerboard cutout backdrop + crisp foreground person silhouette
                val cell = w * 0.16f
                for (r in 0..2) {
                    for (c in 0..2) {
                        if ((r + c) % 2 == 0) {
                            drawRect(
                                color = primary.copy(alpha = 0.28f),
                                topLeft = Offset(w * 0.14f + c * cell, h * 0.18f + r * cell),
                                size = Size(cell, cell)
                            )
                        }
                    }
                }
                drawRoundRect(
                    color = secondary,
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(cell * 3f, cell * 3f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                    style = Stroke(width = stroke * 0.85f)
                )
                // Foreground subject portrait badge
                drawCircle(color = Color.White, radius = w * 0.13f, center = Offset(w * 0.64f, h * 0.44f))
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.44f, h * 0.61f),
                    size = Size(w * 0.40f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
                )
            }

            HomeAiToolType.BEAT_SYNC -> {
                // Symmetrical equalizer waveform bars + cyan beat pulse dot
                val heights = listOf(0.28f, 0.54f, 0.78f, 0.46f, 0.66f, 0.34f)
                val barW = w * 0.085f
                val gap = (w * 0.76f) / heights.size
                heights.forEachIndexed { i, frac ->
                    val cx = w * 0.17f + i * gap
                    val bh = h * frac
                    drawLine(
                        color = if (i % 2 == 0) primary else Color.White,
                        start = Offset(cx, (h - bh) / 2f),
                        end = Offset(cx, (h + bh) / 2f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(color = NeonCyan, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.12f))
            }

            HomeAiToolType.AI_ENHANCER -> {
                // Diagonal magic grading wand + dual sparkles
                drawLine(
                    brush = Brush.linearGradient(listOf(primary, secondary)),
                    start = Offset(w * 0.20f, h * 0.80f),
                    end = Offset(w * 0.68f, h * 0.32f),
                    strokeWidth = stroke * 1.25f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.55f, h * 0.45f),
                    end = Offset(w * 0.68f, h * 0.32f),
                    strokeWidth = stroke * 1.3f,
                    cap = StrokeCap.Round
                )
                drawFourPointSpark(Offset(w * 0.74f, h * 0.24f), w * 0.16f, secondary)
                drawFourPointSpark(Offset(w * 0.30f, h * 0.30f), w * 0.11f, primary)
                drawFourPointSpark(Offset(w * 0.78f, h * 0.66f), w * 0.10f, Color.White)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFourPointSpark(
    center: Offset,
    radius: Float,
    color: Color
) {
    val inner = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path, color = color)
}

/**
 * Sleek Frosted-Glass Settings & Profile Sheet Modal opened from the clean Home top header.
 * Keeps theme switching, low-network Lite Mode, Storyboards, and Privacy/Store Info 1 tap away
 * without cluttering the flagship home header.
 */
@Composable
fun StudioSettingsSheetDialog(
    themeMode: StudioThemeMode,
    networkStatus: NetworkStatus,
    onSelectTheme: (StudioThemeMode) -> Unit,
    onToggleLowDataMode: () -> Unit,
    onOpenPrivacyInfo: () -> Unit,
    onOpenAiLab: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.glassSurface,
            border = BorderStroke(1.dp, palette.glassBorderBrush),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("studio_settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NvEditBrandEmblem(size = 34.dp)
                        Column {
                            Text(
                                text = "NV Edit Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PRO 4K • 60fps • Watermark-Free",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close studio settings",
                            tint = palette.textSecondary
                        )
                    }
                }

                // Studio Theme Switcher (OLED Obsidian / Studio Light / Midnight Glass)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Studio Appearance",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioThemeMode.entries.forEach { mode ->
                                val active = mode == themeMode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) palette.accentPrimary.copy(alpha = 0.24f) else palette.surface,
                                    border = BorderStroke(
                                        width = if (active) 1.2.dp else 1.dp,
                                        color = if (active) palette.accentSecondary else palette.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectTheme(mode) }
                                        .testTag("settings_theme_${mode.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.shortLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) palette.accentSecondary else palette.textSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Low-Network Lite Mode Toggle
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentSecondary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DataSaverOn,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Low-Network Lite Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${networkStatus.quality.label} • Fast Gemini Lite + Auto-Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = networkStatus.manualLowDataMode,
                            onCheckedChange = { onToggleLowDataMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = palette.accentSecondary
                            ),
                            modifier = Modifier.testTag("settings_low_data_switch")
                        )
                    }
                }

                // Quick Navigation Actions: AI Lab / Storyboards + Privacy & Store Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onDismiss()
                                onOpenAiLab()
                            }
                            .testTag("settings_open_ai_lab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewCarousel,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AI & Shots",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onDismiss()
                                onOpenPrivacyInfo()
                            }
                            .testTag("settings_open_privacy_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PrivacyTip,
                                contentDescription = null,
                                tint = ProGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Privacy & Info",
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
