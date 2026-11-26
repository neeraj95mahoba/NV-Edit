package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.MediaFrameEngine
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.TransitionType
import com.example.model.VideoProjectDocument
import com.example.ui.theme.BeatMarkerYellow
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.TrackAudioAccent
import com.example.ui.theme.TrackPipAccent
import com.example.ui.theme.TrackTextAccent
import com.example.ui.theme.TrackVideoAccent
import com.example.ui.theme.WarmAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TimelinePanel(
    document: VideoProjectDocument,
    frameEngine: MediaFrameEngine,
    selectedClipId: String?,
    playheadMs: Long,
    zoom: Float,
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onSplitClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onRippleDeleteClip: () -> Unit = {},
    onAddAdjustmentLayer: () -> Unit = {},
    onDetectBeats: () -> Unit,
    onClickTransitionNode: (clipId: String) -> Unit,
    onTrimClip: (clipId: String, deltaStartMs: Long, deltaDurationMs: Long) -> Unit,
    onMoveClip: (clipId: String, deltaMs: Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onImportVisual: () -> Unit,
    onImportAudio: () -> Unit,
    isMagneticSnapEnabled: Boolean = true,
    onToggleMagneticSnap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    val pxPerSecondBase = 68f
    val dpPerSecond = (pxPerSecondBase * zoom).dp
    val density = LocalDensity.current
    val pxPerMs = with(density) { dpPerSecond.toPx() / 1000f }
    val totalMs = (document.totalDurationMs + 3000L).coerceAtLeast(10_000L)
    val timelineWidthDp = ((totalMs / 1000f) * dpPerSecond.value + 72f).dp.coerceAtLeast(380.dp)
    val horizontalScroll = rememberScrollState()
    val hasSelection = selectedClipId != null

    val latestZoom by rememberUpdatedState(zoom)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)

    val mainVideoClips = document.clips
        .filter { it.trackType == TrackType.MAIN_VIDEO }
        .sortedBy { it.startMs }

    val adjustmentClips = document.clips
        .filter { it.trackType == TrackType.ADJUSTMENT_LAYER }
        .sortedBy { it.startMs }

    val audioClips = document.clips
        .filter { it.trackType == TrackType.AUDIO_MUSIC }
        .sortedBy { it.startMs }

    val mainEndMs = mainVideoClips.maxOfOrNull { it.endMs } ?: 0L
    val adjEndMs = adjustmentClips.maxOfOrNull { it.endMs } ?: 0L
    val audioEndMs = audioClips.maxOfOrNull { it.endMs } ?: 0L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        // Contextual Frosted-Glass Action Strip (Split, Dup, Del, Beats, Scale Zoom, Import)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceElevated)
                .border(0.5.dp, palette.border)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimelineActionChip(
                    icon = Icons.Filled.ContentCut,
                    label = "Split",
                    enabled = hasSelection,
                    accent = NeonCyan,
                    testTag = "timeline_split_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSplitClip()
                    }
                )
                TimelineActionChip(
                    icon = Icons.Filled.ContentCopy,
                    label = "Dup",
                    enabled = hasSelection,
                    accent = ElectricPurple,
                    testTag = "timeline_duplicate_button",
                    onClick = onDuplicateClip
                )
                TimelineActionChip(
                    icon = Icons.Filled.DeleteOutline,
                    label = "Del",
                    enabled = hasSelection,
                    accent = CoralRed,
                    testTag = "timeline_delete_button",
                    onClick = onDeleteClip
                )
                TimelineActionChip(
                    icon = Icons.Filled.DeleteSweep,
                    label = "Ripple",
                    enabled = hasSelection,
                    accent = WarmAmber,
                    testTag = "timeline_ripple_delete_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRippleDeleteClip()
                    }
                )
                TimelineActionChip(
                    icon = Icons.Filled.Bolt,
                    label = "Beats",
                    enabled = true,
                    accent = BeatMarkerYellow,
                    testTag = "timeline_beats_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDetectBeats()
                    }
                )
                TimelineActionChip(
                    icon = Icons.Filled.Link,
                    label = if (isMagneticSnapEnabled) "Snap" else "Free",
                    enabled = true,
                    accent = if (isMagneticSnapEnabled) ProGold else palette.textMuted,
                    testTag = "timeline_magnetic_snap_toggle",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleMagneticSnap()
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onImportVisual,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("timeline_add_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.cd_add_media),
                        tint = NeonCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onImportAudio,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("timeline_add_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = "Import audio track",
                        tint = TrackAudioAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = { onZoomChange(zoom - 0.35f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("timeline_zoom_out")
                ) {
                    Icon(
                        Icons.Filled.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onZoomChange(zoom + 0.35f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("timeline_zoom_in")
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Track Header Column + Pinch-to-Zoom Scrollable Precision Multi-Track Timeline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp)
                .background(palette.background)
        ) {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .background(palette.surface)
                    .padding(top = 24.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                TrackHeaderBadge("VID", TrackVideoAccent, Modifier.weight(1.12f))
                TrackHeaderBadge("ADJ", ProGold, Modifier.weight(0.72f))
                TrackHeaderBadge("PIP", TrackPipAccent, Modifier.weight(0.78f))
                TrackHeaderBadge("TXT", TrackTextAccent, Modifier.weight(0.78f))
                TrackHeaderBadge("AUD", TrackAudioAccent, Modifier.weight(0.92f))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Two-Finger Pinch-to-Zoom Track Scale right on the timeline canvas
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size >= 2) {
                                    val zoomDelta = event.calculateZoom()
                                    if (abs(zoomDelta - 1f) > 0.01f) {
                                        val nextZoom = (latestZoom * zoomDelta).coerceIn(0.45f, 3.2f)
                                        latestOnZoomChange(nextZoom)
                                        event.changes.forEach { ch ->
                                            if (ch.positionChanged()) ch.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .horizontalScroll(horizontalScroll)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                        .pointerInput(pxPerMs, totalMs) {
                            detectTapGestures { offset ->
                                val ms = (offset.x / pxPerMs).toLong().coerceIn(0L, document.totalDurationMs)
                                onSeek(ms)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Precision Time Ruler with Frame Sub-Ticks & Beat Markers
                        TimelineRuler(
                            totalMs = totalMs,
                            dpPerSecond = dpPerSecond,
                            pxPerMs = pxPerMs,
                            maxSeekMs = document.totalDurationMs,
                            beatMarkersMs = document.beatMarkersMs,
                            onSeek = onSeek,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )

                        // Main Video Filmstrip Track + Transition Nodes + Right-Edge "+ Add" CTA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f)
                        ) {
                            TrackLane(
                                clips = mainVideoClips,
                                frameEngine = frameEngine,
                                selectedClipId = selectedClipId,
                                pxPerMs = pxPerMs,
                                laneColor = TrackVideoAccent,
                                onSelectClip = onSelectClip,
                                onTrimClip = onTrimClip,
                                onMoveClip = onMoveClip,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Clickable Pro Transition Nodes between adjacent video clips
                            mainVideoClips.drop(1).forEach { clip ->
                                val nodeX = (clip.startMs * pxPerMs).roundToInt()
                                val hasActiveTrans = clip.transitionIn.type != TransitionType.NONE
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(nodeX - 11, 0) }
                                        .align(Alignment.CenterStart)
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (hasActiveTrans) ProGold else palette.surfaceElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (hasActiveTrans) Color.White else NeonCyan,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onClickTransitionNode(clip.id)
                                        }
                                        .testTag("transition_node_${clip.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = "Edit clip transition",
                                        tint = if (hasActiveTrans) OledBlack else NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Sleek "+ Add" Button Anchored at Right End of Main Video Track
                            val addBtnOffsetX = (mainEndMs * pxPerMs).roundToInt() + 10
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .offset { IntOffset(addBtnOffsetX, 0) }
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onImportVisual()
                                    }
                                    .testTag("timeline_right_add_clip_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = stringResource(R.string.cd_add_track_clip),
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Adjustment Layer Track (Color Grade / HDR / LUT Overlay Layer) + Quick "+ Adj" Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.72f)
                        ) {
                            TrackLane(
                                clips = adjustmentClips,
                                frameEngine = frameEngine,
                                selectedClipId = selectedClipId,
                                pxPerMs = pxPerMs,
                                laneColor = ProGold,
                                onSelectClip = onSelectClip,
                                onTrimClip = onTrimClip,
                                onMoveClip = onMoveClip,
                                modifier = Modifier.fillMaxSize()
                            )

                            val adjAddX = (adjEndMs * pxPerMs).roundToInt() + 10
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, ProGold.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .offset { IntOffset(adjAddX, 0) }
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable(onClick = onAddAdjustmentLayer)
                                    .testTag("timeline_right_add_adj_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoFixHigh,
                                        contentDescription = stringResource(R.string.cd_add_adjustment_layer),
                                        tint = ProGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "+ Adj",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ProGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // PIP / Overlay Track
                        TrackLane(
                            clips = document.clips.filter { it.trackType == TrackType.OVERLAY_PIP },
                            frameEngine = frameEngine,
                            selectedClipId = selectedClipId,
                            pxPerMs = pxPerMs,
                            laneColor = TrackPipAccent,
                            onSelectClip = onSelectClip,
                            onTrimClip = onTrimClip,
                            onMoveClip = onMoveClip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.78f)
                        )

                        // Text & Captions Track
                        TrackLane(
                            clips = document.clips.filter { it.trackType == TrackType.TEXT_STICKER },
                            frameEngine = frameEngine,
                            selectedClipId = selectedClipId,
                            pxPerMs = pxPerMs,
                            laneColor = TrackTextAccent,
                            onSelectClip = onSelectClip,
                            onTrimClip = onTrimClip,
                            onMoveClip = onMoveClip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.78f)
                        )

                        // Audio Waveform Track + Quick "+ Audio" Right-Edge Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.92f)
                        ) {
                            TrackLane(
                                clips = audioClips,
                                frameEngine = frameEngine,
                                selectedClipId = selectedClipId,
                                pxPerMs = pxPerMs,
                                laneColor = TrackAudioAccent,
                                volumeKeyframeTimes = document.audioEnhance.volumeKeyframes.map { it.timeMs },
                                beatMarkersMs = document.beatMarkersMs,
                                onSelectClip = onSelectClip,
                                onTrimClip = onTrimClip,
                                onMoveClip = onMoveClip,
                                modifier = Modifier.fillMaxSize()
                            )

                            val audioAddX = (audioEndMs * pxPerMs).roundToInt() + 10
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, TrackAudioAccent.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .offset { IntOffset(audioAddX, 0) }
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable(onClick = onImportAudio)
                                    .testTag("timeline_right_add_audio_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add audio clip",
                                        tint = TrackAudioAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Audio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                    }

                    // Crisp White Vertical Playhead Needle with Glowing Cyan-Rimmed Cap
                    val playheadX = (playheadMs * pxPerMs).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(playheadX - 10, 0) }
                            .width(20.dp)
                            .fillMaxHeight()
                            .pointerInput(pxPerMs, document.totalDurationMs) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = (dragAmount / pxPerMs).toLong()
                                    onSeek((playheadMs + deltaMs).coerceIn(0L, document.totalDurationMs))
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(width = 11.dp, height = 13.dp)
                                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp, topStart = 3.dp, topEnd = 3.dp))
                                .background(Color.White)
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp, topStart = 3.dp, topEnd = 3.dp))
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (enabled) accent.copy(alpha = 0.14f) else palette.surface,
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.50f) else palette.border),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accent else palette.textMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) palette.textPrimary else palette.textMuted,
                fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TrackHeaderBadge(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(0.8.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimelineRuler(
    totalMs: Long,
    dpPerSecond: Dp,
    pxPerMs: Float,
    maxSeekMs: Long,
    beatMarkersMs: List<Long>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val totalSeconds = (totalMs / 1000L).toInt() + 1
    Box(
        modifier = modifier
            .background(palette.surface)
            .pointerInput(pxPerMs, maxSeekMs) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val posMs = (change.position.x / pxPerMs).toLong().coerceIn(0L, maxSeekMs)
                    onSeek(posMs)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val secPx = dpPerSecond.toPx()
            val majorColor = palette.textMuted
            val minorColor = palette.border
            // Bottom hairline
            drawLine(
                color = minorColor,
                start = Offset(0f, size.height - 0.5f),
                end = Offset(size.width, size.height - 0.5f),
                strokeWidth = 1f
            )
            for (s in 0..totalSeconds) {
                val x = s * secPx
                // Major second tick
                drawLine(
                    color = majorColor,
                    start = Offset(x, size.height * 0.42f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.4f
                )
                // 4 sub-second frame markers (every 0.25s)
                for (sub in 1..3) {
                    val fx = x + secPx * (sub * 0.25f)
                    val topFrac = if (sub == 2) 0.62f else 0.78f
                    drawLine(
                        color = minorColor,
                        start = Offset(fx, size.height * topFrac),
                        end = Offset(fx, size.height),
                        strokeWidth = 1f
                    )
                }
            }
            // Yellow Beat-Sync Markers along the Ruler
            beatMarkersMs.forEach { beatMs ->
                val bx = beatMs * pxPerMs
                drawCircle(
                    color = BeatMarkerYellow,
                    radius = 3.dp.toPx(),
                    center = Offset(bx, size.height - 4.dp.toPx())
                )
            }
        }
        val stepSec = if (dpPerSecond.value < 55f) 2 else 1
        for (s in 0..totalSeconds step stepSec) {
            val mins = s / 60
            val remSec = s % 60
            Text(
                text = String.format(Locale.US, "%02d:%02d", mins, remSec),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier.offset(x = (s * dpPerSecond.value + 3f).dp, y = 2.dp)
            )
        }
    }
}

@Composable
private fun TrackLane(
    clips: List<TimelineClip>,
    frameEngine: MediaFrameEngine,
    selectedClipId: String?,
    pxPerMs: Float,
    laneColor: Color,
    volumeKeyframeTimes: List<Long> = emptyList(),
    beatMarkersMs: List<Long> = emptyList(),
    onSelectClip: (String?) -> Unit,
    onTrimClip: (clipId: String, deltaStartMs: Long, deltaDurationMs: Long) -> Unit,
    onMoveClip: (clipId: String, deltaMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val density = LocalDensity.current
    val laneBg = if (palette.isLight) palette.surfaceElevated.copy(alpha = 0.65f) else Color(0xFF0E0F14)

    Box(
        modifier = modifier
            .background(laneBg)
            .padding(vertical = 1.dp)
    ) {
        clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val leftPx = (clip.startMs * pxPerMs).roundToInt()
            val widthDp = with(density) { (clip.durationMs * pxPerMs).toDp().coerceAtLeast(38.dp) }
            val clipAccent = Color(clip.accentColorHex)

            val isVisualTrack = clip.trackType == TrackType.MAIN_VIDEO || clip.trackType == TrackType.OVERLAY_PIP
            val thumbBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = clip.mediaUri,
                key2 = clip.drawableKey
            ) {
                value = if (isVisualTrack) {
                    frameEngine.loadThumbnailBitmap(
                        uriString = clip.mediaUri,
                        drawableKey = clip.drawableKey,
                        maxDim = 180
                    )
                } else null
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx, 0) }
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(clipAccent.copy(alpha = if (isSelected) 0.34f else 0.20f))
                    .border(
                        width = if (isSelected) 1.6.dp else 1.dp,
                        color = if (isSelected) Color.White else clipAccent.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .pointerInput(clip.id, pxPerMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount / pxPerMs).toLong()
                            if (deltaMs != 0L) onMoveClip(clip.id, deltaMs)
                        }
                    }
                    .testTag("timeline_clip_${clip.id}")
            ) {
                // Visual Video Filmstrip Repeating Frames inside Video/PIP Timeline Clips
                val bmp = thumbBitmap
                if (bmp != null) {
                    val frameTileWidth = 44.dp
                    val tileCount = ceil(widthDp.value / frameTileWidth.value).toInt().coerceIn(1, 24)
                    Row(modifier = Modifier.fillMaxSize()) {
                        repeat(tileCount) { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.72f)
                                )
                                if (idx > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(0.7.dp)
                                            .fillMaxHeight()
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.52f),
                                        Color.Black.copy(alpha = 0.14f)
                                    )
                                )
                            )
                    )
                }

                // Audio Waveform Bars
                if (clip.trackType == TrackType.AUDIO_MUSIC && clip.waveformPeaks.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp)) {
                        val count = clip.waveformPeaks.size
                        val step = size.width / count.coerceAtLeast(1)
                        val midY = size.height / 2f
                        clip.waveformPeaks.forEachIndexed { i, amp ->
                            val barH = (amp * size.height * 0.44f).coerceAtLeast(2f)
                            val x = i * step + step * 0.5f
                            drawLine(
                                color = laneColor.copy(alpha = 0.72f),
                                start = Offset(x, midY - barH),
                                end = Offset(x, midY + barH),
                                strokeWidth = (step * 0.48f).coerceIn(1.5f, 3.2f)
                            )
                        }
                    }
                }

                // Keyframe Diamonds
                if (clip.keyframes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        clip.keyframes.forEach { kf ->
                            val frac = (kf.timeOffsetMs.toFloat() / clip.durationMs.coerceAtLeast(1L).toFloat())
                                .coerceIn(0.03f, 0.97f)
                            val cx = size.width * frac
                            val cy = size.height - 7.dp.toPx()
                            val r = 4.dp.toPx()
                            val diamond = Path().apply {
                                moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
                            }
                            drawPath(diamond, color = WarmAmber)
                        }
                    }
                }

                // Clip Label + Duration Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isSelected) 12.dp else 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clip.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1fs", clip.durationMs / 1000f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (clip.speed != 1.0f) NeonCyan else Color.White.copy(alpha = 0.85f)
                    )
                }

                // Left & Right Trim Handles when Selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .pointerInput(clip.id, pxPerMs) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = (dragAmount / pxPerMs).toLong()
                                    if (deltaMs != 0L) onTrimClip(clip.id, deltaMs, 0L)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.width(2.dp).height(12.dp).background(OledBlack, CircleShape))
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .pointerInput(clip.id, pxPerMs) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = (dragAmount / pxPerMs).toLong()
                                    if (deltaMs != 0L) onTrimClip(clip.id, 0L, deltaMs)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.width(2.dp).height(12.dp).background(OledBlack, CircleShape))
                    }
                }
            }
        }

        // Yellow Beat Markers & Volume Keyframe Dots on Audio Track
        if (volumeKeyframeTimes.isNotEmpty() || beatMarkersMs.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                beatMarkersMs.forEach { tMs ->
                    val x = tMs * pxPerMs
                    drawLine(
                        color = BeatMarkerYellow.copy(alpha = 0.78f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.4.dp.toPx()
                    )
                    drawCircle(color = BeatMarkerYellow, radius = 3.dp.toPx(), center = Offset(x, size.height - 5.dp.toPx()))
                }
                volumeKeyframeTimes.forEach { tMs ->
                    val x = tMs * pxPerMs
                    drawCircle(color = NeonCyan, radius = 4.dp.toPx(), center = Offset(x, size.height * 0.25f))
                }
            }
        }
    }
}
