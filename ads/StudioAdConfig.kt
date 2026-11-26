package com.example.ads

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Strongly-typed runtime representation of `app/src/main/assets/adConfig.js`.
 * Pasting real AdMob App/Unit IDs or Meta Audience Network Placement IDs into `adConfig.js`
 * automatically configures the native Android APK without needing Kotlin edits.
 */
data class StudioAdConfig(
    val testMode: Boolean = true,
    val admobAppId: String = DEFAULT_TEST_APP_ID,
    val admobBannerUnitId: String = DEFAULT_TEST_BANNER_ID,
    val admobNativeUnitId: String = DEFAULT_TEST_NATIVE_ID,
    val admobInterstitialUnitId: String = DEFAULT_TEST_INTERSTITIAL_ID,
    val admobRewardedUnitId: String = DEFAULT_TEST_REWARDED_ID,
    val metaMediationEnabled: Boolean = true,
    val metaBannerPlacementId: String = PLACEHOLDER_META_BANNER,
    val metaNativePlacementId: String = PLACEHOLDER_META_NATIVE,
    val metaInterstitialPlacementId: String = PLACEHOLDER_META_INTERSTITIAL,
    val metaRewardedPlacementId: String = PLACEHOLDER_META_REWARDED,
    val topBannerEnabledOnHome: Boolean = true,
    val topBannerEnabledOnTemplates: Boolean = true,
    val topBannerEnabledOnAiStudio: Boolean = true,
    val nativeCardIntervalInTemplates: Int = 4,
    val nativeCardOnExportComplete: Boolean = true,
    val interstitialOnlyAfterExportFinish: Boolean = true,
    val interstitialOnMediaImportOncePerSession: Boolean = true,
    val textToVideoRewardedAds: Int = 3,
    val textToImageRewardedAds: Int = 2,
    val autoCaptionsRewardedAds: Int = 2,
    val bgRemoverRewardedAds: Int = 2
) {
    /**
     * Resolved Banner (320x50) unit ID — guarantees Google's official test unit ID is used
     * while `testMode` is enabled or if a placeholder value is detected.
     */
    val resolvedBannerUnitId: String
        get() = resolveUnitId(admobBannerUnitId, DEFAULT_TEST_BANNER_ID)

    /**
     * Resolved Native / Medium Rectangle (300x250) unit ID.
     * Note: Medium Rectangle `AdView(AdSize.MEDIUM_RECTANGLE)` uses a Banner/MREC Ad Unit ID.
     * If a native-test ID is present in testMode, we safely map to Google's official test banner/MREC ID
     * so `AdView` loads reliably during testing, while live production IDs pass straight through.
     */
    val resolvedMediumRectangleUnitId: String
        get() = if (testMode || isPlaceholder(admobNativeUnitId) || admobNativeUnitId == DEFAULT_TEST_NATIVE_ID) {
            DEFAULT_TEST_BANNER_ID
        } else {
            admobNativeUnitId
        }

    val resolvedNativeUnitId: String
        get() = resolveUnitId(admobNativeUnitId, DEFAULT_TEST_NATIVE_ID)

    val resolvedInterstitialUnitId: String
        get() = resolveUnitId(admobInterstitialUnitId, DEFAULT_TEST_INTERSTITIAL_ID)

    val resolvedRewardedUnitId: String
        get() = resolveUnitId(admobRewardedUnitId, DEFAULT_TEST_REWARDED_ID)

    val hasLiveProductionIds: Boolean
        get() = !testMode &&
            !isPlaceholder(admobBannerUnitId) &&
            admobBannerUnitId != DEFAULT_TEST_BANNER_ID

    val activeMediationLabel: String
        get() = if (metaMediationEnabled) "AdMob • Meta Mediation" else "Google AdMob"

    fun toJsonString(): String = JSONObject().apply {
        put("testMode", testMode)
        put("admobAppId", admobAppId)
        put("bannerAdUnitId", resolvedBannerUnitId)
        put("nativeAdUnitId", resolvedNativeUnitId)
        put("interstitialAdUnitId", resolvedInterstitialUnitId)
        put("rewardedAdUnitId", resolvedRewardedUnitId)
        put("metaMediationEnabled", metaMediationEnabled)
        put("metaBannerPlacementId", metaBannerPlacementId)
        put("metaNativePlacementId", metaNativePlacementId)
        put("metaInterstitialPlacementId", metaInterstitialPlacementId)
        put("metaRewardedPlacementId", metaRewardedPlacementId)
        put("nativeCardIntervalInTemplates", nativeCardIntervalInTemplates)
        put("textToVideoRewardedAds", textToVideoRewardedAds)
        put("textToImageRewardedAds", textToImageRewardedAds)
        put("autoCaptionsRewardedAds", autoCaptionsRewardedAds)
        put("bgRemoverRewardedAds", bgRemoverRewardedAds)
    }.toString()

    private fun resolveUnitId(configured: String, testFallback: String): String {
        return if (testMode || isPlaceholder(configured)) testFallback else configured
    }

    private fun isPlaceholder(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isEmpty() ||
            trimmed.startsWith("YOUR_", ignoreCase = true) ||
            trimmed.contains("XXXX", ignoreCase = true)
    }

    companion object {
        private const val TAG = "StudioAdConfig"
        private const val CONFIG_ASSET_FILE = "adConfig.js"

        // Official Google AdMob Android Test IDs (never trigger invalid traffic warnings)
        const val DEFAULT_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
        const val DEFAULT_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val DEFAULT_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        const val DEFAULT_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val DEFAULT_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        const val PLACEHOLDER_META_BANNER = "YOUR_META_BANNER_PLACEMENT_ID"
        const val PLACEHOLDER_META_NATIVE = "YOUR_META_NATIVE_PLACEMENT_ID"
        const val PLACEHOLDER_META_INTERSTITIAL = "YOUR_META_INTERSTITIAL_PLACEMENT_ID"
        const val PLACEHOLDER_META_REWARDED = "YOUR_META_REWARDED_PLACEMENT_ID"

        fun loadFromAssets(context: Context): StudioAdConfig {
            val rawJs = runCatching {
                context.assets.open(CONFIG_ASSET_FILE).bufferedReader().use { it.readText() }
            }.getOrElse { err ->
                Log.w(TAG, "Falling back to default AdMob test config: ${err.message}")
                return StudioAdConfig()
            }

            fun extractString(key: String, default: String): String {
                val regex = Regex("""\b$key\s*:\s*["']([^"']+)["']""")
                return regex.find(rawJs)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    .ifEmpty { default }
            }

            fun extractBoolean(key: String, default: Boolean): Boolean {
                val regex = Regex("""\b$key\s*:\s*(true|false)\b""")
                return when (regex.find(rawJs)?.groupValues?.getOrNull(1)) {
                    "true" -> true
                    "false" -> false
                    else -> default
                }
            }

            fun extractInt(key: String, default: Int): Int {
                val regex = Regex("""\b$key\s*:\s*(\d+)\b""")
                return regex.find(rawJs)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: default
            }

            return StudioAdConfig(
                testMode = extractBoolean("testMode", true),
                admobAppId = extractString("appId", DEFAULT_TEST_APP_ID),
                admobBannerUnitId = extractString("bannerAdUnitId", DEFAULT_TEST_BANNER_ID),
                admobNativeUnitId = extractString("nativeAdUnitId", DEFAULT_TEST_NATIVE_ID),
                admobInterstitialUnitId = extractString("interstitialAdUnitId", DEFAULT_TEST_INTERSTITIAL_ID),
                admobRewardedUnitId = extractString("rewardedAdUnitId", DEFAULT_TEST_REWARDED_ID),
                metaMediationEnabled = extractBoolean("mediationEnabled", true),
                metaBannerPlacementId = extractString("bannerPlacementId", PLACEHOLDER_META_BANNER),
                metaNativePlacementId = extractString("nativePlacementId", PLACEHOLDER_META_NATIVE),
                metaInterstitialPlacementId = extractString("interstitialPlacementId", PLACEHOLDER_META_INTERSTITIAL),
                metaRewardedPlacementId = extractString("rewardedPlacementId", PLACEHOLDER_META_REWARDED),
                topBannerEnabledOnHome = extractBoolean("topBannerEnabledOnHome", true),
                topBannerEnabledOnTemplates = extractBoolean("topBannerEnabledOnTemplates", true),
                topBannerEnabledOnAiStudio = extractBoolean("topBannerEnabledOnAiStudio", true),
                nativeCardIntervalInTemplates = extractInt("nativeCardIntervalInTemplates", 4).coerceAtLeast(1),
                nativeCardOnExportComplete = extractBoolean("nativeCardOnExportComplete", true),
                interstitialOnlyAfterExportFinish = extractBoolean("interstitialOnlyAfterExportFinish", true),
                interstitialOnMediaImportOncePerSession = extractBoolean("interstitialOnMediaImportOncePerSession", true),
                textToVideoRewardedAds = extractInt("textToVideo", 3).coerceAtLeast(0),
                textToImageRewardedAds = extractInt("textToImage", 2).coerceAtLeast(0),
                autoCaptionsRewardedAds = extractInt("autoCaptions", 2).coerceAtLeast(0),
                bgRemoverRewardedAds = extractInt("bgRemover", 2).coerceAtLeast(0)
            )
        }
    }
}
