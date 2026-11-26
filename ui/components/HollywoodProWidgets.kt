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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorGrading
import com.example.model.HslColorBand
import com.example.model.KeyframeEasing
import com.example.model.defaultHslBands
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TrackAudioAccent
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val DefaultToneCurve = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/**
 * Interactive DaVinci Resolve-style RGB Tone Curves editor (Master, Red, Green, Blue)
 * with 5 draggable spline control points.
 */
@Composable
fun RgbToneCurveEditor(
    grading: ColorGrading,
    onUpdate: ((ColorGrading) -> ColorGrading) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var channel by remember { mutableIntStateOf(0) } // 0=Master, 1=R, 2=G, 3=B
    var activeNode by remember { mutableIntStateOf(-1) }

    val channelMeta = listOf(
        "Master" to Color.White,
        "Red" to Color(0xFFFF4D67),
        "Green" to Color(0xFF22C55E),
        "Blue" to Color(0xFF38BDF8)
    )
    val curveColor = channelMeta[channel].second
    val activePoints = when (channel) {
        1 -> grading.toneCurveRed
        2 -> grading.toneCurveGreen
        3 -> grading.toneCurveBlue
        else -> grading.toneCurveMaster
    }.let { if (it.size == 5) it else DefaultToneCurve }

    fun writePoints(next: List<Float>) {
        onUpdate { g ->
            when (channel) {
                1 -> g.copy(toneCurveRed = next)
                2 -> g.copy(toneCurveGreen = next)
                3 -> g.copy(toneCurveBlue = next)
                else -> g.copy(toneCurveMaster = next)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                channelMeta.forEachIndexed { idx, (label, accent) ->
                    val selected = idx == channel
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) accent.copy(alpha = 0.22f) else palette.surfaceElevated,
                        border = BorderStroke(1.dp, if (selected) accent else palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { channel = idx }
                            .testTag("tone_curve_channel_$label")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) accent else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { writePoints(DefaultToneCurve) }
                    .testTag("tone_curve_reset_button")
            ) {
                Text(
                    text = "Reset Curve",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledBlack)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .testTag("rgb_tone_curve_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .pointerInput(channel, activePoints) {
                        detectTapGestures { pos ->
                            val idx = ((pos.x / size.width.coerceAtLeast(1)) * 4f).roundToInt().coerceIn(0, 4)
                            val yNorm = (1f - pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val next = activePoints.toMutableList()
                            next[idx] = yNorm
                            writePoints(next)
                        }
                    }
                    .pointerInput(channel, activePoints) {
                        detectDragGestures(
                            onDragStart = { start ->
                                activeNode = ((start.x / size.width.coerceAtLeast(1)) * 4f).roundToInt().coerceIn(0, 4)
                            },
                            onDragEnd = { activeNode = -1 },
                            onDragCancel = { activeNode = -1 },
                            onDrag = { change, _ ->
                                if (activeNode in 0..4) {
                                    change.consume()
                                    val yNorm = (1f - change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val next = activePoints.toMutableList()
                                    next[activeNode] = yNorm
                                    writePoints(next)
                                }
                            }
                        )
                    }
            ) {
                // 4x4 studio luminance grid + identity diagonal
                val gridColor = Color.White.copy(alpha = 0.08f)
                for (i in 1..3) {
                    val gx = size.width * (i / 4f)
                    val gy = size.height * (i / 4f)
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                val coords = activePoints.mapIndexed { idx, v ->
                    Offset(
                        x = size.width * (idx / 4f),
                        y = (1f - v.coerceIn(0f, 1f)) * size.height
                    )
                }
                val path = Path().apply {
                    moveTo(coords[0].x, coords[0].y)
                    for (i in 1 until coords.size) {
                        val p0 = coords[i - 1]
                        val p1 = coords[i]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(coords.last().x, size.height)
                    lineTo(coords.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(curveColor.copy(alpha = 0.24f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                coords.forEachIndexed { idx, pt ->
                    val isSelected = idx == activeNode
                    drawCircle(
                        color = OledBlack,
                        radius = if (isSelected) 7.dp.toPx() else 5.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) ProGold else curveColor,
                        radius = if (isSelected) 5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt
                    )
                }
            }
            Text(
                text = "Shadows • Mids • Highlights",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

/**
 * 8-Color HSL Color Mixer (Red, Orange, Yellow, Green, Aqua, Blue, Purple, Magenta)
 * with individual Hue, Saturation, and Luminance sliders.
 */
@Composable
fun HslColorMixerPanel(
    bands: List<HslColorBand>,
    onUpdateBand: (index: Int, (HslColorBand) -> HslColorBand) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val resolvedBands = if (bands.size >= 8) bands else defaultHslBands()
    var selectedIdx by remember { mutableIntStateOf(0) }
    val active = resolvedBands[selectedIdx.coerceIn(0, resolvedBands.lastIndex)]

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "8-Color HSL Mixer (${active.name})",
                style = MaterialTheme.typography.labelSmall,
                color = ProGold,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .clickable {
                        onUpdateBand(selectedIdx) {
                            it.copy(hueShift = 0f, saturationShift = 0f, luminanceShift = 0f)
                        }
                    }
                    .testTag("hsl_reset_band_button")
            ) {
                Text(
                    text = "Reset ${active.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(resolvedBands) { idx, band ->
                val selected = idx == selectedIdx
                val swatch = Color(band.swatchHex)
                val modified = band.hueShift != 0f || band.saturationShift != 0f || band.luminanceShift != 0f
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (selected) swatch.copy(alpha = 0.24f) else palette.surfaceElevated,
                    border = BorderStroke(
                        width = if (selected) 1.6.dp else 1.dp,
                        color = if (selected) swatch else if (modified) ProGold.copy(alpha = 0.6f) else palette.border
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable { selectedIdx = idx }
                        .testTag("hsl_band_${band.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(swatch)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = band.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        CompactProSlider(
            label = "${active.name} Hue",
            value = active.hueShift,
            range = -1f..1f,
            valueLabel = formatSignedPercent(active.hueShift)
        ) { v -> onUpdateBand(selectedIdx) { it.copy(hueShift = v) } }

        CompactProSlider(
            label = "${active.name} Saturation",
            value = active.saturationShift,
            range = -1f..1f,
            valueLabel = formatSignedPercent(active.saturationShift)
        ) { v -> onUpdateBand(selectedIdx) { it.copy(saturationShift = v) } }

        CompactProSlider(
            label = "${active.name} Luminance",
            value = active.luminanceShift,
            range = -1f..1f,
            valueLabel = formatSignedPercent(active.luminanceShift)
        ) { v -> onUpdateBand(selectedIdx) { it.copy(luminanceShift = v) } }
    }
}

/**
 * After Effects-style visual Speed/Bezier Easing Graph Editor supporting
 * Linear, Ease-In, Ease-Out, Ease I/O (custom Bezier handles), Overshoot, Bounce, and Elastic.
 */
@Composable
fun BezierEasingGraphEditor(
    activeEasing: KeyframeEasing,
    bezierX1: Float,
    bezierY1: Float,
    bezierX2: Float,
    bezierY2: Float,
    onSelectEasing: (KeyframeEasing) -> Unit,
    onUpdateBezier: (x1: Float, y1: Float, x2: Float, y2: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var dragHandle by remember { mutableIntStateOf(0) } // 1=P1, 2=P2

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(KeyframeEasing.entries) { easing ->
                val active = easing == activeEasing
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectEasing(easing) }
                        .testTag("keyframe_easing_${easing.name}")
                ) {
                    Text(
                        text = easing.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) NeonCyan else palette.textSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledBlack)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .testTag("bezier_graph_editor_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .pointerInput(activeEasing, bezierX1, bezierY1, bezierX2, bezierY2) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val p1 = Offset(bezierX1 * w, (1f - bezierY1) * h)
                                val p2 = Offset(bezierX2 * w, (1f - bezierY2) * h)
                                val d1 = (start - p1).getDistance()
                                val d2 = (start - p2).getDistance()
                                dragHandle = if (d1 <= d2 && d1 < 95f) 1 else if (d2 < 95f) 2 else 0
                            },
                            onDragEnd = { dragHandle = 0 },
                            onDragCancel = { dragHandle = 0 },
                            onDrag = { change, _ ->
                                if (dragHandle != 0) {
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val nx = (change.position.x / w).coerceIn(0.02f, 0.98f)
                                    val ny = (1f - change.position.y / h).coerceIn(-0.35f, 1.35f)
                                    if (dragHandle == 1) {
                                        onUpdateBezier(nx, ny, bezierX2, bezierY2)
                                    } else {
                                        onUpdateBezier(bezierX1, bezierY1, nx, ny)
                                    }
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val grid = Color.White.copy(alpha = 0.08f)
                drawLine(grid, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 1f)
                drawLine(grid, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                val curvePath = Path()
                val steps = 40
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val yVal = sampleEasingForGraph(t, activeEasing, bezierX1, bezierY1, bezierX2, bezierY2)
                    val px = t * w
                    val py = (1f - yVal).coerceIn(-0.18f, 1.18f) * h
                    if (i == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
                }
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan, ProGold)),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Interactive Bezier P1 & P2 tangents
                val p1 = Offset(bezierX1.coerceIn(0f, 1f) * w, (1f - bezierY1).coerceIn(0f, 1f) * h)
                val p2 = Offset(bezierX2.coerceIn(0f, 1f) * w, (1f - bezierY2).coerceIn(0f, 1f) * h)
                drawLine(ProGold.copy(alpha = 0.55f), Offset(0f, h), p1, strokeWidth = 1.3.dp.toPx())
                drawLine(NeonCyan.copy(alpha = 0.55f), Offset(w, 0f), p2, strokeWidth = 1.3.dp.toPx())
                drawCircle(color = OledBlack, radius = 6.dp.toPx(), center = p1)
                drawCircle(color = ProGold, radius = 4.2.dp.toPx(), center = p1)
                drawCircle(color = OledBlack, radius = 6.dp.toPx(), center = p2)
                drawCircle(color = NeonCyan, radius = 4.2.dp.toPx(), center = p2)
            }

            Text(
                text = "Bezier Graph • ${activeEasing.label}",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 4.dp)
            )
            Text(
                text = String.format(Locale.US, "P1(%.2f,%.2f) P2(%.2f,%.2f)", bezierX1, bezierY1, bezierX2, bezierY2),
                fontSize = 9.sp,
                color = palette.textMuted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

private fun sampleEasingForGraph(
    t: Float,
    easing: KeyframeEasing,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
): Float = when (easing) {
    KeyframeEasing.LINEAR -> t
    KeyframeEasing.EASE_IN -> t * t * t
    KeyframeEasing.EASE_OUT -> 1f - (1f - t) * (1f - t) * (1f - t)
    KeyframeEasing.EASE_IN_OUT -> {
        val inv = 1f - t
        3f * inv * inv * t * y1 + 3f * inv * t * t * y2 + t * t * t
    }
    KeyframeEasing.EASE_OUT_BACK -> {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val tm1 = t - 1f
        1f + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1
    }
    KeyframeEasing.BOUNCE -> {
        val n1 = 7.5625f
        val d1 = 2.75f
        when {
            t < 1f / d1 -> n1 * t * t
            t < 2f / d1 -> {
                val tt = t - 1.5f / d1
                n1 * tt * tt + 0.75f
            }
            t < 2.5f / d1 -> {
                val tt = t - 2.25f / d1
                n1 * tt * tt + 0.9375f
            }
            else -> {
                val tt = t - 2.625f / d1
                n1 * tt * tt + 0.984375f
            }
        }
    }
    KeyframeEasing.CUSTOM_ELASTIC -> {
        if (t <= 0f) 0f
        else if (t >= 1f) 1f
        else {
            val c4 = (2.0 * PI) / 3.0
            (2.0.pow(-10.0 * t) * sin((t * 10.0 - 0.75) * c4) + 1.0).toFloat()
        }
    }
}

/**
 * 10-Band Parametric Studio Equalizer Rack (31Hz to 16kHz, -12dB..+12dB)
 * with vertical interactive touch faders and studio mastering presets.
 */
@Composable
fun TenBandEqualizerRack(
    eqBandsDb: List<Float>,
    onUpdateBand: (index: Int, db: Float) -> Unit,
    onApplyPreset: (List<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val bands = remember(eqBandsDb) {
        if (eqBandsDb.size == 10) eqBandsDb else List(10) { 0f }
    }
    val freqLabels = remember {
        listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    }
    val presets = remember {
        listOf(
            "Flat" to List(10) { 0f },
            "Cinema" to listOf(4.5f, 3.5f, 1.5f, 0f, -1f, 0.5f, 2f, 3.5f, 4f, 3f),
            "Vocal" to listOf(-4f, -2.5f, -1f, 1.5f, 3.5f, 4.5f, 4f, 2.5f, 1f, 0f),
            "Sub Bass" to listOf(8f, 6.5f, 4.5f, 1.5f, 0f, -0.5f, 0f, 1f, 1.5f, 1f),
            "Crisp Air" to listOf(-1f, 0f, 0f, 0.5f, 1f, 2f, 3.5f, 5f, 6.5f, 7f)
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-Band Parametric EQ (±12 dB)",
                style = MaterialTheme.typography.labelSmall,
                color = ProGold,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(presets) { (name, values) ->
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { onApplyPreset(values) }
                            .testTag("eq_preset_$name")
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledBlack)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .testTag("ten_band_eq_rack"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bands.forEachIndexed { idx, db ->
                val clampedDb = db.coerceIn(-12f, 12f)
                val barAccent = when {
                    idx < 3 -> ElectricPurple
                    idx < 7 -> NeonCyan
                    else -> TrackAudioAccent
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (clampedDb >= 0f) "+${clampedDb.roundToInt()}" else "${clampedDb.roundToInt()}",
                        fontSize = 8.sp,
                        color = if (clampedDb != 0f) barAccent else palette.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(idx) {
                                detectTapGestures { pos ->
                                    val norm = (1f - pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val nextDb = (norm * 24f) - 12f
                                    onUpdateBand(idx, nextDb)
                                }
                            }
                            .pointerInput(idx) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val norm = (1f - change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val nextDb = (norm * 24f) - 12f
                                    onUpdateBand(idx, nextDb)
                                }
                            }
                            .testTag("eq_fader_${freqLabels[idx]}")
                    ) {
                        val cx = size.width / 2f
                        val midY = size.height / 2f
                        val thumbY = (1f - (clampedDb + 12f) / 24f) * size.height
                        drawLine(
                            color = Color.White.copy(alpha = 0.14f),
                            start = Offset(cx, 3f),
                            end = Offset(cx, size.height - 3f),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = barAccent,
                            start = Offset(cx, midY),
                            end = Offset(cx, thumbY),
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = Color.White, radius = 3.6.dp.toPx(), center = Offset(cx, thumbY))
                    }
                    Text(
                        text = freqLabels[idx],
                        fontSize = 8.sp,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Interactive Freehand Lasso Masking Pad: draw any custom polygon mask with your finger
 * (persisted as normalized -0.5f..0.5f x,y pairs on TimelineClip.freehandMaskPoints).
 */
@Composable
fun FreehandMaskPad(
    points: List<Float>,
    onUpdatePoints: (List<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var liveDraft by remember(points) { mutableStateOf(points) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Draw Custom Freehand Mask",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable {
                            val diamond = listOf(0f, -0.45f, 0.45f, 0f, 0f, 0.45f, -0.45f, 0f)
                            liveDraft = diamond
                            onUpdatePoints(diamond)
                        }
                        .testTag("freehand_mask_preset_diamond")
                ) {
                    Text("Diamond", style = MaterialTheme.typography.labelSmall, color = ProGold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable {
                            val hex = listOf(-0.24f, -0.42f, 0.24f, -0.42f, 0.46f, 0f, 0.24f, 0.42f, -0.24f, 0.42f, -0.46f, 0f)
                            liveDraft = hex
                            onUpdatePoints(hex)
                        }
                        .testTag("freehand_mask_preset_hex")
                ) {
                    Text("Hex", style = MaterialTheme.typography.labelSmall, color = AcidLime, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = CoralRed.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable {
                            liveDraft = emptyList()
                            onUpdatePoints(emptyList())
                        }
                        .testTag("freehand_mask_clear_button")
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = CoralRed, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledBlack)
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .testTag("freehand_mask_draw_pad")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val nx = (pos.x / size.width.coerceAtLeast(1) - 0.5f).coerceIn(-0.48f, 0.48f)
                                val ny = (pos.y / size.height.coerceAtLeast(1) - 0.5f).coerceIn(-0.48f, 0.48f)
                                liveDraft = listOf(nx, ny)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val nx = (change.position.x / size.width.coerceAtLeast(1) - 0.5f).coerceIn(-0.48f, 0.48f)
                                val ny = (change.position.y / size.height.coerceAtLeast(1) - 0.5f).coerceIn(-0.48f, 0.48f)
                                val lastX = liveDraft.getOrNull(liveDraft.size - 2) ?: 99f
                                val lastY = liveDraft.getOrNull(liveDraft.size - 1) ?: 99f
                                val dx = nx - lastX
                                val dy = ny - lastY
                                if (dx * dx + dy * dy > 0.002f && liveDraft.size < 96) {
                                    liveDraft = liveDraft + nx + ny
                                }
                            },
                            onDragEnd = {
                                if (liveDraft.size >= 6) onUpdatePoints(liveDraft)
                            }
                        )
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(Color.White.copy(alpha = 0.08f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)

                if (liveDraft.size >= 6) {
                    val path = Path().apply {
                        moveTo((liveDraft[0] + 0.5f) * size.width, (liveDraft[1] + 0.5f) * size.height)
                        var i = 2
                        while (i + 1 < liveDraft.size) {
                            lineTo((liveDraft[i] + 0.5f) * size.width, (liveDraft[i + 1] + 0.5f) * size.height)
                            i += 2
                        }
                        close()
                    }
                    drawPath(path, color = NeonCyan.copy(alpha = 0.22f))
                    drawPath(path, color = NeonCyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            if (liveDraft.size < 6) {
                Text(
                    text = "Trace shape with finger to cut custom mask",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun formatSignedPercent(v: Float): String {
    val pct = (v * 100f).roundToInt()
    return if (pct > 0) "+$pct%" else "$pct%"
}
