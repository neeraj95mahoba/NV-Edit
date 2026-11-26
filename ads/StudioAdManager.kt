package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

enum class FullscreenPreviewAdType {
    INTERSTITIAL,
    REWARDED
}

data class FullscreenAdPreviewRequest(
    val type: FullscreenPreviewAdType,
    val unitId: String,
    val mediationLabel: String,
    val adNumber: Int = 1,
    val totalAds: Int = 1,
    val resolutionLabel: String? = null,
    val contextTitle: String? = null,
    val durationSeconds: Int = if (type == FullscreenPreviewAdType.REWARDED) 5 else 2
)

data class RewardedSequenceProgress(
    val isRunning: Boolean = false,
    val resolutionLabel: String = "",
    val currentAdNumber: Int = 0,
    val completedAds: Int = 0,
    val totalAds: Int = 0,
    val statusMessage: String = ""
)

data class StudioAdUiState(
    val config: StudioAdConfig = StudioAdConfig(),
    val isSdkInitialized: Boolean = false,
    val bannerVisible: Boolean = true,
    val nativeVisible: Boolean = true,
    val bannerRefreshEpoch: Int = 0,
    val nativeRefreshEpoch: Int = 0,
    val isInterstitialReady: Boolean = false,
    val isRewardedReady: Boolean = false,
    val rewardsEarnedCount: Int = 0,
    val hasShownImportInterstitialThisSession: Boolean = false,
    val activePreviewAd: FullscreenAdPreviewRequest? = null,
    val rewardedSequence: RewardedSequenceProgress = RewardedSequenceProgress(),
    val lastAdStatus: String = "AdMob & Meta Mediation Ready"
)

interface StudioAdController {
    val state: StateFlow<StudioAdUiState>
    val config: StudioAdConfig

    fun showBannerAd()
    fun showNativeAd()
    fun showInterstitialAd(
        contextTitle: String = "Post-Export Interstitial",
        onDismissed: () -> Unit = {}
    )
    fun showMediaImportInterstitialOncePerSession(
        sourceLabel: String = "Media Import",
        onCompleted: () -> Unit = {}
    )
    fun showRewardedAd(
        adNumber: Int = 1,
        totalAds: Int = 1,
        resolutionLabel: String? = null,
        onRewardEarned: (amount: Int, type: String) -> Unit = { _, _ -> },
        onDismissed: (rewardEarned: Boolean) -> Unit = {}
    )
    fun showRewardedAdSequence(
        actionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit = {}
    )
    fun showResolutionRewardedSequence(
        resolutionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit = {}
    )
    fun cancelRewardedSequence()
    fun completeFullscreenPreviewAd(rewardGranted: Boolean)
}

val LocalStudioAdController = staticCompositionLocalOf<StudioAdController> {
    NoOpStudioAdController
}

private object NoOpStudioAdController : StudioAdController {
    private val defaultState = MutableStateFlow(StudioAdUiState())
    override val state: StateFlow<StudioAdUiState> = defaultState.asStateFlow()
    override val config: StudioAdConfig = StudioAdConfig()
    override fun showBannerAd() {}
    override fun showNativeAd() {}
    override fun showInterstitialAd(contextTitle: String, onDismissed: () -> Unit) {
        onDismissed()
    }
    override fun showMediaImportInterstitialOncePerSession(sourceLabel: String, onCompleted: () -> Unit) {
        onCompleted()
    }
    override fun showRewardedAd(
        adNumber: Int,
        totalAds: Int,
        resolutionLabel: String?,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onDismissed: (rewardEarned: Boolean) -> Unit
    ) {
        onDismissed(false)
    }
    override fun showRewardedAdSequence(
        actionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit
    ) {
        onAllAdsCompleted()
    }
    override fun showResolutionRewardedSequence(
        resolutionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit
    ) {
        onAllAdsCompleted()
    }
    override fun cancelRewardedSequence() {}
    override fun completeFullscreenPreviewAd(rewardGranted: Boolean) {}
}

/**
 * Native Android Ad Monetization Manager supporting Google AdMob & Meta Audience Network Mediation.
 * - Supports sequential Rewarded Ads for Ultra-HD Exports (8K=1, 16K=2, 32K=3) AND AI Studio Tools
 *   (Text to Video=3, Text to Photo=2, AI Auto-Captions=2, AI Background Remover=2) via
 *   `window.AndroidAds.showRewardedAd(adNumber, totalAds)`.
 * - Supports once-per-session Interstitial Ads on Media Import / AI Photo-to-Video.
 * - Uses live Google Mobile Ads SDK when configured for production, and shows a sleek 5-second
 *   Rewarded Ad player modal (with live countdown & "Reward Granted" confirmation) in test/preview mode.
 */
class StudioAdManager(
    activity: Activity
) : StudioAdController {

    private val appContext: Context = activity.applicationContext
    private val activityRef = WeakReference(activity)
    private val mainHandler = Handler(Looper.getMainLooper())

    override val config: StudioAdConfig = StudioAdConfig.loadFromAssets(appContext)

    private val _state = MutableStateFlow(StudioAdUiState(config = config))
    override val state: StateFlow<StudioAdUiState> = _state.asStateFlow()

    private var loadedInterstitial: InterstitialAd? = null
    private var loadedRewarded: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var importInterstitialShown = false

    private var pendingPreviewDismissCallback: ((Boolean) -> Unit)? = null
    private var pendingPreviewRewardCallback: ((Int, String) -> Unit)? = null
    private var activeSequenceToken: Long = 0L

    fun initialize() {
        runCatching {
            MobileAds.initialize(appContext) { initStatus ->
                val adapters = initStatus.adapterStatusMap.keys.joinToString(", ")
                Log.d(TAG, "MobileAds initialized. Adapters: $adapters")
                _state.update {
                    it.copy(
                        isSdkInitialized = true,
                        lastAdStatus = "${config.activeMediationLabel} Initialized"
                    )
                }
                preloadInterstitialAd()
                preloadRewardedAd()
            }
        }.onFailure { err ->
            Log.w(TAG, "MobileAds initialization fallback: ${err.message}")
            _state.update {
                it.copy(
                    isSdkInitialized = false,
                    lastAdStatus = "Ad Preview Mode (${err.localizedMessage ?: "Offline"})"
                )
            }
        }
    }

    override fun showBannerAd() {
        mainHandler.post {
            _state.update {
                it.copy(
                    bannerVisible = true,
                    bannerRefreshEpoch = it.bannerRefreshEpoch + 1,
                    lastAdStatus = "Top Banner (320×50) Active"
                )
            }
        }
    }

    override fun showNativeAd() {
        mainHandler.post {
            _state.update {
                it.copy(
                    nativeVisible = true,
                    nativeRefreshEpoch = it.nativeRefreshEpoch + 1,
                    lastAdStatus = "Native Card (300×250) Active"
                )
            }
        }
    }

    override fun showInterstitialAd(
        contextTitle: String,
        onDismissed: () -> Unit
    ) {
        mainHandler.post {
            val hostActivity = activityRef.get()
            val ad = loadedInterstitial
            if (hostActivity != null && !hostActivity.isFinishing && ad != null && !config.testMode) {
                loadedInterstitial = null
                _state.update { it.copy(isInterstitialReady = false) }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        preloadInterstitialAd()
                        onDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                        preloadInterstitialAd()
                        onDismissed()
                    }
                }
                ad.show(hostActivity)
            } else {
                pendingPreviewDismissCallback = { onDismissed() }
                pendingPreviewRewardCallback = null
                _state.update {
                    it.copy(
                        activePreviewAd = FullscreenAdPreviewRequest(
                            type = FullscreenPreviewAdType.INTERSTITIAL,
                            unitId = config.resolvedInterstitialUnitId,
                            mediationLabel = config.activeMediationLabel,
                            contextTitle = contextTitle,
                            durationSeconds = 2
                        ),
                        lastAdStatus = "$contextTitle Shown"
                    )
                }
                preloadInterstitialAd()
            }
        }
    }

    override fun showMediaImportInterstitialOncePerSession(
        sourceLabel: String,
        onCompleted: () -> Unit
    ) {
        mainHandler.post {
            if (!config.interstitialOnMediaImportOncePerSession || importInterstitialShown) {
                onCompleted()
                return@post
            }
            importInterstitialShown = true
            _state.update { it.copy(hasShownImportInterstitialThisSession = true) }
            showInterstitialAd(
                contextTitle = "$sourceLabel Interstitial",
                onDismissed = onCompleted
            )
        }
    }

    override fun showRewardedAd(
        adNumber: Int,
        totalAds: Int,
        resolutionLabel: String?,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onDismissed: (rewardEarned: Boolean) -> Unit
    ) {
        val safeTotal = totalAds.coerceAtLeast(1)
        val safeNumber = adNumber.coerceIn(1, safeTotal)

        mainHandler.post {
            val hostActivity = activityRef.get()
            val ad = loadedRewarded
            if (hostActivity != null && !hostActivity.isFinishing && ad != null && !config.testMode) {
                loadedRewarded = null
                _state.update { it.copy(isRewardedReady = false) }
                var earnedThisAd = false
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        preloadRewardedAd()
                        onDismissed(earnedThisAd)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                        preloadRewardedAd()
                        onDismissed(false)
                    }
                }
                ad.show(hostActivity) { rewardItem ->
                    earnedThisAd = true
                    _state.update { it.copy(rewardsEarnedCount = it.rewardsEarnedCount + 1) }
                    onRewardEarned(rewardItem.amount, rewardItem.type)
                }
            } else {
                pendingPreviewRewardCallback = onRewardEarned
                pendingPreviewDismissCallback = onDismissed
                _state.update {
                    it.copy(
                        activePreviewAd = FullscreenAdPreviewRequest(
                            type = FullscreenPreviewAdType.REWARDED,
                            unitId = config.resolvedRewardedUnitId,
                            mediationLabel = config.activeMediationLabel,
                            adNumber = safeNumber,
                            totalAds = safeTotal,
                            resolutionLabel = resolutionLabel,
                            contextTitle = resolutionLabel,
                            durationSeconds = 5
                        ),
                        lastAdStatus = "Playing Ad $safeNumber of $safeTotal…"
                    )
                }
                preloadRewardedAd()
            }
        }
    }

    override fun showRewardedAdSequence(
        actionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit
    ) {
        if (totalAds <= 0) {
            onAllAdsCompleted()
            return
        }

        mainHandler.post {
            val sequenceToken = System.nanoTime()
            activeSequenceToken = sequenceToken

            fun launchStep(stepNumber: Int) {
                if (activeSequenceToken != sequenceToken) return

                _state.update {
                    it.copy(
                        rewardedSequence = RewardedSequenceProgress(
                            isRunning = true,
                            resolutionLabel = actionLabel,
                            currentAdNumber = stepNumber,
                            completedAds = stepNumber - 1,
                            totalAds = totalAds,
                            statusMessage = "Playing Ad $stepNumber of $totalAds for $actionLabel…"
                        )
                    )
                }

                showRewardedAd(
                    adNumber = stepNumber,
                    totalAds = totalAds,
                    resolutionLabel = actionLabel,
                    onRewardEarned = { _, _ -> },
                    onDismissed = { rewardEarned ->
                        if (activeSequenceToken != sequenceToken) return@showRewardedAd
                        if (!rewardEarned) {
                            _state.update {
                                it.copy(
                                    rewardedSequence = RewardedSequenceProgress(),
                                    lastAdStatus = "$actionLabel ad sequence cancelled"
                                )
                            }
                            onCancelled()
                            return@showRewardedAd
                        }

                        if (stepNumber < totalAds) {
                            val nextStep = stepNumber + 1
                            _state.update {
                                it.copy(
                                    rewardedSequence = RewardedSequenceProgress(
                                        isRunning = true,
                                        resolutionLabel = actionLabel,
                                        currentAdNumber = nextStep,
                                        completedAds = stepNumber,
                                        totalAds = totalAds,
                                        statusMessage = "Ad $stepNumber of $totalAds completed, loading Ad $nextStep of $totalAds…"
                                    ),
                                    lastAdStatus = "Ad $stepNumber of $totalAds completed, loading Ad $nextStep of $totalAds…"
                                )
                            }
                            preloadRewardedAd()
                            mainHandler.postDelayed({
                                launchStep(nextStep)
                            }, 800L)
                        } else {
                            _state.update {
                                it.copy(
                                    rewardedSequence = RewardedSequenceProgress(
                                        isRunning = false,
                                        resolutionLabel = actionLabel,
                                        currentAdNumber = totalAds,
                                        completedAds = totalAds,
                                        totalAds = totalAds,
                                        statusMessage = "Reward Granted! Starting $actionLabel…"
                                    ),
                                    lastAdStatus = "Unlocked $actionLabel ($totalAds/$totalAds ads)"
                                )
                            }
                            onAllAdsCompleted()
                        }
                    }
                )
            }

            launchStep(1)
        }
    }

    override fun showResolutionRewardedSequence(
        resolutionLabel: String,
        totalAds: Int,
        onAllAdsCompleted: () -> Unit,
        onCancelled: () -> Unit
    ) {
        showRewardedAdSequence(
            actionLabel = "$resolutionLabel Export",
            totalAds = totalAds,
            onAllAdsCompleted = onAllAdsCompleted,
            onCancelled = onCancelled
        )
    }

    override fun cancelRewardedSequence() {
        mainHandler.post {
            activeSequenceToken = 0L
            pendingPreviewRewardCallback = null
            pendingPreviewDismissCallback = null
            _state.update {
                it.copy(
                    activePreviewAd = null,
                    rewardedSequence = RewardedSequenceProgress()
                )
            }
        }
    }

    override fun completeFullscreenPreviewAd(rewardGranted: Boolean) {
        val rewardCb = pendingPreviewRewardCallback
        val dismissCb = pendingPreviewDismissCallback
        pendingPreviewRewardCallback = null
        pendingPreviewDismissCallback = null
        _state.update {
            it.copy(
                activePreviewAd = null,
                rewardsEarnedCount = if (rewardGranted) it.rewardsEarnedCount + 1 else it.rewardsEarnedCount
            )
        }
        if (rewardGranted) {
            rewardCb?.invoke(1, "studio_unlock")
        }
        dismissCb?.invoke(rewardGranted)
    }

    fun preloadInterstitialAd() {
        if (isLoadingInterstitial || loadedInterstitial != null) return
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        runCatching {
            InterstitialAd.load(
                appContext,
                config.resolvedInterstitialUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        isLoadingInterstitial = false
                        loadedInterstitial = interstitialAd
                        _state.update {
                            it.copy(
                                isInterstitialReady = true,
                                lastAdStatus = "Interstitial Ad Ready"
                            )
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingInterstitial = false
                        loadedInterstitial = null
                        Log.d(TAG, "Interstitial preload skipped/failed: ${loadAdError.message}")
                        _state.update { it.copy(isInterstitialReady = false) }
                    }
                }
            )
        }.onFailure {
            isLoadingInterstitial = false
        }
    }

    fun preloadRewardedAd() {
        if (isLoadingRewarded || loadedRewarded != null) return
        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        runCatching {
            RewardedAd.load(
                appContext,
                config.resolvedRewardedUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        isLoadingRewarded = false
                        loadedRewarded = rewardedAd
                        _state.update {
                            it.copy(
                                isRewardedReady = true,
                                lastAdStatus = "Rewarded Ad Ready"
                            )
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingRewarded = false
                        loadedRewarded = null
                        Log.d(TAG, "Rewarded preload skipped/failed: ${loadAdError.message}")
                        _state.update { it.copy(isRewardedReady = false) }
                    }
                }
            )
        }.onFailure {
            isLoadingRewarded = false
        }
    }

    companion object {
        private const val TAG = "StudioAdManager"
    }
}
