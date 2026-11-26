package com.example.ui.components

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
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalStudioStrings
import com.example.ui.theme.AcidLime
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold

enum class LegalDialogTab {
    PRIVACY,
    TERMS
}

@Composable
fun PrivacyAndStoreInfoDialog(
    initialTab: LegalDialogTab = LegalDialogTab.PRIVACY,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val strings = LocalStudioStrings.current
    val isHindi = LocalAppLanguage.current == AppLanguage.HINDI
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonStudioGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == LegalDialogTab.PRIVACY) Icons.Filled.VerifiedUser else Icons.Filled.Gavel,
                        contentDescription = null,
                        tint = OledBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (selectedTab == LegalDialogTab.PRIVACY) {
                            "NV Edit • ${strings.privacyPolicy}"
                        } else {
                            "NV Edit • ${strings.termsOfUse}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Neeraj Mahoba",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegalTabChip(
                        label = strings.privacyPolicy,
                        selected = selectedTab == LegalDialogTab.PRIVACY,
                        onClick = { selectedTab = LegalDialogTab.PRIVACY },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("legal_tab_privacy")
                    )
                    LegalTabChip(
                        label = strings.termsOfUse,
                        selected = selectedTab == LegalDialogTab.TERMS,
                        onClick = { selectedTab = LegalDialogTab.TERMS },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("legal_tab_terms")
                    )
                }

                if (selectedTab == LegalDialogTab.PRIVACY) {
                    PolicyInfoCard(
                        icon = Icons.Filled.PrivacyTip,
                        accent = NeonCyan,
                        title = if (isHindi) "मीडिया और स्टोरेज गोपनीयता" else "Media & Zero-Permission Privacy",
                        body = if (isHindi) {
                            "NV Edit केवल Android Photo Picker का उपयोग करता है ताकि आप केवल वही फोटो और वीडियो चुनें जिन्हें आप एडिट करना चाहते हैं। सभी ड्राफ्ट आपके फ़ोन में सुरक्षित रहते हैं और एक्सपोर्ट सीधे गैलरी में सेव होते हैं।"
                        } else {
                            "NV Edit uses the zero-permission Android Photo Picker so you choose only the videos, photos, and audio clips you want to edit. Project drafts stay locally on your device, and exported MP4/Photo files save straight to your Gallery."
                        }
                    )
                    PolicyInfoCard(
                        icon = Icons.Filled.AdsClick,
                        accent = ProGold,
                        title = if (isHindi) "Google AdMob और Meta विज्ञापन नीति" else "Ad Monetization & Mediation",
                        body = if (isHindi) {
                            "ऐप को 100% मुफ्त और बिना वॉटरमार्क रखने के लिए Google AdMob और Meta Audience Network के रिवॉर्डेड व बैनर विज्ञापन दिखाए जाते हैं। विज्ञापन पार्टनर केवल मानक डिवाइस विज्ञापन आईडी का उपयोग कर सकते हैं।"
                        } else {
                            "To keep all 4K–32K editing tools unlocked and watermark-free, NV Edit displays rewarded, interstitial, and banner ads via Google AdMob & Meta Audience Network mediation following Indus Appstore & GetApps guidelines."
                        }
                    )
                    PolicyInfoCard(
                        icon = Icons.Filled.CloudDone,
                        accent = AcidLime,
                        title = if (isHindi) "AI प्रॉम्प्ट और डेटा सुरक्षा" else "Cloud AI & Content Transparency",
                        body = if (isHindi) {
                            "AI Video Lab केवल आपके द्वारा टाइप किए गए प्रॉम्प्ट को सुरक्षित HTTPS/App Check के माध्यम से प्रोसेस करता है। आपकी निजी गैलरी फ़ाइलें बिना अनुमति अपलोड नहीं होतीं।"
                        } else {
                            "AI Video Lab connects securely over HTTPS with App Check. Only prompts you intentionally submit are processed to generate creative media; your private local gallery files are never uploaded without action."
                        }
                    )
                } else {
                    PolicyInfoCard(
                        icon = Icons.Filled.OfflineBolt,
                        accent = NeonCyan,
                        title = if (isHindi) "क्रिएटर स्वामित्व और बिना वॉटरमार्क" else "Creator Rights & Zero Watermark",
                        body = if (isHindi) {
                            "NV Edit में बनाए और एक्सपोर्ट किए गए सभी वीडियो व फोटो पर आपका 100% स्वामित्व है। आप इन्हें व्यक्तिगत या व्यावसायिक उपयोग के लिए बिना किसी वॉटरमार्क के साझा कर सकते हैं।"
                        } else {
                            "You retain 100% ownership and commercial rights to every video and photo you edit and export with NV Edit. All exports up to 4K video and 32K photo are completely watermark-free."
                        }
                    )
                    PolicyInfoCard(
                        icon = Icons.Filled.Gavel,
                        accent = ProGold,
                        title = if (isHindi) "ज़िम्मेदार उपयोग और AI सामग्री" else "Acceptable Use & AI Features",
                        body = if (isHindi) {
                            "कृपया कॉपीराइट सामग्री और AI जनरेटेड मीडिया का उपयोग लागू कानूनों के अनुसार करें। रिवॉर्डेड अनलॉक वर्तमान सेशन में Pro AI और अल्ट्रा-HD एक्सपोर्ट सुविधाएँ प्रदान करते हैं।"
                        } else {
                            "Users are responsible for ensuring imported audio, video, and AI prompts comply with applicable copyright and community guidelines. Rewarded ad unlocks grant full Pro AI and Ultra-HD export access for the requested tool."
                        }
                    )
                    PolicyInfoCard(
                        icon = Icons.Filled.VerifiedUser,
                        accent = AcidLime,
                        title = if (isHindi) "डेवलपर जानकारी" else "Publisher & App Info",
                        body = if (isHindi) {
                            "NV Edit (संस्करण ${BuildConfig.VERSION_NAME}) • डेवलपर: नीरज महोबा। Indus Appstore, Xiaomi GetApps और Android डिवाइसेज़ के लिए अनुकूलित।"
                        } else {
                            "NV Edit v${BuildConfig.VERSION_NAME} • Published by Neeraj Mahoba. Optimized for Indus Appstore, Xiaomi GetApps, and low-storage Android devices."
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_privacy_info_dialog")
            ) {
                Text("OK", color = palette.accentSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun LegalTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accentPrimary.copy(alpha = 0.24f) else palette.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 1.dp,
            color = if (selected) palette.accentSecondary else palette.border
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accentSecondary else palette.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PolicyInfoCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}
