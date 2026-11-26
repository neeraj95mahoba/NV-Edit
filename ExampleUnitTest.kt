package com.example

import com.example.model.PhotoExportFormat
import com.example.model.PhotoExportResolution
import com.example.model.PhotoSpecCalculator
import com.example.model.PhotoUpscaleMultiplier
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun photoUpscaleAndExportAdGatesMatchSpec() {
    assertEquals(0, PhotoUpscaleMultiplier.X2.requiredRewardedAds)
    assertEquals(0, PhotoUpscaleMultiplier.X4.requiredRewardedAds)
    assertEquals(1, PhotoUpscaleMultiplier.X8.requiredRewardedAds)
    assertEquals(2, PhotoUpscaleMultiplier.X10.requiredRewardedAds)

    assertEquals(0, PhotoExportResolution.RES_4K.requiredRewardedAds)
    assertEquals(1, PhotoExportResolution.RES_8K.requiredRewardedAds)
    assertEquals(2, PhotoExportResolution.RES_16K.requiredRewardedAds)
    assertEquals(3, PhotoExportResolution.RES_32K.requiredRewardedAds)
  }

  @Test
  fun photoSpecCalculatorComputes32KTiledGrid() {
    val est = PhotoSpecCalculator.estimateExport(
      sourceW = 3840,
      sourceH = 2160,
      resolution = PhotoExportResolution.RES_32K,
      format = PhotoExportFormat.PNG,
      dpi = 300
    )
    assertEquals(30720, est.targetWidthPx)
    assertEquals(17280, est.targetHeightPx)
    assertTrue(est.tileGridLabel.contains("chunks"))
  }

  @Test
  fun hindiAndEnglishLocalizationContainVersionAndKeyLabels() {
    val en = com.example.ui.i18n.studioStringsFor(com.example.ui.i18n.AppLanguage.ENGLISH)
    val hi = com.example.ui.i18n.studioStringsFor(com.example.ui.i18n.AppLanguage.HINDI)

    assertEquals("+ New Project", en.newProjectTitle)
    assertEquals("+ नया प्रोजेक्ट", hi.newProjectTitle)
    assertEquals("Clear Temp Cache", en.clearTempCache)
    assertTrue(hi.versionByAuthor.contains("1.0.0"))
    assertTrue(en.versionByAuthor.contains("Neeraj Mahoba"))
  }
}
