package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.MediaFrameEngine
import com.example.media.VideoCanvasRenderer
import com.example.model.SafeZonePreset
import com.example.model.TrackType
import com.example.model.VideoProjectDocument
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

enum class CanvasGestureMode(val label: String) {
    VIEWPORT("View"),
    CLIP_TRANSFORM("Clip")
}

@Composable
fun InteractivePreviewCanvas(
    document: VideoProjectDocument,
    playheadMs: Long,
    frameRevision: Int,
    comparingOriginal: Boolean,
    canvasRenderer: VideoCanvasRenderer,
    frameEngine: MediaFrameEngine,
    onTogglePlayback: () -> Unit,
    onSetComparingOriginal: (Boolean) -> Unit,
    onImportEmptyCanvasMedia: () -> Unit,
    onClipMultiTouchTransform: (zoomFactor: Float, panDeltaXNorm: Float, panDeltaYNorm: Float) -> Unit,
    onResetClipTransform: () -> Unit,
    isPlaying: Boolean = false,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    safeZonePreset: SafeZonePreset = SafeZonePreset.OFF,
    onCycleSafeZone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var gestureMode by remember(document.id) { mutableStateOf(CanvasGestureMode.VIEWPORT) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isGestureActive by remember { mutableStateOf(false) }
    var showSnapCenterX by remember { mutableStateOf(false) }
    var showSnapCenterY by remember { mutableStateOf(false) }

    val viewportScale = remember(document.id) { Animatable(1f) }
    val viewportOffset = remember(document.id) { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Reset viewport cleanly if aspect ratio changes
    LaunchedEffect(document.aspectRatio) {
        viewportScale.snapTo(1f)
        viewportOffset.snapTo(Offset.Zero)
    }

    val latestTogglePlayback by rememberUpdatedState(onTogglePlayback)
    val latestClipTransform by rememberUpdatedState(onClipMultiTouchTransform)
    val latestResetClip by rememberUpdatedState(onResetClipTransform)
    val latestGestureMode by rememberUpdatedState(gestureMode)

    val snapThresholdPx = with(density) { 8.dp.toPx() }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTimeMs by remember { mutableStateOf(0L) }

    fun clampViewportOffset(raw: Offset, scale: Float, size: IntSize): Offset {
        if (size.width <= 0 || size.height <= 0) return Offset.Zero
        val maxX = ((size.width * (scale - 1f)) / 2f) + size.width * 0.25f
        val maxY = ((size.height * (scale - 1f)) / 2f) + size.height * 0.25f
        return Offset(
            x = raw.x.coerceIn(-maxX, maxX),
            y = raw.y.coerceIn(-maxY, maxY)
        )
    }

    fun animateResetViewport() {
        scope.launch {
            launch {
                viewportScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                viewportOffset.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
    }

    val isViewportTransformed = viewportScale.value > 1.015f ||
        abs(viewportOffset.value.x) > 2f ||
        abs(viewportOffset.value.y) > 2f

    val borderColor = when {
        comparingOriginal -> ProGold
        isGestureActive -> NeonCyan
        gestureMode == CanvasGestureMode.CLIP_TRANSFORM -> ElectricPurple.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.11f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(document.aspectRatio.ratio, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isGestureActive || comparingOriginal) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .onSizeChanged { canvasSize = it }
            .pointerInput(document.id, snapThresholdPx) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var hadMultiTouch = false
                    var didDrag = false
                    var totalSingleDrag = 0f
                    var snappedXTriggered = false
                    var snappedYTriggered = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        if (pressed.size >= 2) {
                            // Active multi-touch: Pinch-to-Zoom + Two-Finger Pan
                            hadMultiTouch = true
                            pendingSingleTapJob?.cancel()
                            if (!isGestureActive) isGestureActive = true

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)

                            val sizeW = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val sizeH = canvasSize.height.toFloat().coerceAtLeast(1f)

                            if (latestGestureMode == CanvasGestureMode.VIEWPORT) {
                                val prevScale = viewportScale.value
                                val nextScale = (prevScale * zoomChange).coerceIn(1.0f, 5.0f)
                                val scaleRatio = if (prevScale > 0f) nextScale / prevScale else 1f
                                val center = Offset(sizeW / 2f, sizeH / 2f)
                                val focal = if (centroid != Offset.Unspecified) centroid - center else Offset.Zero

                                var nextOffset = (viewportOffset.value - focal) * scaleRatio + focal + panChange
                                nextOffset = clampViewportOffset(nextOffset, nextScale, canvasSize)

                                // Subtle magnetic center snap while panning
                                val nearCenterX = abs(nextOffset.x) <= snapThresholdPx
                                val nearCenterY = abs(nextOffset.y) <= snapThresholdPx
                                showSnapCenterX = nearCenterX
                                showSnapCenterY = nearCenterY
                                if (nearCenterX && !snappedXTriggered && abs(panChange.x) > 0.5f) {
                                    snappedXTriggered = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (!nearCenterX) {
                                    snappedXTriggered = false
                                }
                                if (nearCenterY && !snappedYTriggered && abs(panChange.y) > 0.5f) {
                                    snappedYTriggered = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (!nearCenterY) {
                                    snappedYTriggered = false
                                }

                                scope.launch {
                                    viewportScale.snapTo(nextScale)
                                    viewportOffset.snapTo(nextOffset)
                                }
                            } else {
                                // Clip Transform Mode: apply pinch/pan directly to active video or PIP layer
                                if (abs(zoomChange - 1f) > 0.001f || panChange.getDistance() > 0.5f) {
                                    latestClipTransform(
                                        zoomChange,
                                        panChange.x / sizeW,
                                        panChange.y / sizeH
                                    )
                                }
                            }

                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                        } else if (!hadMultiTouch) {
                            // Single finger: allow panning when already zoomed in, otherwise preserve tap/double-tap
                            val change = pressed.first()
                            val delta = change.position - change.previousPosition
                            val dist = delta.getDistance()
                            if (dist > 0f) {
                                totalSingleDrag += dist
                                val canSinglePan = latestGestureMode == CanvasGestureMode.VIEWPORT &&
                                    (viewportScale.value > 1.03f || viewportOffset.value.getDistance() > 4f)
                                if (canSinglePan && totalSingleDrag > viewConfiguration.touchSlop) {
                                    didDrag = true
                                    pendingSingleTapJob?.cancel()
                                    if (!isGestureActive) isGestureActive = true
                                    val nextOffset = clampViewportOffset(
                                        viewportOffset.value + delta,
                                        viewportScale.value,
                                        canvasSize
                                    )
                                    showSnapCenterX = abs(nextOffset.x) <= snapThresholdPx
                                    showSnapCenterY = abs(nextOffset.y) <= snapThresholdPx
                                    scope.launch { viewportOffset.snapTo(nextOffset) }
                                    change.consume()
                                }
                            }
                        }
                    }

                    isGestureActive = false
                    showSnapCenterX = false
                    showSnapCenterY = false

                    // Snap back to exact center if user pinched back to 1.0x near center
                    if (hadMultiTouch && latestGestureMode == CanvasGestureMode.VIEWPORT) {
                        if (viewportScale.value <= 1.04f && viewportOffset.value.getDistance() < snapThresholdPx * 3f) {
                            animateResetViewport()
                        }
                    }

                    // Clean Tap vs Double-Tap detection (never fires after pinch/pan)
                    if (!hadMultiTouch && !didDrag && totalSingleDrag < viewConfiguration.touchSlop) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTimeMs <= 260L) {
                            lastTapTimeMs = 0L
                            pendingSingleTapJob?.cancel()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (latestGestureMode == CanvasGestureMode.VIEWPORT) {
                                if (viewportScale.value > 1.03f || viewportOffset.value.getDistance() > 4f) {
                                    animateResetViewport()
                                } else {
                                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                                    val tapFocal = firstDown.position - center
                                    val targetScale = 2.0f
                                    val targetOffset = clampViewportOffset(
                                        raw = -tapFocal * (targetScale - 1f),
                                        scale = targetScale,
                                        size = canvasSize
                                    )
                                    scope.launch {
                                        launch { viewportScale.animateTo(targetScale, spring(stiffness = Spring.StiffnessMediumLow)) }
                                        launch { viewportOffset.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                }
                            } else {
                                latestResetClip()
                            }
                        } else {
                            lastTapTimeMs = now
                            pendingSingleTapJob?.cancel()
                            pendingSingleTapJob = scope.launch {
                                delay(265L)
                                latestTogglePlayback()
                            }
                        }
                    }
                }
            }
            .testTag("live_preview_canvas")
    ) {
        // 1. Multi-Touch Transformed Live Preview Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = viewportScale.value
                    scaleY = viewportScale.value
                    translationX = viewportOffset.value.x
                    translationY = viewportOffset.value.y
                }
        ) {
            val _rev = frameRevision
            canvasRenderer.renderFrame(
                canvas = drawContext.canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                document = document,
                playheadMs = playheadMs,
                frameEngine = frameEngine,
                maxDecodeDim = 720,
                compareOriginal = comparingOriginal
            )
        }

        // 2. Center Alignment Crosshair Guides while Panning Near Center
        if (isGestureActive && (showSnapCenterX || showSnapCenterY)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                if (showSnapCenterX) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.85f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dash
                    )
                }
                if (showSnapCenterY) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.85f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dash
                    )
                }
            }
        }

        // 2b. Toggleable Platform Safe-Zone Guides (Reels/Shorts, YouTube 16:9, IG/FB Feed)
        if (safeZonePreset != SafeZonePreset.OFF) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                val dangerFill = Color(0x38FF3366)
                val guideStroke = Stroke(width = 1.2.dp.toPx(), pathEffect = dash)
                when (safeZonePreset) {
                    SafeZonePreset.REELS_SHORTS -> {
                        // Top status/header bar + Bottom caption/audio zone + Right like/comment/share column
                        val topH = size.height * 0.13f
                        val bottomH = size.height * 0.22f
                        val rightW = size.width * 0.17f
                        drawRect(color = dangerFill, topLeft = Offset.Zero, size = Size(size.width, topH))
                        drawRect(color = dangerFill, topLeft = Offset(0f, size.height - bottomH), size = Size(size.width, bottomH))
                        drawRect(color = dangerFill, topLeft = Offset(size.width - rightW, topH), size = Size(rightW, size.height - topH - bottomH))
                        drawRect(
                            color = NeonCyan.copy(alpha = 0.85f),
                            topLeft = Offset(size.width * 0.04f, topH),
                            size = Size(size.width * 0.79f, size.height - topH - bottomH),
                            style = guideStroke
                        )
                    }
                    SafeZonePreset.YOUTUBE_16_9 -> {
                        // Title-safe 10% inner margin & bottom player scrub bar
                        val mx = size.width * 0.08f
                        val my = size.height * 0.10f
                        drawRect(color = dangerFill, topLeft = Offset(0f, size.height * 0.86f), size = Size(size.width, size.height * 0.14f))
                        drawRect(
                            color = ProGold.copy(alpha = 0.85f),
                            topLeft = Offset(mx, my),
                            size = Size(size.width - mx * 2f, size.height * 0.76f),
                            style = guideStroke
                        )
                    }
                    SafeZonePreset.INSTAGRAM_FEED -> {
                        // 1:1 & 4:5 centered feed crop safe box
                        val sq = minOf(size.width, size.height) * 0.88f
                        val left = (size.width - sq) / 2f
                        val top = (size.height - sq) / 2f
                        drawRect(
                            color = ElectricPurple.copy(alpha = 0.9f),
                            topLeft = Offset(left, top),
                            size = Size(sq, sq),
                            style = guideStroke
                        )
                    }
                    SafeZonePreset.OFF -> Unit
                }
            }
        }

        // 3. Clean Empty-Canvas Real Media Import CTA
        val hasAnyVisual = document.clips.any {
            it.trackType == TrackType.MAIN_VIDEO || it.trackType == TrackType.OVERLAY_PIP
        }
        if (!hasAnyVisual) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ElectricPurple.copy(alpha = 0.24f),
                    border = BorderStroke(1.2.dp, NeonCyan),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onImportEmptyCanvasMedia)
                        .testTag("empty_canvas_import_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Import Media",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Select real videos or photos from Gallery",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // 4. Top-Left Active Effects / Compare HUD Badge
        val activeTag = if (comparingOriginal) {
            "ORIGINAL (BEFORE)"
        } else {
            buildString {
                if (document.colorGrading.lutPreset.name != "ORIGINAL") append(document.colorGrading.lutPreset.displayName)
                if (document.cutout.mode.name != "OFF") {
                    if (isNotEmpty()) append(" · ")
                    append(document.cutout.mode.label)
                }
                if (document.visualFx.activeFx.name != "NONE") {
                    if (isNotEmpty()) append(" · ")
                    append(document.visualFx.activeFx.label)
                }
                if (document.visualFx.motionBlur > 0.05f) {
                    if (isNotEmpty()) append(" · ")
                    append("Blur ${(document.visualFx.motionBlur * 100).toInt()}%")
                }
            }
        }
        if (activeTag.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (comparingOriginal) ProGold else OledBlack.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = activeTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (comparingOriginal) OledBlack else NeonCyan,
                    fontWeight = if (comparingOriginal) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        // 5. Top-Right HUD Controls: Safe-Zone Guides Toggle + Multi-Touch Mode Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (safeZonePreset != SafeZonePreset.OFF) ProGold.copy(alpha = 0.88f) else OledBlack.copy(alpha = 0.72f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (safeZonePreset != SafeZonePreset.OFF) Color.White else StudioBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCycleSafeZone()
                    }
                    .testTag("canvas_safe_zone_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridOn,
                        contentDescription = stringResource(R.string.cd_safe_zone_guide),
                        tint = if (safeZonePreset != SafeZonePreset.OFF) OledBlack else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (safeZonePreset == SafeZonePreset.OFF) "Safe" else safeZonePreset.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (safeZonePreset != SafeZonePreset.OFF) OledBlack else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (gestureMode == CanvasGestureMode.CLIP_TRANSFORM) {
                    ElectricPurple.copy(alpha = 0.85f)
                } else {
                    OledBlack.copy(alpha = 0.72f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (gestureMode == CanvasGestureMode.CLIP_TRANSFORM) NeonCyan else StudioBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        gestureMode = if (gestureMode == CanvasGestureMode.VIEWPORT) {
                            CanvasGestureMode.CLIP_TRANSFORM
                        } else {
                            CanvasGestureMode.VIEWPORT
                        }
                    }
                    .testTag("canvas_gesture_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (gestureMode == CanvasGestureMode.VIEWPORT) Icons.Filled.ZoomIn else Icons.Filled.OpenWith,
                        contentDescription = stringResource(R.string.cd_toggle_canvas_gesture_mode),
                        tint = if (gestureMode == CanvasGestureMode.CLIP_TRANSFORM) Color.White else NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = gestureMode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 6. Bottom-Left Live Zoom Indicator & 1-Tap Reset Pill (Appears when pinched/panned)
        AnimatedVisibility(
            visible = isViewportTransformed || isGestureActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OledBlack.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.75f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        animateResetViewport()
                    }
                    .testTag("canvas_reset_zoom_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = stringResource(R.string.cd_reset_canvas_zoom),
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fx", viewportScale.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    if (isViewportTransformed) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // 6b. Floating Translucent Play/Pause, Accurate Timecode & Fullscreen HUD Pill (visible when paused or fullscreen)
        AnimatedVisibility(
            visible = hasAnyVisual && (!isPlaying || isFullscreen) && !isGestureActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = OledBlack.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.testTag("canvas_floating_transport_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTogglePlayback()
                            }
                            .testTag("canvas_floating_play_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${formatHudTimecode(playheadMs)} / ${formatHudTimecode(document.totalDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (onToggleFullscreen != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleFullscreen()
                                }
                                .testTag("canvas_floating_fullscreen_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = stringResource(R.string.cd_fullscreen),
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Bottom-Right Hold-to-Compare Before / After Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (comparingOriginal) ProGold else OledBlack.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, if (comparingOriginal) Color.White else ProGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetComparingOriginal(true)
                        waitForUpOrCancellation()
                        onSetComparingOriginal(false)
                    }
                }
                .testTag("hold_compare_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Compare,
                    contentDescription = stringResource(R.string.cd_compare_before_after),
                    tint = if (comparingOriginal) OledBlack else ProGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (comparingOriginal) "Original" else "Compare",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (comparingOriginal) OledBlack else TextPrimary
                )
            }
        }
    }
}

private fun formatHudTimecode(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val mins = clamped / 60_000L
    val secs = (clamped % 60_000L) / 1000L
    val centis = (clamped % 1000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, centis)
}
