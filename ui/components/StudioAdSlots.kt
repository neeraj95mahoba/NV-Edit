package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.FullscreenAdPreviewRequest
import com.example.ads.FullscreenPreviewAdType
import com.example.ads.LocalStudioAdController
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

/**
 * 1. TOP STICKY BANNER AD SLOT (320x50)
 * Truecaller-inspired sleek dark top banner container with a crisp "Ad" badge and subtle 1dp dark border.
 * Pinned strictly to the top of Home & Templates screens — never blocks editing tools.
 */
@Composable
fun TopStickyBannerAdSlot(
    placementTag: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val adController = LocalStudioAdController.current
    val adState by adController.state.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current
    val bannerCd = stringResource(R.string.cd_top_banner_ad)

    if (!adState.bannerVisible) return

    var nativeBannerLoaded by remember(placementTag, adState.bannerRefreshEpoch) {
        mutableStateOf(false)
    }

    Surface(
        color = if (palette.isLight) palette.surface else Color(0xFF0C0D12),
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = bannerCd }
            .testTag("top_sticky_banner_ad_$placementTag")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (palette.isLight) palette.surfaceElevated else Color(0xFF13141C))
                    .border(
                        width = 1.dp,
                        brush = palette.glassBorderBrush,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Sleek Dark Preview Card shown while AdMob loads or when running offline/unconfigured
                if (!nativeBannerLoaded) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdDisclosureBadge()

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ad_top_banner_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.ad_top_banner_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.accentSecondary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "320×50",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Real Native Google Mobile Ads Banner View (320x50)
                // Live AdMob banners render their own built-in AdChoices / Ad badge; never overlay custom views on top of AdView.
                if (!isPreview) {
                    NativeAdMobBannerHost(
                        adUnitId = adState.config.resolvedBannerUnitId,
                        adSize = AdSize.BANNER,
                        refreshKey = "${placementTag}_${adState.bannerRefreshEpoch}",
                        onLoaded = { nativeBannerLoaded = true },
                        onFailed = { nativeBannerLoaded = false },
                        modifier = Modifier
                            .size(width = 320.dp, height = 50.dp)
                            .alpha(if (nativeBannerLoaded) 1f else 0f)
                    )
                }
            }
        }
    }
}

/**
 * 2. NATIVE / MEDIUM RECTANGLE AD CARD (300x250)
 * Placed inside the Templates feed (after every 4 templates) and on the Export Complete / Share screen.
 */
@Composable
fun NativeMediumRectangleAdCard(
    placementTag: String,
    compactForDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val adController = LocalStudioAdController.current
    val adState by adController.state.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current
    val nativeCd = stringResource(R.string.cd_native_ad_card)

    if (!adState.nativeVisible) return

    var nativeMrecLoaded by remember(placementTag, adState.nativeRefreshEpoch) {
        mutableStateOf(false)
    }

    val cardShape = RoundedCornerShape(if (compactForDialog) 16.dp else 20.dp)

    Surface(
        shape = cardShape,
        color = if (palette.isLight) palette.surface else Color(0xFF13141A),
        border = BorderStroke(1.dp, palette.glassBorderBrush),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = nativeCd }
            .testTag("native_mrec_ad_card_$placementTag")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with "Ad" disclosure pill + Mediation Network indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdDisclosureBadge()
                    Text(
                        text = stringResource(R.string.ad_sponsored_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = adState.config.activeMediationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    fontSize = 10.sp
                )
            }

            // 300x250 Medium Rectangle Native Ad Canvas
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (palette.isLight) {
                                listOf(Color(0xFFF3F6FF), Color(0xFFEAEEF9))
                            } else {
                                listOf(Color(0xFF161824), Color(0xFF0E1018), Color(0xFF131926))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = palette.borderSubtle,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Clean Dark Native Card Preview when loading / offline / test preview
                if (!nativeMrecLoaded) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                ElectricPurple.copy(alpha = 0.25f),
                                                NeonCyan.copy(alpha = 0.22f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        NeonCyan.copy(alpha = 0.35f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text(
                                    text = "300×250 Native",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.ad_native_card_headline),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.ad_native_card_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = AcidLime,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (adState.config.hasLiveProductionIds) "Live Native Slot" else "Ready for AdMob / Meta",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.accentSecondary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = stringResource(R.string.ad_native_card_cta),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Real Native Google Mobile Ads Medium Rectangle (300x250) View
                if (!isPreview) {
                    NativeAdMobBannerHost(
                        adUnitId = adState.config.resolvedMediumRectangleUnitId,
                        adSize = AdSize.MEDIUM_RECTANGLE,
                        refreshKey = "${placementTag}_${adState.nativeRefreshEpoch}",
                        onLoaded = { nativeMrecLoaded = true },
                        onFailed = { nativeMrecLoaded = false },
                        modifier = Modifier
                            .size(width = 300.dp, height = 250.dp)
                            .alpha(if (nativeMrecLoaded) 1f else 0f)
                    )
                }
            }
        }
    }
}

/**
 * Sleek 5-Second Rewarded Video Ad Player Modal & Post-Export Interstitial Preview Dialog.
 * - For Rewarded Ads (8K = 1 Ad, 16K = 2 Ads, 32K = 3 Ads): displays a video-player style modal
 *   with a live 5-second countdown timer, sequential step pills ("Ad 1 of 2"), and a glowing
 *   "Reward Granted" confirmation that smoothly advances to the next ad or starts rendering.
 * - For Post-Export Interstitials (720p–4K): displays a quick 2-second dismissible preview when offline.
 */
@Composable
fun StudioFullscreenAdPreviewDialog(
    request: FullscreenAdPreviewRequest?,
    onComplete: (rewardGranted: Boolean) -> Unit
) {
    if (request == null) return

    val palette = LocalStudioPalette.current
    val isRewarded = request.type == FullscreenPreviewAdType.REWARDED
    val totalSeconds = request.durationSeconds.coerceAtLeast(1)
    var secondsLeft by remember(request) { mutableIntStateOf(totalSeconds) }
    var rewardGrantedShown by remember(request) { mutableStateOf(false) }
    var completionDispatched by remember(request) { mutableStateOf(false) }

    fun dispatchComplete(granted: Boolean) {
        if (!completionDispatched) {
            completionDispatched = true
            onComplete(granted)
        }
    }

    LaunchedEffect(request) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        if (isRewarded) {
            rewardGrantedShown = true
            // Let the creator clearly see the "Reward Granted" confirmation before auto-advancing
            delay(950L)
            dispatchComplete(true)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isRewarded && secondsLeft == 0) {
                dispatchComplete(false)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isRewarded && secondsLeft == 0,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E1018),
            border = BorderStroke(
                width = 1.dp,
                brush = if (rewardGrantedShown) {
                    Brush.horizontalGradient(listOf(AcidLime.copy(alpha = 0.75f), NeonCyan.copy(alpha = 0.75f)))
                } else {
                    palette.glassBorderBrush
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fullscreen_ad_preview_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row: Ad badge, Ad X of Y badge, and Countdown / Close control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdDisclosureBadge()
                        Text(
                            text = if (isRewarded) {
                                "Playing Ad ${request.adNumber} of ${request.totalAds}…"
                            } else {
                                request.contextTitle ?: "Sponsored Interstitial"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRewarded && request.totalAds > 1) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = ElectricPurple.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "Ad ${request.adNumber} of ${request.totalAds}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (secondsLeft > 0) {
                            Surface(
                                shape = CircleShape,
                                color = palette.surfaceElevated,
                                border = BorderStroke(1.dp, ProGold.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "${secondsLeft}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { dispatchComplete(isRewarded) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("close_fullscreen_ad_preview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_close_ad_preview),
                                    tint = palette.textPrimary
                                )
                            }
                        }
                    }
                }

                // Multi-Ad Step Dots (for 2-Ad and 3-Ad chains)
                if (isRewarded && request.totalAds > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (step in 1..request.totalAds) {
                            val isDone = step < request.adNumber || (step == request.adNumber && rewardGrantedShown)
                            val isCurrent = step == request.adNumber && !rewardGrantedShown
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> AcidLime
                                            isCurrent -> ProGold
                                            else -> Color.White.copy(alpha = 0.14f)
                                        }
                                    )
                            )
                        }
                    }
                }

                // Sleek Video Ad Player Viewport Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF171929),
                                    Color(0xFF0E1220),
                                    Color(0xFF141D2B)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (rewardGrantedShown) AcidLime.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (rewardGrantedShown) {
                                        Brush.linearGradient(listOf(AcidLime, NeonCyan))
                                    } else {
                                        NeonStudioGradient
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    rewardGrantedShown -> Icons.Filled.Verified
                                    isRewarded -> Icons.Filled.PlayCircleFilled
                                    else -> Icons.Filled.WorkspacePremium
                                },
                                contentDescription = null,
                                tint = OledBlack,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        val targetName = request.resolutionLabel ?: request.contextTitle
                        if (rewardGrantedShown) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = AcidLime.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("reward_granted_confirmation_badge")
                            ) {
                                Text(
                                    text = "${stringResource(R.string.ad_reward_granted_label)} ✓",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AcidLime,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = when {
                                    isRewarded && targetName != null ->
                                        "Unlocking $targetName"
                                    isRewarded ->
                                        "Supporting Free NV Edit Studio"
                                    else ->
                                        request.contextTitle ?: "Sponsored Partner Ad"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = when {
                                rewardGrantedShown && request.adNumber < request.totalAds ->
                                    "Ad ${request.adNumber} of ${request.totalAds} completed — loading Ad ${request.adNumber + 1} of ${request.totalAds}…"
                                rewardGrantedShown && targetName != null ->
                                    "All ${request.totalAds} ad${if (request.totalAds > 1) "s" else ""} complete — starting $targetName…"
                                isRewarded ->
                                    "Playing Ad ${request.adNumber} of ${request.totalAds} • ${secondsLeft}s left • ${request.mediationLabel}"
                                else ->
                                    "${request.mediationLabel} • Connected via adConfig.js"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // Live Playback Countdown Progress Bar
                LinearProgressIndicator(
                    progress = {
                        ((totalSeconds - secondsLeft).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                    },
                    color = when {
                        rewardGrantedShown -> AcidLime
                        isRewarded -> ProGold
                        else -> NeonCyan
                    },
                    trackColor = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )

                // Footer Action: Instant continue once unlocked, or Cancel while counting down
                if (secondsLeft == 0) {
                    val targetName = request.resolutionLabel ?: request.contextTitle
                    Button(
                        onClick = { dispatchComplete(isRewarded) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rewardGrantedShown) AcidLime else NeonCyan,
                            contentColor = OledBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_fullscreen_ad_preview_button")
                    ) {
                        Text(
                            text = when {
                                isRewarded && request.adNumber < request.totalAds ->
                                    "Continue to Ad ${request.adNumber + 1} of ${request.totalAds}"
                                isRewarded && targetName != null ->
                                    "Reward Granted • Start $targetName"
                                isRewarded ->
                                    "Claim Reward & Continue"
                                else ->
                                    "Continue to Studio"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isRewarded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unit: ${request.unitId.takeLast(14)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = stringResource(R.string.ad_rewarded_cancel_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dispatchComplete(false) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("cancel_rewarded_ad_preview_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdDisclosureBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = ProGold.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ProGold.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.ad_badge_label),
            style = MaterialTheme.typography.labelSmall,
            color = ProGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun NativeAdMobBannerHost(
    adUnitId: String,
    adSize: AdSize,
    refreshKey: String,
    onLoaded: () -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adView = remember(adUnitId, adSize, refreshKey) {
        runCatching {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        onLoaded()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        onFailed()
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }.getOrNull()
    }

    DisposableEffect(adView) {
        onDispose {
            runCatching { adView?.destroy() }
        }
    }

    if (adView != null) {
        AndroidView(
            factory = { adView },
            modifier = modifier
        )
    }
}
