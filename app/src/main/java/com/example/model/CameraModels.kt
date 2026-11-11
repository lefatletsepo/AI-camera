package com.example.model

import android.net.Uri

enum class CameraMode(val title: String, val badge: String) {
    PRO("PRO", "MANUAL"),
    NIGHT("NIGHT", "LOW LIGHT"),
    PHOTO("PHOTO", "AI"),
    VIDEO("VIDEO", "4K UHD"),
    PORTRAIT("PORTRAIT", "BOKEH"),
    HDR("HDR", "DYNAMIC")
}

enum class FlashMode(val title: String) {
    OFF("Off"),
    ON("On"),
    AUTO("Auto"),
    TORCH("Torch")
}

enum class AspectRatioSetting(val title: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f)
}

enum class TimerSetting(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s")
}

enum class AiQuality(val label: String, val multiplier: Float) {
    FAST("Fast (Quick Post-Process)", 0.6f),
    BALANCED("Balanced (Pro Dynamic)", 0.85f),
    ULTRA("Ultra Quality (Flagship Super-Res)", 1.0f)
}

data class CameraSettings(
    val flashMode: FlashMode = FlashMode.AUTO,
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_4_3,
    val timerSetting: TimerSetting = TimerSetting.OFF,
    val isGridEnabled: Boolean = true,
    val is4kVideoEnabled: Boolean = true,
    val fps60Enabled: Boolean = true,
    val autoAiEnhance: Boolean = true,
    val aiQuality: AiQuality = AiQuality.BALANCED,
    val skinTonePreservation: Boolean = true,
    val shadowRecoveryIntensity: Float = 0.8f,
    val denoiseIntensity: Float = 0.7f,
    val shutterSoundEnabled: Boolean = true,
    val preferredLensFacing: Int = 0 // 0 = Back, 1 = Front
)

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val isEnhanced: Boolean,
    val dateAdded: Long,
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L
)
