package com.example.media

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Display
import com.example.data.model.DeviceCodecProfile
import kotlin.math.roundToInt

object CodecCapabilityChecker {

    fun inspectDevice(context: Context): DeviceCodecProfile {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var hevcSupported = false
        var bestCaps: MediaCodecInfo.VideoCapabilities? = null

        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            if (types.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) }) {
                hevcSupported = true
                val caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC).videoCapabilities ?: continue
                if (bestCaps == null ||
                    caps.supportedWidths.upper * caps.supportedHeights.upper >
                    bestCaps.supportedWidths.upper * bestCaps.supportedHeights.upper
                ) {
                    bestCaps = caps
                }
            }
        }

        if (bestCaps == null) {
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                if (info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }) {
                    bestCaps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities
                    break
                }
            }
        }

        val maxWidth = bestCaps?.supportedWidths?.upper ?: 1920
        val maxHeight = bestCaps?.supportedHeights?.upper ?: 1080
        val maxFps1080p = try {
            if (bestCaps != null && bestCaps.isSizeSupported(1920, 1080)) {
                bestCaps.getSupportedFrameRatesFor(1920, 1080).upper.roundToInt().coerceIn(30, 240)
            } else 60
        } catch (_: Exception) {
            60
        }
        val maxFps4k = try {
            if (bestCaps != null && bestCaps.isSizeSupported(3840, 2160)) {
                bestCaps.getSupportedFrameRatesFor(3840, 2160).upper.roundToInt().coerceIn(24, 120)
            } else 30
        } catch (_: Exception) {
            30
        }

        val hdrDisplay = try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val types = display?.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
            types.isNotEmpty()
        } catch (_: Exception) {
            false
        }

        return DeviceCodecProfile(
            hevcSupported = hevcSupported,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            maxFps1080p = maxFps1080p,
            maxFps4k = maxFps4k,
            hdrDisplaySupported = hdrDisplay
        )
    }

    /**
     * Aligns video dimensions to an even macroblock boundary (multiples of 16 or 2)
     * to prevent encoder stride artifacts, green lines, and 4-way quadrant splits.
     */
    fun alignDimensions(width: Int, height: Int, alignment: Int = 16): Pair<Int, Int> {
        val safeW = ((width.coerceAtLeast(64) + alignment / 2) / alignment) * alignment
        val safeH = ((height.coerceAtLeast(64) + alignment / 2) / alignment) * alignment
        return (safeW.coerceAtLeast(64) and 1.inv()) to (safeH.coerceAtLeast(64) and 1.inv())
    }

    fun computeTargetDimensions(
        sourceWidth: Int,
        sourceHeight: Int,
        maxShortSide: Int?
    ): Pair<Int, Int> {
        val w = sourceWidth.coerceAtLeast(64)
        val h = sourceHeight.coerceAtLeast(64)
        if (maxShortSide == null) {
            return alignDimensions(w, h, 2)
        }
        val isLandscape = w >= h
        val shortSide = if (isLandscape) h else w
        if (shortSide <= maxShortSide) {
            return alignDimensions(w, h, 8)
        }
        val scale = maxShortSide.toFloat() / shortSide.toFloat()
        val targetW = (w * scale).roundToInt()
        val targetH = (h * scale).roundToInt()
        return alignDimensions(targetW, targetH, 8)
    }
}
