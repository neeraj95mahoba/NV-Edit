/**
 * NV Edit — Central Ad Monetization Configuration (Google AdMob + Meta Audience Network Mediation)
 *
 * HOW TO GO LIVE:
 * 1. Paste your real Google AdMob App ID below AND in `app/src/main/AndroidManifest.xml`
 *    under `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ... />`.
 * 2. Paste your real AdMob Banner (320x50), Native / Medium Rectangle (300x250),
 *    Interstitial (post-export & media import), and Rewarded Ad Unit IDs below.
 * 3. Link Meta Audience Network in AdMob Mediation and paste your Meta Placement IDs below.
 * 4. Change `testMode: false` to switch from Google Test Ads / Previews to live production ads.
 */
(function (global) {
  "use strict";

  const NV_EDIT_AD_CONFIG = {
    // Set to false once you replace the test IDs below with your live production IDs
    testMode: true,

    // Google AdMob Configuration (defaulting to official Google AdMob Android Test IDs)
    admob: {
      appId: "ca-app-pub-3940256099942544~3347511713",
      bannerAdUnitId: "ca-app-pub-3940256099942544/6300978111",
      nativeAdUnitId: "ca-app-pub-3940256099942544/2247696110",
      interstitialAdUnitId: "ca-app-pub-3940256099942544/1033173712",
      rewardedAdUnitId: "ca-app-pub-3940256099942544/5224354917"
    },

    // Meta Audience Network Mediation Placement IDs (used via AdMob Mediation adapter)
    metaAudienceNetwork: {
      mediationEnabled: true,
      bannerPlacementId: "YOUR_META_BANNER_PLACEMENT_ID",
      nativePlacementId: "YOUR_META_NATIVE_PLACEMENT_ID",
      interstitialPlacementId: "YOUR_META_INTERSTITIAL_PLACEMENT_ID",
      rewardedPlacementId: "YOUR_META_REWARDED_PLACEMENT_ID"
    },

    // Non-Intrusive Placement Rules (Truecaller-Style + Export & AI Studio Rewarded Unlocks)
    placements: {
      topBannerEnabledOnHome: true,
      topBannerEnabledOnTemplates: true,
      topBannerEnabledOnAiStudio: true,
      nativeCardIntervalInTemplates: 4,
      nativeCardOnExportComplete: true,
      interstitialOnlyAfterExportFinish: true,
      interstitialOnMediaImportOncePerSession: true,
      neverShowAdsInEditorWorkspace: true,
      rewardedPreviewDurationSeconds: 5,
      rewardedAdsByResolution: {
        "720p": 0,
        "1080p": 0,
        "2K": 0,
        "4K": 0,
        "8K": 1,
        "16K": 2,
        "32K": 3
      },
      rewardedAdsByAiTool: {
        textToVideo: 3,
        textToImage: 2,
        autoCaptions: 2,
        bgRemover: 2
      }
    }
  };

  /**
   * Unified Web / Native Android Bridge (`window.AndroidAds`)
   * - Inside the packaged Android APK, `window.AndroidAds` is injected natively via
   *   `AndroidAdsBridge` (@JavascriptInterface) and triggers real AdMob / Meta native ads.
   * - Supports sequential rewarded calls: `window.AndroidAds.showRewardedAd(adNumber, totalAds)`
   * - In standalone web preview mode, calls automatically resolve to sleek 5-second preview modals.
   */
  const NVEditAdBridge = {
    config: NV_EDIT_AD_CONFIG,

    isNativeAndroidApk: function () {
      return typeof global.AndroidAds !== "undefined" && global.AndroidAds !== null;
    },

    getRequiredRewardedAdsForResolution: function (resolutionLabel) {
      const map = NV_EDIT_AD_CONFIG.placements.rewardedAdsByResolution;
      return typeof map[resolutionLabel] === "number" ? map[resolutionLabel] : 0;
    },

    getRequiredRewardedAdsForAiTool: function (toolKey) {
      const map = NV_EDIT_AD_CONFIG.placements.rewardedAdsByAiTool;
      return typeof map[toolKey] === "number" ? map[toolKey] : 0;
    },

    showBannerAd: function () {
      if (this.isNativeAndroidApk() && typeof global.AndroidAds.showBannerAd === "function") {
        return global.AndroidAds.showBannerAd();
      }
      return { mode: "web_preview", slot: "top_sticky_banner_320x50", active: true };
    },

    showNativeAd: function () {
      if (this.isNativeAndroidApk() && typeof global.AndroidAds.showNativeAd === "function") {
        return global.AndroidAds.showNativeAd();
      }
      return { mode: "web_preview", slot: "native_medium_rectangle_300x250", active: true };
    },

    showInterstitialAd: function () {
      if (this.isNativeAndroidApk() && typeof global.AndroidAds.showInterstitialAd === "function") {
        return global.AndroidAds.showInterstitialAd();
      }
      return { mode: "web_preview", slot: "interstitial_ad", active: true };
    },

    showRewardedAd: function (adNumber, totalAds) {
      const current = typeof adNumber === "number" && adNumber > 0 ? adNumber : 1;
      const total = typeof totalAds === "number" && totalAds >= current ? totalAds : current;
      if (this.isNativeAndroidApk() && typeof global.AndroidAds.showRewardedAd === "function") {
        return global.AndroidAds.showRewardedAd(current, total);
      }
      return {
        mode: "web_preview",
        slot: "sequential_rewarded_ad",
        adNumber: current,
        totalAds: total,
        countdownSeconds: NV_EDIT_AD_CONFIG.placements.rewardedPreviewDurationSeconds,
        status: "Playing Ad " + current + " of " + total + "..."
      };
    }
  };

  global.NV_EDIT_AD_CONFIG = NV_EDIT_AD_CONFIG;
  global.NVEditAdBridge = NVEditAdBridge;
})(typeof window !== "undefined" ? window : globalThis);
