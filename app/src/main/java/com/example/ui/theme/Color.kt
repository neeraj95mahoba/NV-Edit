package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// True Pitch-Black OLED Studio & Frosted-Glass Obsidian Surfaces
val OledBlack = Color(0xFF08080A)
val StudioSurface = Color(0xFF13141A)
val StudioSurfaceElevated = Color(0xFF1A1C25)
val StudioSurfaceHigh = Color(0xFF232634)
val GlassSurface = Color(0xEB13141A)
val FloatingNavGlass = Color(0xF0111218)
val StudioBorder = Color(0x17FFFFFF) // Subtle ~9% white glass hairline
val StudioBorderSubtle = Color(0x10FFFFFF) // Ultra-thin ~6% white hairline

// Refined Obsidian-Cyan-Violet Big-Tech Studio Accents
val ElectricPurple = Color(0xFF8B5CF6)
val DeepViolet = Color(0xFF5B3CE8)
val NeonCyan = Color(0xFF22D3EE)
val CyanSoft = Color(0xFF38BDF8)
val MagentaSpark = Color(0xFFF43F8C)
val AcidLime = Color(0xFF84F53A)
val WarmAmber = Color(0xFFF5B041)
val CoralRed = Color(0xFFFF4D6A)

// Refined Champagne-Gold Studio Badge Palette
val ProGold = Color(0xFFF6D365)
val ProGoldDeep = Color(0xFFE09F3E)
val BeatMarkerYellow = Color(0xFFFFE047)

// Crisp Typography Neutrals
val TextPrimary = Color(0xFFF7F8FC)
val TextSecondary = Color(0xFF9EA3B5)
val TextMuted = Color(0xFF676C80)

// Timeline Track Identity Colors
val TrackVideoAccent = Color(0xFF8B5CF6)
val TrackPipAccent = Color(0xFF22D3EE)
val TrackTextAccent = Color(0xFFF59E0B)
val TrackAudioAccent = Color(0xFF10E896)

// Signature Refined Studio Gradients
val NeonStudioGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF7C5CFF), Color(0xFF22D3EE))
)

val ObsidianGlassBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.14f),
        Color.White.copy(alpha = 0.04f),
        Color(0xFF22D3EE).copy(alpha = 0.14f)
    )
)

val ProGoldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFDE68A), ProGold, ProGoldDeep)
)

val NeonStudioVerticalGradient = Brush.verticalGradient(
    colors = listOf(ElectricPurple.copy(alpha = 0.22f), NeonCyan.copy(alpha = 0.08f), Color.Transparent)
)

val MagentaPurpleGradient = Brush.horizontalGradient(
    colors = listOf(MagentaSpark, ElectricPurple)
)
