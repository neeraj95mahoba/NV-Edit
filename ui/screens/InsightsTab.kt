package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CreatorInsightsData
import com.example.model.StudioCatalog
import com.example.model.TrendingAudioSuggestion
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import kotlin.math.roundToInt

@Composable
fun InsightsTab(
    selectedPeriod: String,
    insights: CreatorInsightsData,
    projectCount: Int = 0,
    totalLibraryDurationMs: Long = 0L,
    onSelectPeriod: (String) -> Unit,
    onUseTrendingAudio: (TrendingAudioSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var inspectedPointIndex by remember(selectedPeriod) { mutableIntStateOf(3) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Period Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Exports & Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reels performance • ${insights.periodLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("7D", "30D", "90D").forEach { period ->
                        val active = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) palette.accentPrimary else Color.Transparent)
                                .clickable { onSelectPeriod(period) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("insights_period_$period")
                        ) {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Color.White else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Studio Export Pipeline Summary Strip
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.glassBorderBrush),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(ProGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HighQuality,
                                contentDescription = null,
                                tint = ProGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$projectCount Studio Projects • ${formatShortDuration(totalLibraryDurationMs)} Edited",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "4K 60fps MP4 hardware encoder • Zero watermark",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AcidLime.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "4K READY",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // KPI Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightKpiCard(
                    label = "Total Views",
                    value = insights.totalViews,
                    delta = insights.viewsDelta,
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                InsightKpiCard(
                    label = "Avg Watch",
                    value = insights.avgWatchTime,
                    delta = "${insights.completionRate} full",
                    accent = ElectricPurple,
                    modifier = Modifier.weight(1f)
                )
                InsightKpiCard(
                    label = "Net Growth",
                    value = insights.followersDelta,
                    delta = "Active",
                    accent = AcidLime,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Audience Retention Graph
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Audience Retention Curve",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap chart to inspect hook & drop-off second",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        val pct = (insights.retentionCurve.getOrElse(inspectedPointIndex) { 0.7f } * 100).roundToInt()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonCyan.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${inspectedPointIndex}s • $pct%",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    RetentionLineChart(
                        points = insights.retentionCurve,
                        selectedIndex = inspectedPointIndex,
                        onSelectIndex = { inspectedPointIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("retention_curve_chart")
                    )
                }
            }
        }

        // Best Posting Times
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = WarmAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Best Posting Windows",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        insights.bestPostingSlots.forEach { slot ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (slot.engagementScore > 0.94f) NeonCyan.copy(alpha = 0.6f) else StudioBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(slot.day, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(slot.peakHour, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(OledBlack)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(slot.engagementScore)
                                                .height(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan))
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trending Audio Suggestions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MagentaSpark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Trending Audio Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        items(insights.trendingAudios, key = { it.id }) { audio ->
            val accent = Color(audio.accentHex)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = accent
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(audio.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "${audio.artist} • ${audio.bpm} BPM • ${audio.growthLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricPurple.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onUseTrendingAudio(audio) }
                            .testTag("use_trending_audio_${audio.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Use audio in editor",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("Edit", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                        }
                    }
                }
            }
        }

        // Top Reels Performance
        item {
            Text(
                text = "Top Performing Reels",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(insights.topReels) { reel ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = StudioCatalog.drawableForKey(reel.drawableKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reel.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            text = "${reel.views} views • ${reel.shares} shares",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AcidLime.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${reel.retentionPct}% hold",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightKpiCard(
    label: String,
    value: String,
    delta: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(delta, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun RetentionLineChart(
    points: List<Float>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { offset ->
                if (points.size > 1) {
                    val idx = ((offset.x / size.width) * (points.size - 1)).roundToInt()
                        .coerceIn(0, points.lastIndex)
                    onSelectIndex(idx)
                }
            }
        }
    ) {
        if (points.size < 2) return@Canvas
        val padY = 14.dp.toPx()
        val chartH = size.height - padY * 2

        // Horizontal grid lines
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
            val y = padY + (1f - level) * chartH
            drawLine(
                color = StudioBorder,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val coords = points.mapIndexed { idx, v ->
            val x = (idx.toFloat() / points.lastIndex.toFloat()) * size.width
            val y = padY + (1f - v.coerceIn(0f, 1f)) * chartH
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ElectricPurple.copy(alpha = 0.36f), NeonCyan.copy(alpha = 0.04f))
            )
        )

        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Highlighted inspected point
        val active = coords.getOrElse(selectedIndex) { coords.first() }
        drawLine(
            color = NeonCyan.copy(alpha = 0.45f),
            start = Offset(active.x, padY),
            end = Offset(active.x, size.height),
            strokeWidth = 1.5.dp.toPx()
        )
        drawCircle(color = OledBlack, radius = 7.dp.toPx(), center = active)
        drawCircle(color = NeonCyan, radius = 5.dp.toPx(), center = active)
    }
}
