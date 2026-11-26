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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MultilineChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.ColorScopeData
import com.example.model.AspectRatioPreset
import com.example.model.AudioStemMix
import com.example.model.ColorScopeMode
import com.example.model.MultiCamAngle
import com.example.model.RelightPreset
import com.example.model.RelightSettings
import com.example.model.TranscriptWord
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TrackAudioAccent
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// 1. AI TEXT-BASED VIDEO EDITING DRAWER (Descript / Premiere Pro Style)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextBasedVideoEditDrawer(
    transcriptWords: List<TranscriptWord>,
    playheadMs: Long,
    onTranscribeTimeline: (String?) -> Unit,
    onLaunchDictation: () -> Unit,
    onToggleWordDeleted: (String) -> Unit,
    onMarkFillerWords: () -> Unit,
    onApplyTranscriptCuts: () -> Unit,
    onApplyEditedParagraph: (String) -> Unit,
    onSeekToWord: (Long) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var isParagraphMode by remember { mutableStateOf(false) }
    var editableParagraph by remember(transcriptWords) {
        mutableStateOf(transcriptWords.filterNot { it.isDeleted }.joinToString(" ") { it.text })
    }

    val deletedCount = remember(transcriptWords) { transcriptWords.count { it.isDeleted } }
    val deletedMs = remember(transcriptWords) {
        transcriptWords.filter { it.isDeleted }.sumOf { (it.endMs - it.startMs).coerceAtLeast(0L) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ContentCut, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI Text-Based Video Editor", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricPurple.copy(alpha = 0.24f),
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTranscribeTimeline(null)
                    }
                    .testTag("text_edit_transcribe_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Transcribe audio", tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Transcribe", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLaunchDictation()
                    }
                    .testTag("text_edit_mic_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Dictate transcript", tint = ProGold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Mic", style = MaterialTheme.typography.labelSmall, color = ProGold)
                }
            }
        }
    }

    if (transcriptWords.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Edit Video Like a Document (Descript / Premiere Pro Style)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ProGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap Transcribe or Mic to turn spoken audio into word-synced text. Deleting any word automatically trims the matching video & audio segment on your timeline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    } else {
        // Action Bar: Cut Fillers / Toggle Paragraph Mode / Apply Cuts to Timeline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ProGold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, ProGold.copy(alpha = 0.7f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMarkFillerWords()
                    }
                    .testTag("text_edit_cut_fillers_button")
            ) {
                Text(
                    text = "Cut Fillers (um/uh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isParagraphMode) NeonCyan.copy(alpha = 0.2f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (isParagraphMode) NeonCyan else palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isParagraphMode = !isParagraphMode
                        if (isParagraphMode) {
                            editableParagraph = transcriptWords.filterNot { it.isDeleted }.joinToString(" ") { it.text }
                        }
                    }
                    .testTag("text_edit_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Toggle text edit mode", tint = if (isParagraphMode) NeonCyan else palette.textSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (isParagraphMode) "Word Chips" else "Edit Text",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isParagraphMode) NeonCyan else palette.textSecondary
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (deletedCount > 0 || isParagraphMode) ProGoldGradient else Brush.horizontalGradient(listOf(palette.surfaceElevated, palette.surfaceElevated)))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isParagraphMode) {
                            onApplyEditedParagraph(editableParagraph)
                            isParagraphMode = false
                        } else {
                            onApplyTranscriptCuts()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("text_edit_apply_cuts_button")
            ) {
                val secLabel = String.format(Locale.US, "%.1fs", deletedMs / 1000f)
                Text(
                    text = if (isParagraphMode) "Sync Cuts" else if (deletedCount > 0) "Apply Cuts (-$secLabel)" else "Apply Cuts",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (deletedCount > 0 || isParagraphMode) OledBlack else palette.textSecondary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (isParagraphMode) {
            OutlinedTextField(
                value = editableParagraph,
                onValueChange = { editableParagraph = it },
                label = { Text("Delete words here, then tap Sync Cuts") },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_edit_paragraph_field")
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OledBlack.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transcriptWords.forEach { word ->
                        val isAtPlayhead = playheadMs in word.startMs..word.endMs
                        val chipBg = when {
                            word.isDeleted -> CoralRed.copy(alpha = 0.22f)
                            isAtPlayhead -> NeonCyan.copy(alpha = 0.28f)
                            word.isFiller -> ProGold.copy(alpha = 0.18f)
                            else -> palette.surfaceElevated
                        }
                        val chipBorder = when {
                            word.isDeleted -> CoralRed.copy(alpha = 0.75f)
                            isAtPlayhead -> NeonCyan
                            word.isFiller -> ProGold.copy(alpha = 0.65f)
                            else -> palette.border
                        }
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekToWord(word.startMs)
                                    onToggleWordDeleted(word.id)
                                }
                                .testTag("transcript_word_${word.id}")
                        ) {
                            Text(
                                text = word.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    word.isDeleted -> CoralRed
                                    isAtPlayhead -> NeonCyan
                                    word.isFiller -> ProGold
                                    else -> palette.textPrimary
                                },
                                textDecoration = if (word.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                                fontWeight = if (isAtPlayhead || word.isFiller) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 2. MULTI-CAM 4-ANGLE SYNC & AI AUTO-REFRAME DRAWER
// ---------------------------------------------------------------------------------------------

@Composable
fun MultiCamAndReframeDrawer(
    angles: List<MultiCamAngle>,
    activeAngleIndex: Int,
    currentAspectRatio: AspectRatioPreset,
    isRunningAutoReframe: Boolean,
    onSyncAngles: () -> Unit,
    onSwitchAngle: (Int) -> Unit,
    onPickAngleMedia: (Int) -> Unit,
    onRunAutoReframe: (AspectRatioPreset) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current

    // Section A: Multi-Cam 4-Angle Audio-Waveform Sync Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Multi-Cam 4-Angle Sync", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeonCyan.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, NeonCyan),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSyncAngles()
                }
                .testTag("multicam_auto_sync_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Sync, contentDescription = "Sync camera angles via audio waveform", tint = NeonCyan, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Waveform Sync", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 2x2 Live Camera Angle Switcher Matrix
    val displayAngles = if (angles.size == 4) angles else listOf(
        MultiCamAngle(0, "Cam A · Wide", 0xFF00F0FFL, syncOffsetMs = 0L, confidencePct = 99),
        MultiCamAngle(1, "Cam B · Close", 0xFFFFD84DL, syncOffsetMs = 32L, confidencePct = 97),
        MultiCamAngle(2, "Cam C · Side", 0xFF9B5CFFL, syncOffsetMs = -48L, confidencePct = 96),
        MultiCamAngle(3, "Cam D · Overhead", 0xFF10E896L, syncOffsetMs = 64L, confidencePct = 98)
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(displayAngles.subList(0, 2), displayAngles.subList(2, 4)).forEach { rowAngles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                rowAngles.forEach { angle ->
                    val isLive = activeAngleIndex == angle.angleIndex
                    val accent = Color(angle.accentHex)
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isLive) accent.copy(alpha = 0.22f) else palette.surfaceElevated,
                        border = BorderStroke(if (isLive) 1.5.dp else 1.dp, if (isLive) accent else palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwitchAngle(angle.angleIndex)
                            }
                            .testTag("multicam_angle_${angle.angleIndex}")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = angle.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLive) accent else palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = palette.background,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .clickable { onPickAngleMedia(angle.angleIndex) }
                                        .testTag("multicam_pick_${angle.angleIndex}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Assign media to ${angle.label}",
                                        tint = accent,
                                        modifier = Modifier.padding(3.dp).size(12.dp)
                                    )
                                }
                            }
                            // Mini Audio Waveform Sync Preview
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bars = angle.waveformPeaks.ifEmpty {
                                    List(16) { i -> 0.25f + 0.65f * kotlin.math.abs(sin((i + angle.angleIndex) * 0.7f)) }
                                }.take(16)
                                bars.forEach { pk ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((pk.coerceIn(0.15f, 1f) * 14f).dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(accent.copy(alpha = if (isLive) 0.95f else 0.50f))
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isLive) "LIVE CUT ●" else "Tap to Cut",
                                    fontSize = 9.sp,
                                    color = if (isLive) AcidLime else palette.textSecondary,
                                    fontWeight = if (isLive) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                Text(
                                    text = "${if (angle.syncOffsetMs >= 0) "+" else ""}${angle.syncOffsetMs}ms · ${angle.confidencePct}%",
                                    fontSize = 9.sp,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Section B: AI Auto-Reframe (16:9 -> 9:16 Reels / Shorts with Subject & Face Tracking)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ProGold.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, ProGold.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Auto-Reframe (Subject Centering)", style = MaterialTheme.typography.labelMedium, color = ProGold, fontWeight = FontWeight.Bold)
                }
                if (isRunningAutoReframe) {
                    CircularProgressIndicator(color = ProGold, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        text = "Now: ${currentAspectRatio.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val targets = listOf(
                    AspectRatioPreset.PORTRAIT_9_16 to "16:9 → 9:16 Reels",
                    AspectRatioPreset.SOCIAL_4_5 to "4:5 IG",
                    AspectRatioPreset.SQUARE_1_1 to "1:1 Square"
                )
                targets.forEach { (preset, label) ->
                    val isCurrent = currentAspectRatio == preset
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (preset == AspectRatioPreset.PORTRAIT_9_16) ProGold else palette.surfaceElevated,
                        border = BorderStroke(1.dp, if (isCurrent) NeonCyan else ProGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(if (preset == AspectRatioPreset.PORTRAIT_9_16) 1.55f else 1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isRunningAutoReframe) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRunAutoReframe(preset)
                            }
                            .testTag("ai_auto_reframe_${preset.name}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (preset == AspectRatioPreset.PORTRAIT_9_16) OledBlack else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 3. AI 3D RELIGHT STUDIO & DSLR DEPTH-OF-FIELD BOKEH PANEL
// ---------------------------------------------------------------------------------------------

@Composable
fun AiRelightStudioPanel(
    relight: RelightSettings,
    onUpdateRelight: ((RelightSettings) -> RelightSettings) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LightMode, contentDescription = null, tint = ProGold, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI 3D Relight & DSLR Bokeh", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Text(
            text = "${relight.azimuthDeg.roundToInt()}° · Tilt ${relight.elevationDeg.roundToInt()}°",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan
        )
    }

    // 7 Studio & Neon Relight Presets
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(RelightPreset.entries) { preset ->
            val active = relight.preset == preset
            val keyColor = Color(preset.keyColorHex)
            val rimColor = Color(preset.rimColorHex)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) keyColor.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) keyColor else palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateRelight { it.copy(preset = preset) }
                    }
                    .testTag("relight_preset_${preset.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(keyColor, rimColor)))
                    )
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // Interactive 3D Virtual Light Dome Drag Pad + Quick Light Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeKeyColor = if (relight.preset == RelightPreset.OFF) NeonCyan else Color(relight.preset.keyColorHex)
        val activeRimColor = if (relight.preset == RelightPreset.OFF) MagentaSpark else Color(relight.preset.rimColorHex)

        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(OledBlack)
                .border(1.2.dp, activeKeyColor.copy(alpha = 0.65f), CircleShape)
                .pointerInput(relight.preset) {
                    fun updateLightFromPos(pos: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val rMax = (size.width / 2f).coerceAtLeast(1f)
                        val az = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                        val distNorm = (hypot(dx, dy) / rMax).coerceIn(0f, 1f)
                        val elev = (85f - distNorm * 115f).coerceIn(-45f, 85f)
                        onUpdateRelight { curr ->
                            curr.copy(
                                preset = if (curr.preset == RelightPreset.OFF) RelightPreset.NEON_CYBER else curr.preset,
                                azimuthDeg = az,
                                elevationDeg = elev
                            )
                        }
                    }
                    detectTapGestures { pos -> updateLightFromPos(pos) }
                }
                .pointerInput(relight.preset) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        val rMax = (size.width / 2f).coerceAtLeast(1f)
                        val az = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                        val distNorm = (hypot(dx, dy) / rMax).coerceIn(0f, 1f)
                        val elev = (85f - distNorm * 115f).coerceIn(-45f, 85f)
                        onUpdateRelight { curr ->
                            curr.copy(
                                preset = if (curr.preset == RelightPreset.OFF) RelightPreset.NEON_CYBER else curr.preset,
                                azimuthDeg = az,
                                elevationDeg = elev
                            )
                        }
                    }
                }
                .testTag("relight_3d_dome_pad")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension * 0.44f
                // 3D hemisphere rings
                drawCircle(color = Color.White.copy(alpha = 0.14f), radius = radius, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
                drawCircle(color = Color.White.copy(alpha = 0.10f), radius = radius * 0.55f, center = Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
                // Subject dot in center
                drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = Offset(cx, cy))

                // Virtual 3D Light position
                val distNorm = ((85f - relight.elevationDeg) / 115f).coerceIn(0.1f, 0.96f)
                val rad = Math.toRadians(relight.azimuthDeg.toDouble())
                val lx = cx + cos(rad).toFloat() * radius * distNorm
                val ly = cy + sin(rad).toFloat() * radius * distNorm
                val rx = cx - cos(rad).toFloat() * radius * 0.85f
                val ry = cy - sin(rad).toFloat() * radius * 0.85f

                // Beam line from key light to subject
                drawLine(color = activeKeyColor.copy(alpha = 0.55f), start = Offset(lx, ly), end = Offset(cx, cy), strokeWidth = 1.6.dp.toPx())
                // Opposing rim light indicator
                drawCircle(color = activeRimColor.copy(alpha = 0.7f), radius = 4.5.dp.toPx(), center = Offset(rx, ry))
                // Glowing 3D key light orb
                drawCircle(color = activeKeyColor.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = Offset(lx, ly))
                drawCircle(color = activeKeyColor, radius = 6.dp.toPx(), center = Offset(lx, ly))
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CompactProSlider("Light Intensity", relight.intensity, 0f..1f, "${(relight.intensity * 100).roundToInt()}%") { v ->
                onUpdateRelight { it.copy(intensity = v) }
            }
            CompactProSlider("3D Rim Backlight", relight.rimBoost, 0f..1f, "${(relight.rimBoost * 100).roundToInt()}%") { v ->
                onUpdateRelight { it.copy(rimBoost = v) }
            }
            CompactProSlider("Beam Spread", relight.spread, 0.2f..1f, "${(relight.spread * 100).roundToInt()}%") { v ->
                onUpdateRelight { it.copy(spread = v) }
            }
        }
    }

    CompactProSlider("DSLR Depth-of-Field Bokeh", relight.dslrBokeh, 0f..1f, "${(relight.dslrBokeh * 100).roundToInt()}%") { v ->
        onUpdateRelight { it.copy(dslrBokeh = v) }
    }
    if (relight.dslrBokeh > 0.02f) {
        CompactProSlider("Focus Plane Radius", relight.focusRadius, 0.18f..0.75f, "${(relight.focusRadius * 100).roundToInt()}%") { v ->
            onUpdateRelight { it.copy(focusRadius = v) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 4. LIVE CINEMA COLOR SCOPES HUD (RGB Parade / Luma Waveform / Histogram)
// ---------------------------------------------------------------------------------------------

@Composable
fun LiveColorScopesHudOverlay(
    mode: ColorScopeMode,
    scopeData: ColorScopeData,
    onCycleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mode == ColorScopeMode.OFF) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OledBlack.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
        modifier = modifier
            .width(176.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCycleMode)
            .testTag("live_color_scopes_hud")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.MultilineChart, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    Text(
                        text = mode.label,
                        fontSize = 10.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${scopeData.avgLumaIre}/${scopeData.peakLumaIre} IRE",
                    fontSize = 9.sp,
                    color = ProGold,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF06070B))
            ) {
                val w = size.width
                val h = size.height

                // IRE Reference Graticule (25, 50, 75 IRE)
                listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f),
                        start = Offset(0f, h * frac),
                        end = Offset(w, h * frac),
                        strokeWidth = 0.6.dp.toPx()
                    )
                }

                when (mode) {
                    ColorScopeMode.RGB_PARADE -> {
                        val thirdW = w / 3f
                        drawLine(Color.White.copy(alpha = 0.18f), Offset(thirdW, 0f), Offset(thirdW, h), 0.7.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.18f), Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, h), 0.7.dp.toPx())

                        val cols = scopeData.columns.coerceAtLeast(1)
                        val step = thirdW / cols
                        for (x in 0 until cols) {
                            // Red Parade
                            val rX = x * step + step * 0.5f
                            drawLine(
                                color = Color(0xFFFF3B5C).copy(alpha = 0.72f),
                                start = Offset(rX, h * (1f - scopeData.paradeRMax[x])),
                                end = Offset(rX, h * (1f - scopeData.paradeRMin[x])),
                                strokeWidth = step.coerceAtLeast(1f)
                            )
                            drawCircle(Color(0xFFFF7A90), radius = 1.1f, center = Offset(rX, h * (1f - scopeData.paradeRAvg[x])))

                            // Green Parade
                            val gX = thirdW + x * step + step * 0.5f
                            drawLine(
                                color = Color(0xFF22E58B).copy(alpha = 0.72f),
                                start = Offset(gX, h * (1f - scopeData.paradeGMax[x])),
                                end = Offset(gX, h * (1f - scopeData.paradeGMin[x])),
                                strokeWidth = step.coerceAtLeast(1f)
                            )
                            drawCircle(Color(0xFF86FFBE), radius = 1.1f, center = Offset(gX, h * (1f - scopeData.paradeGAvg[x])))

                            // Blue Parade
                            val bX = thirdW * 2f + x * step + step * 0.5f
                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.78f),
                                start = Offset(bX, h * (1f - scopeData.paradeBMax[x])),
                                end = Offset(bX, h * (1f - scopeData.paradeBMin[x])),
                                strokeWidth = step.coerceAtLeast(1f)
                            )
                            drawCircle(Color(0xFFA5F3FC), radius = 1.1f, center = Offset(bX, h * (1f - scopeData.paradeBAvg[x])))
                        }
                    }

                    ColorScopeMode.LUMA_WAVEFORM -> {
                        val cols = scopeData.columns.coerceAtLeast(1)
                        val step = w / cols
                        val avgPath = Path()
                        for (x in 0 until cols) {
                            val px = x * step + step * 0.5f
                            val yTop = h * (1f - scopeData.lumaMax[x])
                            val yBot = h * (1f - scopeData.lumaMin[x])
                            val yAvg = h * (1f - scopeData.lumaAvg[x])
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.35f),
                                start = Offset(px, yTop),
                                end = Offset(px, yBot),
                                strokeWidth = (step * 0.85f).coerceAtLeast(1.2f)
                            )
                            if (x == 0) avgPath.moveTo(px, yAvg) else avgPath.lineTo(px, yAvg)
                        }
                        drawPath(avgPath, color = Color(0xFFE0FFFF), style = Stroke(1.6.dp.toPx()))
                    }

                    ColorScopeMode.HISTOGRAM -> {
                        val bins = scopeData.bins.coerceAtLeast(1)
                        val barW = w / bins
                        fun drawChannelHist(arr: FloatArray, color: Color) {
                            val path = Path().apply { moveTo(0f, h) }
                            for (i in 0 until bins) {
                                val x = i * barW + barW * 0.5f
                                val y = h * (1f - arr[i])
                                path.lineTo(x, y)
                            }
                            path.lineTo(w, h)
                            path.close()
                            drawPath(path, color = color.copy(alpha = 0.34f))
                            drawPath(path, color = color.copy(alpha = 0.90f), style = Stroke(1.1.dp.toPx()))
                        }
                        drawChannelHist(scopeData.histR, Color(0xFFFF3B5C))
                        drawChannelHist(scopeData.histG, Color(0xFF22E58B))
                        drawChannelHist(scopeData.histB, Color(0xFF38BDF8))
                        drawChannelHist(scopeData.histLuma, Color.White.copy(alpha = 0.7f))
                    }

                    ColorScopeMode.OFF -> Unit
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 5. 4-STEM AI AUDIO SPLITTER & AUTO-DUCKING DEPTH RACK
// ---------------------------------------------------------------------------------------------

@Composable
fun AudioStemSplitterRack(
    stemMix: AudioStemMix,
    autoDucking: Boolean,
    duckingDepthDb: Float,
    onSplitFourStems: () -> Unit,
    onUpdateStemMix: ((AudioStemMix) -> AudioStemMix) -> Unit,
    onUpdateDuckingDepth: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (stemMix.isSplitActive) NeonCyan.copy(alpha = 0.65f) else palette.border),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("4-Stem AI Audio Splitter", style = MaterialTheme.typography.labelMedium, color = ProGold, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (stemMix.isSplitActive) AcidLime.copy(alpha = 0.22f) else NeonCyan.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, if (stemMix.isSplitActive) AcidLime else NeonCyan),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSplitFourStems()
                        }
                        .testTag("audio_split_4_stems_button")
                ) {
                    Text(
                        text = if (stemMix.isSplitActive) "4 Stems Active ✓" else "Split Song (4 Stems)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stemMix.isSplitActive) AcidLime else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 4 Independent Stem Channels: Vocals, Drums/Beats, Bass, Melody
            val channels = listOf(
                Triple("VOCALS", "Vocals", NeonCyan),
                Triple("DRUMS", "Drums", ProGold),
                Triple("BASS", "Bass", ElectricPurple),
                Triple("MELODY", "Melody", TrackAudioAccent)
            )
            channels.forEach { (key, title, accent) ->
                val level = when (key) {
                    "VOCALS" -> stemMix.vocalsLevel
                    "DRUMS" -> stemMix.drumsLevel
                    "BASS" -> stemMix.bassLevel
                    else -> stemMix.melodyLevel
                }
                val isMuted = when (key) {
                    "VOCALS" -> stemMix.vocalsMuted
                    "DRUMS" -> stemMix.drumsMuted
                    "BASS" -> stemMix.bassMuted
                    else -> stemMix.melodyMuted
                }
                val isSolo = stemMix.soloStem == key

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMuted) palette.textSecondary else palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(46.dp)
                    )
                    // Solo button
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isSolo) ProGold else palette.background,
                        border = BorderStroke(0.8.dp, if (isSolo) ProGold else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .clickable {
                                onUpdateStemMix { curr ->
                                    curr.copy(isSplitActive = true, soloStem = if (curr.soloStem == key) null else key)
                                }
                            }
                            .testTag("stem_solo_$key")
                    ) {
                        Text(
                            text = "S",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSolo) OledBlack else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    // Mute button
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isMuted) CoralRed else palette.background,
                        border = BorderStroke(0.8.dp, if (isMuted) CoralRed else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .clickable {
                                onUpdateStemMix { curr ->
                                    when (key) {
                                        "VOCALS" -> curr.copy(isSplitActive = true, vocalsMuted = !curr.vocalsMuted)
                                        "DRUMS" -> curr.copy(isSplitActive = true, drumsMuted = !curr.drumsMuted)
                                        "BASS" -> curr.copy(isSplitActive = true, bassMuted = !curr.bassMuted)
                                        else -> curr.copy(isSplitActive = true, melodyMuted = !curr.melodyMuted)
                                    }
                                }
                            }
                            .testTag("stem_mute_$key")
                    ) {
                        Text(
                            text = "M",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isMuted) Color.White else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CompactProSlider(
                            label = "",
                            value = level,
                            range = 0f..1.5f,
                            valueLabel = "${(level * 100).roundToInt()}%"
                        ) { v ->
                            onUpdateStemMix { curr ->
                                when (key) {
                                    "VOCALS" -> curr.copy(isSplitActive = true, vocalsLevel = v)
                                    "DRUMS" -> curr.copy(isSplitActive = true, drumsLevel = v)
                                    "BASS" -> curr.copy(isSplitActive = true, bassLevel = v)
                                    else -> curr.copy(isSplitActive = true, melodyLevel = v)
                                }
                            }
                        }
                    }
                }
            }

            if (autoDucking) {
                CompactProSlider(
                    label = "Auto-Ducking Music Drop",
                    value = duckingDepthDb,
                    range = -24f..-3f,
                    valueLabel = "${duckingDepthDb.roundToInt()} dB",
                    onChange = onUpdateDuckingDepth
                )
            }
        }
    }
}
