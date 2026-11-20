package com.example.data.model

enum class ThemeMode(val key: String) {
    OBSIDIAN("obsidian"),
    MIDNIGHT("midnight"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: OBSIDIAN
    }
}

enum class ProjectStatus {
    READY,
    EDITING,
    EXPORTED,
    FAILED;

    companion object {
        fun fromRaw(raw: String?): ProjectStatus =
            entries.firstOrNull { it.name == raw } ?: READY
    }
}

enum class ExportResolution(val maxHeight: Int?) {
    SOURCE(null),
    FHD_1080P(1080),
    UHD_4K(2160);

    companion object {
        fun fromRaw(raw: String?): ExportResolution =
            entries.firstOrNull { it.name == raw } ?: SOURCE
    }
}

enum class ExportFps(val value: Int) {
    SOURCE(0),
    FPS_30(30),
    FPS_60(60),
    FPS_120(120);

    companion object {
        fun fromValue(value: Int): ExportFps =
            entries.firstOrNull { it.value == value } ?: SOURCE
    }
}

data class HdrGradingParams(
    val highlightRecovery: Float = 0.0f,
    val shadowLift: Float = 0.0f,
    val dynamicContrast: Float = 0.0f,
    val saturationVibrance: Float = 0.0f,
    val brightnessExposure: Float = 0.0f,
    val overallIntensity: Float = 0.85f,
    val highlightEnabled: Boolean = true,
    val shadowEnabled: Boolean = true,
    val contrastEnabled: Boolean = true,
    val saturationEnabled: Boolean = true,
    val brightnessEnabled: Boolean = true,
    val intensityEnabled: Boolean = true,
    val presetName: String = "Neutral"
) {
    companion object {
        val Neutral = HdrGradingParams(
            highlightRecovery = 0.0f,
            shadowLift = 0.0f,
            dynamicContrast = 0.0f,
            saturationVibrance = 0.0f,
            brightnessExposure = 0.0f,
            overallIntensity = 1.0f,
            presetName = "Neutral"
        )

        val DolbyCinema = HdrGradingParams(
            highlightRecovery = 0.62f,
            shadowLift = 0.38f,
            dynamicContrast = 0.54f,
            saturationVibrance = 0.42f,
            brightnessExposure = 0.06f,
            overallIntensity = 0.88f,
            presetName = "Dolby Cinema"
        )

        val VividHdr = HdrGradingParams(
            highlightRecovery = 0.55f,
            shadowLift = 0.52f,
            dynamicContrast = 0.68f,
            saturationVibrance = 0.72f,
            brightnessExposure = 0.10f,
            overallIntensity = 0.90f,
            presetName = "Vivid HDR"
        )

        val FilmicLog = HdrGradingParams(
            highlightRecovery = 0.75f,
            shadowLift = 0.44f,
            dynamicContrast = 0.38f,
            saturationVibrance = 0.25f,
            brightnessExposure = 0.04f,
            overallIntensity = 0.82f,
            presetName = "Filmic Log"
        )

        val NightLift = HdrGradingParams(
            highlightRecovery = 0.40f,
            shadowLift = 0.78f,
            dynamicContrast = 0.42f,
            saturationVibrance = 0.36f,
            brightnessExposure = 0.22f,
            overallIntensity = 0.86f,
            presetName = "Night Lift"
        )

        val Presets = listOf(DolbyCinema, VividHdr, FilmicLog, NightLift)
    }
}

data class HdrProject(
    val id: String = "",
    val title: String = "Untitled Clip",
    val sourceUri: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val durationMs: Long = 0L,
    val status: String = ProjectStatus.READY.name,
    val presetName: String = "Dolby Cinema",
    val highlightRecovery: Double = 0.62,
    val shadowLift: Double = 0.38,
    val dynamicContrast: Double = 0.54,
    val saturationVibrance: Double = 0.42,
    val brightnessExposure: Double = 0.06,
    val overallIntensity: Double = 0.88,
    val highlightEnabled: Boolean = true,
    val shadowEnabled: Boolean = true,
    val contrastEnabled: Boolean = true,
    val saturationEnabled: Boolean = true,
    val brightnessEnabled: Boolean = true,
    val intensityEnabled: Boolean = true,
    val lastExportResolution: String = ExportResolution.SOURCE.name,
    val lastExportFps: Int = 0,
    val lastExportUri: String? = null,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    fun toGradingParams(): HdrGradingParams = HdrGradingParams(
        highlightRecovery = highlightRecovery.toFloat().coerceIn(0f, 1f),
        shadowLift = shadowLift.toFloat().coerceIn(0f, 1f),
        dynamicContrast = dynamicContrast.toFloat().coerceIn(0f, 1f),
        saturationVibrance = saturationVibrance.toFloat().coerceIn(0f, 1f),
        brightnessExposure = brightnessExposure.toFloat().coerceIn(-1f, 1f),
        overallIntensity = overallIntensity.toFloat().coerceIn(0f, 1f),
        highlightEnabled = highlightEnabled,
        shadowEnabled = shadowEnabled,
        contrastEnabled = contrastEnabled,
        saturationEnabled = saturationEnabled,
        brightnessEnabled = brightnessEnabled,
        intensityEnabled = intensityEnabled,
        presetName = presetName
    )

    val projectStatus: ProjectStatus
        get() = ProjectStatus.fromRaw(status)

    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            else -> "${width}×${height}"
        }
}

data class DeviceCodecProfile(
    val hevcSupported: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFps1080p: Int,
    val maxFps4k: Int,
    val hdrDisplaySupported: Boolean
) {
    fun canEncode(targetWidth: Int, targetHeight: Int, targetFps: Int): Boolean {
        val longSide = maxOf(targetWidth, targetHeight)
        val shortSide = minOf(targetWidth, targetHeight)
        if (longSide > maxOf(maxWidth, maxHeight) || shortSide > minOf(maxWidth, maxHeight)) {
            return false
        }
        val effectiveFpsLimit = if (longSide >= 3840 || shortSide >= 2160) maxFps4k else maxFps1080p
        return targetFps <= effectiveFpsLimit
    }
}
