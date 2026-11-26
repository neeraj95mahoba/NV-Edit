package com.example.ads

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * JavaScript-to-Native Android Ads Bridge (`window.AndroidAds`).
 * Attach to any WebView via `bridge.attachToWebView(webView)` so hybrid/web callers using
 * `adConfig.js` automatically switch from 5-second web preview modals to real native Android
 * Google AdMob / Meta Audience Network ads when packaged as an APK.
 */
class AndroidAdsBridge(
    private val adController: StudioAdController
) {
    @JavascriptInterface
    fun showBannerAd(): String {
        adController.showBannerAd()
        return """{"status":"ok","slot":"banner_320x50","mode":"native_android"}"""
    }

    @JavascriptInterface
    fun showNativeAd(): String {
        adController.showNativeAd()
        return """{"status":"ok","slot":"native_300x250","mode":"native_android"}"""
    }

    @JavascriptInterface
    fun showInterstitialAd(): String {
        adController.showInterstitialAd()
        return """{"status":"ok","slot":"interstitial_post_export","mode":"native_android"}"""
    }

    @JavascriptInterface
    fun showRewardedAd(): String {
        return showRewardedAd(1, 1)
    }

    @JavascriptInterface
    fun showRewardedAd(adNumber: Int, totalAds: Int): String {
        val safeTotal = totalAds.coerceAtLeast(1)
        val safeNumber = adNumber.coerceIn(1, safeTotal)
        adController.showRewardedAd(
            adNumber = safeNumber,
            totalAds = safeTotal
        )
        return """{"status":"ok","slot":"rewarded_ad","adNumber":$safeNumber,"totalAds":$safeTotal,"mode":"native_android"}"""
    }

    @JavascriptInterface
    fun getAdConfigJson(): String {
        return adController.config.toJsonString()
    }

    @SuppressLint("JavascriptInterface")
    fun attachToWebView(webView: WebView) {
        webView.addJavascriptInterface(this, JS_INTERFACE_NAME)
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidAds"
    }
}
