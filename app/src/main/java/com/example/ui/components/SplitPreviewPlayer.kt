package com.example.ui.components

import android.net.Uri
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.HdrGradingParams
import com.example.localization.LocalStrings
import com.example.media.HdrShaderController
import com.example.media.HdrToneGlEffect
import com.example.ui.theme.LocalLiquidGlassColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun SplitPreviewPlayer(
    sourceUri: String,
    shaderController: HdrShaderController,
    grading: HdrGradingParams,
    splitPosition: Float,
    splitEnabled: Boolean,
    hdrEnhanced: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    seekRequestMs: Long?,
    onSeekConsumed: () -> Unit,
    onSplitChanged: (Float) -> Unit,
    onPlaybackProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onPlayingStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current
    val currentSplit by rememberUpdatedState(splitPosition)
    val onSplitChangedLatest by rememberUpdatedState(onSplitChanged)

    val exoPlayer = remember(sourceUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            setVideoEffects(listOf(HdrToneGlEffect(shaderController)))
            setMediaItem(MediaItem.fromUri(Uri.parse(sourceUri)))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(grading, splitPosition, splitEnabled, hdrEnhanced, isPlaying) {
        if (!isPlaying && exoPlayer.playbackState == Player.STATE_READY) {
            delay(40L)
            exoPlayer.seekTo(exoPlayer.currentPosition)
        }
    }

    LaunchedEffect(seekRequestMs) {
        if (seekRequestMs != null) {
            exoPlayer.seekTo(seekRequestMs)
            onSeekConsumed()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            onPlaybackProgress(pos, dur)
            delay(150L)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                onPlayingStateChanged(playing)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(1.dp, glass.borderHighlight.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    exoPlayer.setVideoSurfaceView(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!hdrEnhanced) {
            // Standard Viewing Mode indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.standardBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        } else if (!splitEnabled) {
            // Full HDR-Enhanced Viewing Mode indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(glass.accentCyan.copy(alpha = 0.30f))
                    .border(1.dp, glass.accentCyan.copy(alpha = 0.75f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.hdrEnhancedBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        } else {
            val handleOffsetX = (widthPx * splitPosition).roundToInt()

            // Floating Before / After Pills
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.beforeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(glass.accentCyan.copy(alpha = 0.28f))
                    .border(1.dp, glass.accentCyan.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.afterLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // Draggable vertical optical divider + circular glass knob
            Box(
                modifier = Modifier
                    .offset { IntOffset(handleOffsetX - with(density) { 22.dp.roundToPx() }, 0) }
                    .width(44.dp)
                    .fillMaxHeight()
                    .pointerInput(widthPx) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val currentPx = currentSplit * widthPx
                            val nextRatio = ((currentPx + dragAmount) / widthPx).coerceIn(0.05f, 0.95f)
                            onSplitChangedLatest(nextRatio)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.5.dp, glass.accentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = "Drag Before After Split",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
