package com.example

import com.example.data.model.ExportFps
import com.example.data.model.ExportResolution
import com.example.data.model.HdrGradingParams
import com.example.data.model.HdrProject
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.media.CodecCapabilityChecker
import com.example.media.HdrShaderController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dimensionAlignment_alwaysProducesEvenMacroblockMultiples() {
        val (w1, h1) = CodecCapabilityChecker.alignDimensions(1921, 1081, alignment = 16)
        assertEquals(0, w1 % 2)
        assertEquals(0, h1 % 2)
        assertEquals(0, w1 % 16)
        assertEquals(0, h1 % 16)

        val (w4k, h4k) = CodecCapabilityChecker.computeTargetDimensions(3840, 2160, 1080)
        assertEquals(1920, w4k)
        assertEquals(1080, h4k)
        assertEquals(0, w4k % 2)
        assertEquals(0, h4k % 2)
    }

    @Test
    fun gradingParams_roundTripFromProjectSafely() {
        val project = HdrProject(
            id = "proj_1",
            title = "Test Clip",
            width = 3840,
            height = 2160,
            fps = 60,
            highlightRecovery = 0.72,
            shadowLift = 0.45,
            dynamicContrast = 0.58,
            saturationVibrance = 0.49,
            brightnessExposure = 0.12,
            overallIntensity = 0.90
        )
        val params = project.toGradingParams()
        assertEquals(0.72f, params.highlightRecovery, 0.001f)
        assertEquals(0.45f, params.shadowLift, 0.001f)
        assertEquals("4K", project.resolutionLabel)
        assertEquals(ExportResolution.SOURCE, ExportResolution.fromRaw(project.lastExportResolution))
        assertEquals(ExportFps.SOURCE, ExportFps.fromValue(project.lastExportFps))
        assertTrue(HdrGradingParams.Presets.isNotEmpty())
    }

    @Test
    fun localization_switchesAllLanguagesCleanly() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val bn = stringsFor(AppLanguage.BENGALI)
        val ar = stringsFor(AppLanguage.ARABIC)
        assertEquals("HDR Studios", en.appTitle)
        assertEquals("v2.0.0", en.versionBadge)
        assertEquals("Standard", en.viewModeStandard)
        assertEquals("HDR Enhanced", en.viewModeHdr)
        assertFalse(bn.selectVideoSub == en.selectVideoSub)
        assertFalse(bn.viewModeStandard == en.viewModeStandard)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, AppLanguage.ARABIC.layoutDirection)
        assertTrue(ar.socialFacebook.isNotBlank())
    }

    @Test
    fun shaderController_togglesBetweenStandardAndHdrEnhancedModes() {
        val controller = HdrShaderController()
        assertTrue(controller.current().hdrEnhanced)

        controller.update(
            params = HdrGradingParams.VividHdr,
            splitPosition = 0.5f,
            splitEnabled = true,
            hdrEnhanced = false
        )
        assertFalse(controller.current().hdrEnhanced)
        assertEquals("Vivid HDR", controller.current().params.presetName)

        controller.update(
            params = HdrGradingParams.VividHdr,
            splitPosition = 0.65f,
            splitEnabled = true,
            hdrEnhanced = true
        )
        assertTrue(controller.current().hdrEnhanced)
        assertEquals(0.65f, controller.current().splitPosition, 0.001f)
    }
}
