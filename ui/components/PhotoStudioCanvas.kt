package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.PhotoStudioEngine
import com.example.model.DslrApertureStop
import com.example.model.PhotoCanvasInteractionTool
import com.example.model.PhotoStudioUiState
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive Before/After Split-Slider DSLR Photo Preview Canvas.
 * Renders the original image on the left of the split bar and the live AI-enhanced /
 * DSLR / RAW 16-bit / Tone-Curve / Selective-Masked bitmap on the right, while supporting
 * tap-to-focus bokeh, selective brush painting, blemish spot healing, and magic object erasing.
 */
@Composable
fun PhotoPreviewSplitCanvas(
    uiState: PhotoStudioUiState,
    photoEngine: PhotoStudioEngine,
    onTapCanvas: (normX: Float, normY: Float) -> Unit,
    onBrushStroke: (normX: Float, normY: Float) -> Unit,
    onUpdateSplitPosition: (Float) -> Unit,
    onToggleSplitCompare: () -> Unit,
    onSelectInteractionTool: (PhotoCanvasInteractionTool) -> Unit,
    onClearBrushAndHeals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var originalImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var enhancedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var isRenderingFrame by remember { mutableStateOf(false) }

    val latestState by rememberUpdatedState(uiState)

    // Decode & render processed bitmap off the main thread with slight debounce during fast slider drags
    LaunchedEffect(uiState.sourceUri, uiState.settings) {
        isRenderingFrame = true
        delay(28L)
        val (origBmp, procBmp) = withContext(Dispatchers.Default) {
            val src = photoEngine.loadSourceBitmap(uiState.sourceUri, maxDim = 900)
            val proc = photoEngine.processPhotoBitmap(
                source = src,
                settings = uiState.settings,
                targetWidth = src.width,
                targetHeight = src.height
            )
            src.asImageBitmap() to proc.asImageBitmap()
        }
        originalImage = origBmp
        enhancedImage = procBmp
        isRenderingFrame = false
    }

    val strokeOrHealCount = uiState.settings.blemishSpots.size +
        uiState.settings.magicEraseStrokes.size +
        uiState.settings.selectiveStrokes.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0B10))
            .border(1.dp, palette.glassBorderBrush, RoundedCornerShape(20.dp))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(OledBlack)
                .testTag("photo_studio_preview_canvas")
        ) {
            val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val density = LocalDensity.current

            val bmp = enhancedImage ?: originalImage
            val imgRect = remember(bmp?.width, bmp?.height, boxW, boxH) {
                if (bmp == null) {
                    androidx.compose.ui.geometry.Rect(0f, 0f, boxW, boxH)
                } else {
                    val scale = min(boxW / bmp.width.toFloat(), boxH / bmp.height.toFloat())
                    val drawW = bmp.width * scale
                    val drawH = bmp.height * scale
                    val left = (boxW - drawW) * 0.5f
                    val top = (boxH - drawH) * 0.5f
                    androidx.compose.ui.geometry.Rect(left, top, left + drawW, top + drawH)
                }
            }

            // Unified Canvas gesture layer: handles Split Wipe dragging, Tap-to-Focus, Blemish Heal, and Brush/Magic Eraser
            var draggingSplitLine by remember { mutableStateOf(false) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.activeInteractionTool, uiState.splitCompareEnabled, imgRect) {
                        detectTapGestures { offset ->
                            if (imgRect.width <= 1f || imgRect.height <= 1f) return@detectTapGestures
                            val nx = ((offset.x - imgRect.left) / imgRect.width).coerceIn(0.02f, 0.98f)
                            val ny = ((offset.y - imgRect.top) / imgRect.height).coerceIn(0.02f, 0.98f)
                            onTapCanvas(nx, ny)
                        }
                    }
                    .pointerInput(uiState.activeInteractionTool, uiState.splitCompareEnabled, imgRect) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val splitX = imgRect.left + imgRect.width * latestState.splitComparePosition
                                draggingSplitLine = latestState.splitCompareEnabled && (
                                    latestState.activeInteractionTool == PhotoCanvasInteractionTool.REFOCUS_OR_SPLIT ||
                                        abs(startOffset.x - splitX) <= 56f
                                    )
                            },
                            onDragEnd = { draggingSplitLine = false },
                            onDragCancel = { draggingSplitLine = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val pos = change.position
                                if (imgRect.width <= 1f || imgRect.height <= 1f) return@detectDragGestures
                                if (draggingSplitLine) {
                                    val newSplit = ((pos.x - imgRect.left) / imgRect.width).coerceIn(0.05f, 0.95f)
                                    onUpdateSplitPosition(newSplit)
                                } else if (
                                    latestState.activeInteractionTool == PhotoCanvasInteractionTool.BRUSH_BRIGHTEN_FACE ||
                                    latestState.activeInteractionTool == PhotoCanvasInteractionTool.BRUSH_DARKEN_BG ||
                                    latestState.activeInteractionTool == PhotoCanvasInteractionTool.MAGIC_ERASER
                                ) {
                                    val nx = ((pos.x - imgRect.left) / imgRect.width).coerceIn(0.02f, 0.98f)
                                    val ny = ((pos.y - imgRect.top) / imgRect.height).coerceIn(0.02f, 0.98f)
                                    onBrushStroke(nx, ny)
                                }
                            }
                        )
                    }
            ) {
                val orig = originalImage
                val proc = enhancedImage
                val dstOff = IntOffset(imgRect.left.roundToInt(), imgRect.top.roundToInt())
                val dstSz = IntSize(imgRect.width.roundToInt().coerceAtLeast(1), imgRect.height.roundToInt().coerceAtLeast(1))

                if (orig != null && proc != null) {
                    if (uiState.splitCompareEnabled) {
                        val splitX = imgRect.left + imgRect.width * uiState.splitComparePosition
                        // Left Side: Original Unprocessed Image
                        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                            drawImage(
                                image = orig,
                                dstOffset = dstOff,
                                dstSize = dstSz
                            )
                        }
                        // Right Side: AI Super-Resolution + DSLR Enhanced Image
                        clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                            drawImage(
                                image = proc,
                                dstOffset = dstOff,
                                dstSize = dstSz
                            )
                        }

                        // Glowing Split Divider Line
                        drawLine(
                            brush = Brush.verticalGradient(listOf(ElectricPurple, NeonCyan, ElectricPurple)),
                            start = Offset(splitX, imgRect.top),
                            end = Offset(splitX, imgRect.bottom),
                            strokeWidth = 2.4.dp.toPx()
                        )
                    } else {
                        drawImage(
                            image = proc,
                            dstOffset = dstOff,
                            dstSize = dstSz
                        )
                    }
                } else if (orig != null) {
                    drawImage(image = orig, dstOffset = dstOff, dstSize = dstSz)
                }

                // DSLR Optical Bokeh Focus Target Reticle (shown when aperture < f/8.0)
                if (uiState.settings.apertureStop != DslrApertureStop.F8_0) {
                    val fx = imgRect.left + imgRect.width * uiState.settings.bokehFocusX
                    val fy = imgRect.top + imgRect.height * uiState.settings.bokehFocusY
                    drawCircle(
                        color = ProGold.copy(alpha = 0.85f),
                        radius = 20.dp.toPx(),
                        center = Offset(fx, fy),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawCircle(
                        color = ProGold,
                        radius = 3.dp.toPx(),
                        center = Offset(fx, fy)
                    )
                }

                // Selective Radial Mask Overlay Preview Ring
                if (uiState.settings.radialMask.enabled) {
                    val rm = uiState.settings.radialMask
                    val rx = imgRect.left + imgRect.width * rm.centerX
                    val ry = imgRect.top + imgRect.height * rm.centerY
                    val radPx = min(imgRect.width, imgRect.height) * rm.radius
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.72f),
                        radius = radPx,
                        center = Offset(rx, ry),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                // Selective Brush / Magic Eraser / Blemish Marker Hints
                if (uiState.activeInteractionTool != PhotoCanvasInteractionTool.REFOCUS_OR_SPLIT) {
                    uiState.settings.selectiveStrokes.forEach { st ->
                        val cx = imgRect.left + imgRect.width * st.normX
                        val cy = imgRect.top + imgRect.height * st.normY
                        val color = if (st.tool == PhotoCanvasInteractionTool.BRUSH_BRIGHTEN_FACE) {
                            ProGold.copy(alpha = 0.35f)
                        } else {
                            ElectricPurple.copy(alpha = 0.35f)
                        }
                        drawCircle(color = color, radius = 14.dp.toPx(), center = Offset(cx, cy))
                    }
                    uiState.settings.magicEraseStrokes.forEach { st ->
                        val cx = imgRect.left + imgRect.width * st.normX
                        val cy = imgRect.top + imgRect.height * st.normY
                        drawCircle(color = CoralRed.copy(alpha = 0.42f), radius = 11.dp.toPx(), center = Offset(cx, cy))
                    }
                    uiState.settings.blemishSpots.forEach { spot ->
                        val cx = imgRect.left + imgRect.width * spot.normX
                        val cy = imgRect.top + imgRect.height * spot.normY
                        drawCircle(
                            color = AcidLime.copy(alpha = 0.75f),
                            radius = 7.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }

            // Draggable Center Handle Pill on the Split Line
            if (uiState.splitCompareEnabled && imgRect.width > 10f) {
                val handleX = with(density) { (imgRect.left + imgRect.width * uiState.splitComparePosition).toDp() - 16.dp }
                val handleY = with(density) { (imgRect.top + imgRect.height * 0.5f).toDp() - 16.dp }
                Surface(
                    shape = CircleShape,
                    color = OledBlack.copy(alpha = 0.88f),
                    border = BorderStroke(1.5.dp, NeonCyan),
                    modifier = Modifier
                        .offset(x = handleX, y = handleY)
                        .size(32.dp)
                        .testTag("photo_split_slider_handle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Compare,
                            contentDescription = "Swipe Before and After Split Slider",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Top-left "BEFORE" and Top-right "AI DSLR" labels
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OledBlack.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "BEFORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricPurple.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    val badgeText = buildString {
                        append("AI DSLR ")
                        append(uiState.settings.upscaleMultiplier.label)
                        if (uiState.settings.apertureStop != DslrApertureStop.F8_0) {
                            append(" • ${uiState.settings.apertureStop.fStopLabel}")
                        }
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isRenderingFrame || uiState.isProcessingEnhance) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = OledBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (uiState.isProcessingEnhance) "Enhancing Ultra-HD…" else "Rendering…",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Bottom Canvas Control Bar: Split Toggle + Active Touch Tool + Clear Strokes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.splitCompareEnabled) NeonCyan.copy(alpha = 0.18f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (uiState.splitCompareEnabled) NeonCyan else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleSplitCompare)
                        .testTag("photo_split_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Compare,
                            contentDescription = "Toggle Before After Split Slider",
                            tint = if (uiState.splitCompareEnabled) NeonCyan else palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (uiState.splitCompareEnabled) "Split ON" else "Split OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.splitCompareEnabled) NeonCyan else palette.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val tools = PhotoCanvasInteractionTool.entries
                            val next = tools[(uiState.activeInteractionTool.ordinal + 1) % tools.size]
                            onSelectInteractionTool(next)
                        }
                        .testTag("photo_canvas_tool_cycle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = "Active Canvas Interaction Tool",
                            tint = ProGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = uiState.activeInteractionTool.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ProGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (strokeOrHealCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CoralRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearBrushAndHeals)
                        .testTag("photo_clear_strokes_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear brush strokes and heals",
                            tint = CoralRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Clear ($strokeOrHealCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "${uiState.sourceWidthPx}×${uiState.sourceHeightPx}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
            }
        }
    }
}
