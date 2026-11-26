package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.model.ExportResolution
import com.example.network.NetworkStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStudioStrings
import com.example.ui.theme.AcidLime
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioThemeMode
import com.example.viewmodel.StudioCacheStats
import java.util.Locale

/**
 * Full-featured Settings Modal for NV Edit:
 * - Language Toggle (English / हिंदी)
 * - Cache & Storage Cleaner with freed-MB indicator
 * - Default Export Quality & Auto-Save toggle
 * - Studio Appearance & Low-Network Lite Mode
 * - Legal & Info (Privacy Policy, Terms of Use, Rate App, Version 1.0.0 by Neeraj Mahoba)
 */
@Composable
fun StudioSettingsSheetDialog(
    themeMode: StudioThemeMode,
    networkStatus: NetworkStatus,
    appLanguage: AppLanguage,
    defaultExportResolution: ExportResolution,
    autoSaveEnabled: Boolean,
    cacheStats: StudioCacheStats,
    onSelectTheme: (StudioThemeMode) -> Unit,
    onToggleLowDataMode: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectDefaultExportResolution: (ExportResolution) -> Unit,
    onToggleAutoSave: () -> Unit,
    onClearTempCache: () -> Unit,
    onOpenLegalTab: (LegalDialogTab) -> Unit,
    onOpenAiLab: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val strings = LocalStudioStrings.current
    val context = LocalContext.current

    fun launchRateApp() {
        val packageId = BuildConfig.APPLICATION_ID
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageId")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(marketIntent) }.isSuccess ||
            runCatching { context.startActivity(webIntent) }.isSuccess
        if (!opened) {
            Toast.makeText(context, strings.noRateStoreToast, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.glassSurface,
            border = BorderStroke(1.dp, palette.glassBorderBrush),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .testTag("studio_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NvEditBrandEmblem(size = 36.dp)
                        Column {
                            Text(
                                text = "${strings.appName} • ${strings.settingsTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.settingsSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close studio settings",
                            tint = palette.textSecondary
                        )
                    }
                }

                // 1. Multi-Language Toggle (English / हिंदी)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.languageSection,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val active = lang == appLanguage
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) NeonCyan.copy(alpha = 0.20f) else palette.surface,
                                    border = BorderStroke(
                                        width = if (active) 1.2.dp else 1.dp,
                                        color = if (active) NeonCyan else palette.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectLanguage(lang) }
                                        .testTag("settings_lang_${lang.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang.displayLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (active) NeonCyan else palette.textSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Cache & Storage Cleaner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CleaningServices,
                                    contentDescription = null,
                                    tint = AcidLime,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.storageCleanerTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = strings.storageCleanerSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surface,
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f MB", cacheStats.tempCacheMb),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("settings_cache_size_label")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onClearTempCache,
                                enabled = !cacheStats.isClearing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidLime,
                                    contentColor = OledBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_clear_cache_button")
                            ) {
                                Text(
                                    text = if (cacheStats.isClearing) strings.clearingCache else strings.clearTempCache,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            cacheStats.lastFreedMb?.let { freed ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AcidLime.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "${strings.freedCachePrefix} ${String.format(Locale.US, "%.1f", freed)} MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AcidLime,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(horizontal = 9.dp, vertical = 7.dp)
                                            .testTag("settings_freed_mb_badge")
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Default Export Quality & Auto-Save Toggle
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HighQuality,
                                contentDescription = null,
                                tint = ProGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.defaultExportQuality,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportResolution.entries.forEach { res ->
                                val active = res == defaultExportResolution
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) ProGold.copy(alpha = 0.22f) else palette.surface,
                                    border = BorderStroke(
                                        width = if (active) 1.2.dp else 1.dp,
                                        color = if (active) ProGold else palette.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectDefaultExportResolution(res) }
                                        .testTag("settings_export_quality_${res.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = res.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) ProGold else palette.textSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkAdded,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.autoSaveTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = strings.autoSaveSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = autoSaveEnabled,
                                onCheckedChange = { onToggleAutoSave() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OledBlack,
                                    checkedTrackColor = palette.accentSecondary
                                ),
                                modifier = Modifier.testTag("settings_auto_save_switch")
                            )
                        }
                    }
                }

                // 4. Studio Appearance Theme Switcher
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.studioAppearance,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioThemeMode.entries.forEach { mode ->
                                val active = mode == themeMode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) palette.accentPrimary.copy(alpha = 0.24f) else palette.surface,
                                    border = BorderStroke(
                                        width = if (active) 1.2.dp else 1.dp,
                                        color = if (active) palette.accentSecondary else palette.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectTheme(mode) }
                                        .testTag("settings_theme_${mode.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.shortLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) palette.accentSecondary else palette.textSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Low-Network Lite Mode Toggle
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentSecondary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DataSaverOn,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.lowNetworkMode,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${networkStatus.quality.label} • Fast Gemini Lite + Auto-Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = networkStatus.manualLowDataMode,
                            onCheckedChange = { onToggleLowDataMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = palette.accentSecondary
                            ),
                            modifier = Modifier.testTag("settings_low_data_switch")
                        )
                    }
                }

                // 6. Legal & Info Section (Privacy Policy, Terms of Use, Rate App, AI & Shots, Version 1.0.0 by Neeraj Mahoba)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.legalSectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        SettingsLinkRow(
                            icon = Icons.Filled.PrivacyTip,
                            iconTint = NeonCyan,
                            label = strings.privacyPolicy,
                            onClick = {
                                onDismiss()
                                onOpenLegalTab(LegalDialogTab.PRIVACY)
                            },
                            testTag = "settings_open_privacy_button"
                        )
                        SettingsLinkRow(
                            icon = Icons.Filled.Gavel,
                            iconTint = ProGold,
                            label = strings.termsOfUse,
                            onClick = {
                                onDismiss()
                                onOpenLegalTab(LegalDialogTab.TERMS)
                            },
                            testTag = "settings_open_terms_button"
                        )
                        SettingsLinkRow(
                            icon = Icons.Filled.StarRate,
                            iconTint = AcidLime,
                            label = strings.rateApp,
                            onClick = { launchRateApp() },
                            testTag = "settings_rate_app_button"
                        )
                        SettingsLinkRow(
                            icon = Icons.Filled.AutoAwesome,
                            iconTint = palette.accentPrimary,
                            label = strings.aiAndShots,
                            onClick = {
                                onDismiss()
                                onOpenAiLab()
                            },
                            testTag = "settings_open_ai_lab_button"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_version_info_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.versionByAuthor,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
