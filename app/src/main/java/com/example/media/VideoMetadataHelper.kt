package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.HdrGradingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

data class VideoInspectionResult(
    val title: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMs: Long
)

object VideoMetadataHelper {

    suspend fun inspectVideo(context: Context, uri: Uri): VideoInspectionResult =
        withContext(Dispatchers.IO) {
            var title = queryDisplayName(context, uri) ?: "HDR Clip"
            if (title.contains('.')) {
                title = title.substringBeforeLast('.')
            }
            var width = 1920
            var height = 1080
            var durationMs = 0L
            var fps = 30

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
                val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rot == 90 || rot == 270) {
                    width = rawH
                    height = rawW
                } else {
                    width = rawW
                    height = rawH
                }
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                if (captureFps != null && captureFps > 1f) {
                    fps = captureFps.toInt().coerceIn(1, 240)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            if (fps == 30) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, uri, null)
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            val trackFps = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                            if (trackFps in 1..240) {
                                fps = trackFps
                                break
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    try {
                        extractor.release()
                    } catch (_: Exception) {
                    }
                }
            }

            VideoInspectionResult(
                title = title.ifBlank { "HDR Clip" }.take(120),
                width = width.coerceIn(64, 8192),
                height = height.coerceIn(64, 8192),
                fps = fps.coerceIn(1, 240),
                durationMs = durationMs.coerceAtLeast(0L)
            )
        }

    suspend fun extractAndCacheThumbnail(
        context: Context,
        uri: Uri,
        projectId: String
    ): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val outFile = File(dir, "$projectId.jpg")
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            if (frame != null) {
                val scaled = Bitmap.createScaledBitmap(frame, 360, (360f * frame.height / max(1, frame.width)).toInt().coerceAtLeast(180), true)
                FileOutputStream(outFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
                }
                if (scaled != frame) scaled.recycle()
                frame.recycle()
                return@withContext outFile.absolutePath
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
        null
    }

    /**
     * Analyzes actual sampled video frame luminance & chroma distribution to compute
     * balanced, artifact-free HDR Dynamic Range grading parameters.
     */
    suspend fun computeAutoBalance(
        context: Context,
        uri: Uri,
        positionMs: Long
    ): HdrGradingParams = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
                ?: return@withContext HdrGradingParams.DolbyCinema

            val sampleW = 128
            val sampleH = 72
            val small = Bitmap.createScaledBitmap(frame, sampleW, sampleH, true)
            val pixels = IntArray(sampleW * sampleH)
            small.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
            if (small != frame) small.recycle()
            frame.recycle()

            var sumLuma = 0.0
            var darkPixels = 0
            var brightPixels = 0
            var sumSat = 0.0

            for (p in pixels) {
                val r = Color.red(p) / 255.0
                val g = Color.green(p) / 255.0
                val b = Color.blue(p) / 255.0
                val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
                val mx = max(r, max(g, b))
                val mn = min(r, min(g, b))
                val sat = if (mx > 0.001) (mx - mn) / mx else 0.0
                sumLuma += luma
                sumSat += sat
                if (luma < 0.22) darkPixels++
                if (luma > 0.80) brightPixels++
            }

            val total = pixels.size.toDouble().coerceAtLeast(1.0)
            val avgLuma = sumLuma / total
            val darkRatio = darkPixels / total
            val brightRatio = brightPixels / total
            val avgSat = sumSat / total

            val highlightRecovery = (0.35 + brightRatio * 1.4).toFloat().coerceIn(0.25f, 0.85f)
            val shadowLift = (0.25 + darkRatio * 1.2).toFloat().coerceIn(0.20f, 0.82f)
            val contrast = (0.48 + (0.5 - kotlin.math.abs(avgLuma - 0.5)) * 0.25).toFloat().coerceIn(0.35f, 0.72f)
            val vibrance = (0.55 - avgSat * 0.45).toFloat().coerceIn(0.22f, 0.68f)
            val exposure = ((0.46 - avgLuma) * 0.35).toFloat().coerceIn(-0.20f, 0.28f)

            HdrGradingParams(
                highlightRecovery = highlightRecovery,
                shadowLift = shadowLift,
                dynamicContrast = contrast,
                saturationVibrance = vibrance,
                brightnessExposure = exposure,
                overallIntensity = 0.88f,
                presetName = "Auto Balance"
            )
        } catch (_: Exception) {
            HdrGradingParams.DolbyCinema
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isUriReadable(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
