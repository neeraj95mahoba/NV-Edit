package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProGold

/**
 * Sleek 1.5-second animated launch splash overlay featuring a glowing
 * neon-cyan & gold "NV Edit" cinema lens logo and "Pro AI Video & Photo Studio" tagline.
 */
@Composable
fun NvEditSplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var animStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animStarted = true }

    val logoScale by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splashContentAlpha"
    )
    val progressFill by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0.08f,
        animationSpec = tween(durationMillis = 1380, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    val infinite = rememberInfiniteTransition(label = "splashLensSpin")
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(320)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07080D))
                .testTag("nv_edit_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Neon-Cyan & Gold Cinema Studio Glow Backdrop
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.48f, size.height * 0.42f),
                        radius = size.minDimension * 0.62f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ProGold.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.56f, size.height * 0.50f),
                        radius = size.minDimension * 0.48f
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .graphicsLayer { alpha = contentAlpha }
            ) {
                // Glowing Neon-Cyan & Gold Cinema Lens Emblem
                Canvas(
                    modifier = Modifier
                        .size(134.dp)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val outerR = w * 0.43f
                    val innerR = w * 0.34f
                    val coreR = w * 0.28f

                    // Outer soft halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.34f),
                                ProGold.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = w * 0.50f
                        ),
                        radius = w * 0.50f,
                        center = center
                    )

                    // Outer neon-cyan cinema lens barrel
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(NeonCyan, ProGold, NeonCyan)),
                        radius = outerR,
                        center = center,
                        style = Stroke(width = w * 0.045f)
                    )

                    // Rotating gold-cyan aperture arcs
                    rotate(degrees = ringRotation, pivot = center) {
                        drawArc(
                            color = ProGold,
                            startAngle = -35f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(center.x - innerR, center.y - innerR),
                            size = Size(innerR * 2f, innerR * 2f),
                            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = NeonCyan,
                            startAngle = 145f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(center.x - innerR, center.y - innerR),
                            size = Size(innerR * 2f, innerR * 2f),
                            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
                        )
                    }

                    // Deep obsidian optical lens glass core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF17223B), Color(0xFF0A0D16)),
                            center = Offset(w * 0.42f, h * 0.40f),
                            radius = coreR * 1.4f
                        ),
                        radius = coreR,
                        center = center
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.55f),
                        radius = coreR,
                        center = center,
                        style = Stroke(width = w * 0.015f)
                    )

                    // Stylized NV Prism Monogram
                    val nvPath = Path().apply {
                        moveTo(w * 0.34f, h * 0.61f)
                        lineTo(w * 0.34f, h * 0.39f)
                        lineTo(w * 0.50f, h * 0.60f)
                        lineTo(w * 0.66f, h * 0.39f)
                    }
                    drawPath(
                        path = nvPath,
                        brush = Brush.linearGradient(listOf(NeonCyan, Color.White, ProGold)),
                        style = Stroke(
                            width = w * 0.068f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Gold cinema record / spark dot
                    drawCircle(
                        color = ProGold,
                        radius = w * 0.042f,
                        center = Offset(w * 0.66f, h * 0.59f)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "NV Edit",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Pro AI Video & Photo Studio",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = ProGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ProGold.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "4K–32K • DSLR • ZERO WATERMARK",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom subtle loading progress bar + creator signature
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(156.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .width((156f * progressFill).dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(NeonCyan, ProGold)))
                    )
                }
                Text(
                    text = "v1.0.0 • by Neeraj Mahoba",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
