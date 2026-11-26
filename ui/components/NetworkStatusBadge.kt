package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.network.NetworkQuality
import com.example.network.NetworkStatus
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProGold
import com.example.ui.theme.WarmAmber

@Composable
fun NetworkStatusPill(
    networkStatus: NetworkStatus,
    onToggleLowDataMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val (accent, icon, label) = when {
        networkStatus.isOffline -> Triple(WarmAmber, Icons.Filled.WifiOff, "Offline")
        networkStatus.useLiteOptimization -> Triple(ProGold, Icons.Filled.Bolt, "Lite Net")
        else -> Triple(AcidLime, Icons.Filled.SignalCellularAlt, "Fast")
    }

    Surface(
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onToggleLowDataMode)
            .testTag("network_lite_mode_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.cd_network_mode),
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NetworkResilienceBanner(
    networkStatus: NetworkStatus,
    onToggleLowDataMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val (accent, icon, message) = when {
        networkStatus.isOffline -> Triple(
            WarmAmber,
            Icons.Filled.WifiOff,
            "Offline Mode • Editing, Photo Animate, Paste Lyrics & 4K Export work 100% offline"
        )
        networkStatus.quality == NetworkQuality.LOW_NETWORK -> Triple(
            ProGold,
            Icons.Filled.SignalCellularAlt2Bar,
            "Weak Network Detected • Auto Gemini Lite + Smart Retry enabled"
        )
        networkStatus.manualLowDataMode -> Triple(
            NeonCyan,
            Icons.Filled.Bolt,
            "Low-Data Lite Mode ON • Fast Gemini Lite + Compact AI Caching"
        )
        else -> Triple(
            AcidLime,
            Icons.Filled.SignalCellularAlt,
            "Online • Toggle Lite Mode anytime for low-signal / 2G-3G areas"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!networkStatus.isOffline) {
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = networkStatus.manualLowDataMode,
                    onCheckedChange = { onToggleLowDataMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.background,
                        checkedTrackColor = ProGold,
                        uncheckedTrackColor = palette.surfaceElevated
                    ),
                    modifier = Modifier.testTag("banner_low_data_switch")
                )
            }
        }
    }
}
