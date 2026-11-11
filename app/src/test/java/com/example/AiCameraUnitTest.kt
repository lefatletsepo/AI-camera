package com.example

import com.example.model.AiQuality
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.model.FlashMode
import com.example.model.TimerSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCameraUnitTest {

    @Test
    fun testCameraModes() {
        assertEquals("PHOTO", CameraMode.PHOTO.title)
        assertEquals("VIDEO", CameraMode.VIDEO.title)
        assertEquals("4K UHD", CameraMode.VIDEO.badge)
        assertEquals("PRO", CameraMode.PRO.title)
        assertEquals("MANUAL", CameraMode.PRO.badge)
    }

    @Test
    fun testDefaultSettings() {
        val settings = CameraSettings()
        assertEquals(FlashMode.AUTO, settings.flashMode)
        assertEquals(AspectRatioSetting.RATIO_4_3, settings.aspectRatio)
        assertEquals(TimerSetting.OFF, settings.timerSetting)
        assertTrue(settings.isGridEnabled)
        assertTrue(settings.is4kVideoEnabled)
        assertTrue(settings.skinTonePreservation)
        assertEquals(AiQuality.BALANCED, settings.aiQuality)
    }

    @Test
    fun testAspectRatioRatios() {
        val r43 = AspectRatioSetting.RATIO_4_3.ratio
        val r169 = AspectRatioSetting.RATIO_16_9.ratio
        val r11 = AspectRatioSetting.RATIO_1_1.ratio

        assertTrue(r43 > 1.3f && r43 < 1.34f)
        assertTrue(r169 > 1.7f && r169 < 1.78f)
        assertEquals(1.0f, r11, 0.001f)
    }
}
