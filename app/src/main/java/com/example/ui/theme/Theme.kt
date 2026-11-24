package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StudioThemeMode(val label: String, val shortLabel: String) {
    OLED_DARK("OLED Obsidian", "OLED"),
    STUDIO_LIGHT("Studio Light", "Light"),
    MIDNIGHT_PURPLE("Midnight Glass", "Midnight");

    fun next(): StudioThemeMode = entries[(ordinal + 1) % entries.size]
}

@Immutable
data class StudioPalette(
    val mode: StudioThemeMode,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHigh: Color,
    val glassSurface: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val isLight: Boolean
) {
    val glassBorderBrush: Brush
        get() = if (isLight) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A).copy(alpha = 0.10f),
                    Color(0xFF0F172A).copy(alpha = 0.04f),
                    accentPrimary.copy(alpha = 0.16f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f),
                    accentSecondary.copy(alpha = 0.14f)
                )
            )
        }
}

val OledStudioPalette = StudioPalette(
    mode = StudioThemeMode.OLED_DARK,
    background = OledBlack,
    surface = StudioSurface,
    surfaceElevated = StudioSurfaceElevated,
    surfaceHigh = StudioSurfaceHigh,
    glassSurface = GlassSurface,
    border = StudioBorder,
    borderSubtle = StudioBorderSubtle,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    accentPrimary = ElectricPurple,
    accentSecondary = NeonCyan,
    isLight = false
)

val LightStudioPalette = StudioPalette(
    mode = StudioThemeMode.STUDIO_LIGHT,
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEEF1F8),
    surfaceHigh = Color(0xFFE2E6F2),
    glassSurface = Color(0xF5FFFFFF),
    border = Color(0x1F0F172A),
    borderSubtle = Color(0x120F172A),
    textPrimary = Color(0xFF0C0E16),
    textSecondary = Color(0xFF4B5168),
    textMuted = Color(0xFF7B8199),
    accentPrimary = Color(0xFF6D4AFF),
    accentSecondary = Color(0xFF0891B2),
    isLight = true
)

val MidnightPurplePalette = StudioPalette(
    mode = StudioThemeMode.MIDNIGHT_PURPLE,
    background = Color(0xFF0A0614),
    surface = Color(0xFF140D24),
    surfaceElevated = Color(0xFF1E1436),
    surfaceHigh = Color(0xFF2A1C4A),
    glassSurface = Color(0xEB150E26),
    border = Color(0x22C4B5FD),
    borderSubtle = Color(0x14C4B5FD),
    textPrimary = Color(0xFFF8F5FF),
    textSecondary = Color(0xFFBAAED9),
    textMuted = Color(0xFF7F70A6),
    accentPrimary = Color(0xFFA78BFA),
    accentSecondary = Color(0xFF22D3EE),
    isLight = false
)

val LocalStudioPalette = staticCompositionLocalOf { OledStudioPalette }

private fun paletteFor(mode: StudioThemeMode): StudioPalette = when (mode) {
    StudioThemeMode.OLED_DARK -> OledStudioPalette
    StudioThemeMode.STUDIO_LIGHT -> LightStudioPalette
    StudioThemeMode.MIDNIGHT_PURPLE -> MidnightPurplePalette
}

private fun colorSchemeFor(p: StudioPalette) = if (p.isLight) {
    lightColorScheme(
        primary = p.accentPrimary,
        onPrimary = Color.White,
        primaryContainer = p.accentPrimary.copy(alpha = 0.14f),
        onPrimaryContainer = p.textPrimary,
        secondary = p.accentSecondary,
        onSecondary = Color.White,
        secondaryContainer = p.accentSecondary.copy(alpha = 0.14f),
        onSecondaryContainer = p.accentSecondary,
        tertiary = MagentaSpark,
        onTertiary = Color.White,
        background = p.background,
        onBackground = p.textPrimary,
        surface = p.surface,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceElevated,
        onSurfaceVariant = p.textSecondary,
        surfaceContainerLowest = p.background,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surfaceElevated,
        surfaceContainerHigh = p.surfaceHigh,
        outline = p.border,
        outlineVariant = p.borderSubtle,
        error = CoralRed,
        onError = Color.White
    )
} else {
    darkColorScheme(
        primary = p.accentPrimary,
        onPrimary = OledBlack,
        primaryContainer = DeepViolet.copy(alpha = 0.28f),
        onPrimaryContainer = p.textPrimary,
        secondary = p.accentSecondary,
        onSecondary = OledBlack,
        secondaryContainer = p.accentSecondary.copy(alpha = 0.16f),
        onSecondaryContainer = p.accentSecondary,
        tertiary = MagentaSpark,
        onTertiary = p.textPrimary,
        background = p.background,
        onBackground = p.textPrimary,
        surface = p.surface,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceElevated,
        onSurfaceVariant = p.textSecondary,
        surfaceContainerLowest = p.background,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surfaceElevated,
        surfaceContainerHigh = p.surfaceHigh,
        outline = p.border,
        outlineVariant = p.borderSubtle,
        error = CoralRed,
        onError = p.textPrimary
    )
}

private val CutPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CutPulseTheme(
    themeMode: StudioThemeMode = StudioThemeMode.OLED_DARK,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeMode)
    CompositionLocalProvider(LocalStudioPalette provides palette) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette),
            typography = StudioTypography,
            shapes = CutPulseShapes,
            content = content
        )
    }
}
