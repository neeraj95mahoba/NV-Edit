package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.viewmodel.HomeTab

enum class AppLanguage(
    val code: String,
    val shortLabel: String,
    val displayLabel: String
) {
    ENGLISH("en", "EN", "English"),
    HINDI("hi", "हिं", "हिंदी (Hindi)")
}

data class StudioStrings(
    val appName: String,
    val tabProjects: String,
    val tabPhoto: String,
    val tabTemplates: String,
    val tabAiLab: String,
    val tabInsights: String,
    val newProjectTitle: String,
    val newProjectSubtitle: String,
    val aiStudioTools: String,
    val seeAll: String,
    val savedProjects: String,
    val searchPlaceholder: String,
    val photoStudioTitle: String,
    val zeroWatermark: String,
    val pickPhoto: String,
    val export32k: String,
    val settingsTitle: String,
    val settingsSubtitle: String,
    val languageSection: String,
    val storageCleanerTitle: String,
    val storageCleanerSubtitle: String,
    val clearTempCache: String,
    val clearingCache: String,
    val freedCachePrefix: String,
    val defaultExportQuality: String,
    val autoSaveTitle: String,
    val autoSaveSubtitle: String,
    val studioAppearance: String,
    val lowNetworkMode: String,
    val aiAndShots: String,
    val privacyPolicy: String,
    val termsOfUse: String,
    val rateApp: String,
    val legalSectionTitle: String,
    val versionByAuthor: String,
    val noRateStoreToast: String
) {
    fun tabLabel(tab: HomeTab): String = when (tab) {
        HomeTab.PROJECTS -> tabProjects
        HomeTab.PHOTO_STUDIO -> tabPhoto
        HomeTab.TEMPLATES -> tabTemplates
        HomeTab.AI_LAB -> tabAiLab
        HomeTab.INSIGHTS -> tabInsights
    }
}

private val EnglishStrings = StudioStrings(
    appName = "NV Edit",
    tabProjects = "Projects",
    tabPhoto = "Photo",
    tabTemplates = "Templates",
    tabAiLab = "AI Lab",
    tabInsights = "Insights",
    newProjectTitle = "+ New Project",
    newProjectSubtitle = "4K 60fps • Multi-Track • Chroma Key • No Watermark",
    aiStudioTools = "AI Studio Tools",
    seeAll = "See all →",
    savedProjects = "Saved Projects",
    searchPlaceholder = "Search drafts by name or aspect ratio…",
    photoStudioTitle = "Photo Studio",
    zeroWatermark = "Zero Watermark",
    pickPhoto = "Pick",
    export32k = "Export 32K",
    settingsTitle = "Settings",
    settingsSubtitle = "PRO 4K–32K • Watermark-Free",
    languageSection = "App Language / भाषा",
    storageCleanerTitle = "Cache & Storage Cleaner",
    storageCleanerSubtitle = "Free up temp render files so editing stays fast",
    clearTempCache = "Clear Temp Cache",
    clearingCache = "Clearing…",
    freedCachePrefix = "Freed",
    defaultExportQuality = "Default Export Quality",
    autoSaveTitle = "Auto-Save Drafts",
    autoSaveSubtitle = "Automatically save timeline edits while working",
    studioAppearance = "Studio Appearance",
    lowNetworkMode = "Low-Network Lite Mode",
    aiAndShots = "AI & Shots",
    privacyPolicy = "Privacy Policy",
    termsOfUse = "Terms of Use",
    rateApp = "Rate App",
    legalSectionTitle = "Legal & App Info",
    versionByAuthor = "App Version 1.0.0 (by Neeraj Mahoba)",
    noRateStoreToast = "Thank you for supporting NV Edit! Store rating opens on published devices."
)

private val HindiStrings = StudioStrings(
    appName = "NV Edit",
    tabProjects = "प्रोजेक्ट्स",
    tabPhoto = "फोटो",
    tabTemplates = "टेम्पलेट्स",
    tabAiLab = "AI लैब",
    tabInsights = "इनसाइट्स",
    newProjectTitle = "+ नया प्रोजेक्ट",
    newProjectSubtitle = "4K 60fps • मल्टी-ट्रैक • क्रोमा की • बिना वॉटरमार्क",
    aiStudioTools = "AI स्टूडियो टूल्स",
    seeAll = "सभी देखें →",
    savedProjects = "सहेजे गए प्रोजेक्ट्स",
    searchPlaceholder = "नाम या रेशियो से ड्राफ्ट खोजें…",
    photoStudioTitle = "फोटो स्टूडियो",
    zeroWatermark = "बिना वॉटरमार्क",
    pickPhoto = "चुनें",
    export32k = "32K एक्सपोर्ट",
    settingsTitle = "सेटिंग्स",
    settingsSubtitle = "PRO 4K–32K • बिना वॉटरमार्क",
    languageSection = "ऐप की भाषा / App Language",
    storageCleanerTitle = "कैश और स्टोरेज क्लीनर",
    storageCleanerSubtitle = "फ़ोन तेज़ रखने के लिए अस्थायी कैश फ़ाइलें हटाएँ",
    clearTempCache = "अस्थायी कैश साफ़ करें",
    clearingCache = "साफ़ हो रहा है…",
    freedCachePrefix = "फ़्री हुआ",
    defaultExportQuality = "डिफ़ॉल्ट एक्सपोर्ट क्वालिटी",
    autoSaveTitle = "ऑटो-सेव ड्राफ्ट",
    autoSaveSubtitle = "एडिटिंग के दौरान बदलाव अपने आप सेव करें",
    studioAppearance = "स्टूडियो थीम",
    lowNetworkMode = "लो-नेटवर्क लाइट मोड",
    aiAndShots = "AI और शॉट्स",
    privacyPolicy = "गोपनीयता नीति",
    termsOfUse = "उपयोग की शर्तें",
    rateApp = "ऐप को रेट करें",
    legalSectionTitle = "कानूनी जानकारी और ऐप विवरण",
    versionByAuthor = "ऐप संस्करण 1.0.0 (नीरज महोबा द्वारा)",
    noRateStoreToast = "NV Edit को सपोर्ट करने के लिए धन्यवाद! पब्लिश्ड स्टोर पर रेटिंग खुलेगी।"
)

fun studioStringsFor(language: AppLanguage): StudioStrings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.HINDI -> HindiStrings
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalStudioStrings = staticCompositionLocalOf { EnglishStrings }
