package com.example.media

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.R
import com.example.data.model.ExportFps
import com.example.data.model.ExportResolution
import com.example.data.model.HdrGradingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val progressPercent: Int) : ExportState
    data class Completed(val galleryUri: Uri, val displayName: String) : ExportState
    data class Failed(val message: String) : ExportState
    data object Cancelled : ExportState
}

@OptIn(UnstableApi::class)
class VideoExportManager(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    fun startExport(
        sourceUri: Uri,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceFps: Int,
        grading: HdrGradingParams,
        resolution: ExportResolution,
        fpsOption: ExportFps,
        title: String,
        onStateChanged: (ExportState) -> Unit
    ) {
        cancelExport()

        val deviceProfile = CodecCapabilityChecker.inspectDevice(context)
        val (targetW, targetH) = CodecCapabilityChecker.computeTargetDimensions(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            maxShortSide = resolution.maxHeight
        )
        val requestedFps = if (fpsOption == ExportFps.SOURCE) sourceFps else fpsOption.value
        val safeFps = requestedFps.coerceIn(24, if (targetW >= 3840 || targetH >= 2160) deviceProfile.maxFps4k else deviceProfile.maxFps1080p)

        val tempFile = File(context.cacheDir, "hdr_export_${System.currentTimeMillis()}.mp4")
        val exportShaderController = HdrShaderController(
            HdrShaderRenderState(
                params = grading.copy(),
                splitPosition = 0f,
                splitEnabled = false
            )
        )

        val videoEffects = mutableListOf<Effect>(
            HdrToneGlEffect(exportShaderController),
            Presentation.createForWidthAndHeight(
                targetW,
                targetH,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        )

        val editedMediaItemBuilder = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
            .setEffects(
                androidx.media3.transformer.Effects(
                    emptyList(), // Keep original audio track intact
                    videoEffects
                )
            )
        if (fpsOption != ExportFps.SOURCE) {
            editedMediaItemBuilder.setFrameRate(safeFps)
        }
        val editedMediaItem = editedMediaItemBuilder.build()

        val bitrate = when {
            maxOf(targetW, targetH) >= 3840 -> 36_000_000
            maxOf(targetW, targetH) >= 1920 -> 18_000_000
            else -> 10_000_000
        }

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(bitrate)
                    .build()
            )
            .setEnableFallback(true)
            .build()

        val mimeType = if (deviceProfile.hevcSupported) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(mimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    stopProgressPolling()
                    activeTransformer = null
                    saveTempFileToGallery(
                        tempFile = tempFile,
                        title = title,
                        onSaved = { uri, displayName ->
                            postCompletionNotification(displayName)
                            onStateChanged(ExportState.Completed(uri, displayName))
                        },
                        onError = { err ->
                            onStateChanged(ExportState.Failed(err))
                        }
                    )
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    stopProgressPolling()
                    activeTransformer = null
                    tempFile.delete()
                    onStateChanged(
                        ExportState.Failed(
                            exportException.localizedMessage ?: "Hardware encoder export error"
                        )
                    )
                }
            })
            .build()

        activeTransformer = transformer
        onStateChanged(ExportState.Running(0))

        val composition = Composition.Builder(
            androidx.media3.transformer.EditedMediaItemSequence.Builder(editedMediaItem).build()
        )
            .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
            .build()

        transformer.start(composition, tempFile.absolutePath)
        startProgressPolling(transformer, onStateChanged)
    }

    fun cancelExport() {
        stopProgressPolling()
        try {
            activeTransformer?.cancel()
        } catch (_: Exception) {
        }
        activeTransformer = null
    }

    private fun startProgressPolling(
        transformer: Transformer,
        onStateChanged: (ExportState) -> Unit
    ) {
        val progressHolder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                if (activeTransformer !== transformer) return
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onStateChanged(ExportState.Running(progressHolder.progress.coerceIn(0, 99)))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 180L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 180L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun saveTempFileToGallery(
        tempFile: File,
        title: String,
        onSaved: (Uri, String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val safeName = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "HDR_Master" }
                val displayName = "${safeName}_HDR_${System.currentTimeMillis()}.mp4"
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/HDR Studios"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val galleryUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not create MediaStore entry")

                resolver.openOutputStream(galleryUri)?.use { out ->
                    FileInputStream(tempFile).use { input ->
                        input.copyTo(out, bufferSize = 65_536)
                    }
                } ?: throw IllegalStateException("Failed to open Gallery output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(galleryUri, doneValues, null, null)
                tempFile.delete()

                mainHandler.post { onSaved(galleryUri, displayName) }
            } catch (e: Exception) {
                tempFile.delete()
                mainHandler.post {
                    onError(e.localizedMessage ?: "Failed to save video to Gallery")
                }
            }
        }.start()
    }

    private fun postCompletionNotification(displayName: String) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "hdr_studios_exports"
        val channel = NotificationChannel(
            channelId,
            "HDR Exports",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dolby_emblem)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Saved $displayName to Movies / HDR Studios")
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
    }
}
