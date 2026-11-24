package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.example.model.SpeedCurvePoint
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun SpeedCurveGraph(
    points: List<SpeedCurvePoint>,
    onUpdatePoint: (index: Int, newSpeedMultiplier: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDragIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OledBlack)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .testTag("speed_curve_graph")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            var bestIdx = -1
                            var bestDist = Float.MAX_VALUE
                            points.forEachIndexed { idx, pt ->
                                val px = pt.normalizedTime * size.width
                                val py = speedToY(pt.speedMultiplier, size.height.toFloat())
                                val dist = (Offset(px, py) - startOffset).getDistance()
                                if (dist < bestDist) {
                                    bestDist = dist
                                    bestIdx = idx
                                }
                            }
                            activeDragIndex = if (bestDist < 90f) bestIdx else -1
                        },
                        onDragEnd = { activeDragIndex = -1 },
                        onDragCancel = { activeDragIndex = -1 },
                        onDrag = { change, _ ->
                            if (activeDragIndex in points.indices) {
                                change.consume()
                                val speed = yToSpeed(change.position.y, size.height.toFloat())
                                onUpdatePoint(activeDragIndex, speed)
                            }
                        }
                    )
                }
        ) {
            if (points.size < 2) return@Canvas

            // 1.0x reference baseline (dashed)
            val baseY = speedToY(1.0f, size.height)
            drawLine(
                color = TextMuted.copy(alpha = 0.55f),
                start = Offset(0f, baseY),
                end = Offset(size.width, baseY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Smooth Bezier Velocity Curve
            val coords = points.map { pt ->
                Offset(
                    x = pt.normalizedTime.coerceIn(0f, 1f) * size.width,
                    y = speedToY(pt.speedMultiplier, size.height)
                )
            }
            val curvePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            val fillPath = Path().apply {
                addPath(curvePath)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(ElectricPurple.copy(alpha = 0.34f), NeonCyan.copy(alpha = 0.04f))
                )
            )
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draggable Control Nodes
            coords.forEachIndexed { idx, offset ->
                val active = idx == activeDragIndex
                drawCircle(
                    color = if (active) NeonCyan.copy(alpha = 0.3f) else Color.Transparent,
                    radius = 13.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = OledBlack,
                    radius = 7.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = if (active) Color.White else NeonCyan,
                    radius = 5.dp.toPx(),
                    center = offset
                )
            }
        }

        // Scale markers
        Text(
            text = "10x",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 4.dp)
        )
        Text(
            text = "1.0x",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )
        Text(
            text = "0.1x",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 4.dp)
        )

        if (activeDragIndex in points.indices) {
            val spd = points[activeDragIndex].speedMultiplier
            Text(
                text = String.format(Locale.US, "%.2fx", spd),
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 6.dp)
            )
        }
    }
}

// Logarithmic mapping so 0.1x..1.0x..10x feels evenly balanced on the graph
private fun speedToY(speed: Float, height: Float): Float {
    val clamped = speed.coerceIn(0.1f, 10f)
    val norm = ((ln(clamped.toDouble()) - ln(0.1)) / (ln(10.0) - ln(0.1))).toFloat().coerceIn(0f, 1f)
    return (1f - norm) * height
}

private fun yToSpeed(y: Float, height: Float): Float {
    val norm = (1f - (y / height.coerceAtLeast(1f))).coerceIn(0f, 1f)
    val speed = 0.1f * (100f.pow(norm))
    return if (abs(speed - 1f) < 0.08f) 1.0f else speed.coerceIn(0.1f, 10f)
}
