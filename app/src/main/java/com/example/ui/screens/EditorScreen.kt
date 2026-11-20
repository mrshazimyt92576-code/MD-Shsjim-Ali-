package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HdrGradingParams
import com.example.localization.LocalStrings
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCapsuleButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SplitPreviewPlayer
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.viewmodel.EditorViewModel
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current

    val project by viewModel.project.collectAsStateWithLifecycle()
    val grading by viewModel.grading.collectAsStateWithLifecycle()
    val splitPosition by viewModel.splitPosition.collectAsStateWithLifecycle()
    val splitEnabled by viewModel.splitEnabled.collectAsStateWithLifecycle()
    val hdrEnhanced by viewModel.hdrEnhanced.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingBalance.collectAsStateWithLifecycle()

    var seekRequestMs by remember { mutableStateOf<Long?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }

    LiquidGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Top Glass Toolbar
                LiquidGlassCard(
                    shape = CircleShape,
                    padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("editor_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = glass.textPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${project.width}×${project.height} • ${project.fps} FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.accentCyan
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleSplitEnabled() },
                                modifier = Modifier.testTag("toggle_split_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Compare,
                                    contentDescription = "Toggle Before After Split",
                                    tint = if (hdrEnhanced && splitEnabled) glass.accentCyan else glass.textSecondary
                                )
                            }
                            LiquidGlassCapsuleButton(
                                onClick = { showExportSheet = true },
                                accented = true,
                                height = 40.dp,
                                horizontalPadding = 14.dp,
                                modifier = Modifier.testTag("open_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileUpload,
                                    contentDescription = strings.exportVideo,
                                    tint = Color(0xFF050912),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.exportVideo,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050912)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Split Before / After Video Viewport
                SplitPreviewPlayer(
                    sourceUri = project.sourceUri,
                    shaderController = viewModel.shaderController,
                    grading = grading,
                    splitPosition = splitPosition,
                    splitEnabled = splitEnabled,
                    hdrEnhanced = hdrEnhanced,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    seekRequestMs = seekRequestMs,
                    onSeekConsumed = { seekRequestMs = null },
                    onSplitChanged = { viewModel.updateSplitPosition(it) },
                    onPlaybackProgress = { pos, dur -> viewModel.updatePlaybackPosition(pos, dur) },
                    onPlayingStateChanged = { viewModel.setPlaying(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Standard vs HDR-Enhanced Viewing Mode Segmented Toggle
                LiquidGlassCard(
                    shape = CircleShape,
                    padding = PaddingValues(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGlassCapsuleButton(
                            onClick = { viewModel.setHdrEnhanced(false) },
                            accented = !hdrEnhanced,
                            height = 40.dp,
                            horizontalPadding = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewing_mode_standard")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = strings.viewModeStandard,
                                tint = if (!hdrEnhanced) Color(0xFF050912) else glass.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.viewModeStandard,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!hdrEnhanced) FontWeight.Bold else FontWeight.Medium,
                                color = if (!hdrEnhanced) Color(0xFF050912) else glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        LiquidGlassCapsuleButton(
                            onClick = { viewModel.setHdrEnhanced(true) },
                            accented = hdrEnhanced,
                            height = 40.dp,
                            horizontalPadding = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewing_mode_hdr")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = strings.viewModeHdr,
                                tint = if (hdrEnhanced) Color(0xFF050912) else glass.accentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.viewModeHdr,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (hdrEnhanced) FontWeight.Bold else FontWeight.Medium,
                                color = if (hdrEnhanced) Color(0xFF050912) else glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback, Timeline Scrubber & Audio Control Bar
                LiquidGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.setPlaying(!isPlaying) },
                                modifier = Modifier.testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = glass.accentCyan
                                )
                            }

                            Slider(
                                value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                                onValueChange = { seekRequestMs = it.toLong() },
                                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = glass.accentCyan,
                                    activeTrackColor = glass.accentCyan,
                                    inactiveTrackColor = glass.glassFillStrong
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timeline_scrubber")
                            )

                            IconButton(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier.testTag("mute_audio_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (isMuted) strings.unmuteAudio else strings.muteAudio,
                                    tint = if (isMuted) glass.accentCoral else glass.textPrimary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimecode(positionMs)} / ${formatTimecode(durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                            Text(
                                text = strings.audioIntactNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Grading Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Capsule Actions: Reset + Auto Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassCapsuleButton(
                            onClick = { viewModel.resetGrading() },
                            height = 44.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_grading_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = strings.reset,
                                tint = glass.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.reset,
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textPrimary
                            )
                        }

                        LiquidGlassCapsuleButton(
                            onClick = { viewModel.runAutoBalance(context) },
                            enabled = !isAnalyzing,
                            accented = true,
                            height = 44.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_balance_button")
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF050912),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = strings.autoBalance,
                                    tint = Color(0xFF050912),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.autoBalance,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050912)
                            )
                        }
                    }

                    // Preset Capsule Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HdrGradingParams.Presets.forEach { preset ->
                            val active = grading.presetName == preset.presetName
                            LiquidGlassCapsuleButton(
                                onClick = { viewModel.applyPreset(preset) },
                                accented = active,
                                height = 38.dp,
                                horizontalPadding = 14.dp,
                                modifier = Modifier.testTag("preset_${preset.presetName}")
                            ) {
                                Text(
                                    text = preset.presetName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color(0xFF050912) else glass.textPrimary
                                )
                            }
                        }
                    }

                    // 6 Tone & Dynamic Range Sliders with Individual On/Off Toggles
                    GradingSliderCard(
                        label = strings.highlightRecovery,
                        value = grading.highlightRecovery,
                        range = 0f..1f,
                        enabled = grading.highlightEnabled,
                        displayPercent = (grading.highlightRecovery * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(highlightEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(highlightRecovery = v, presetName = "Custom") } },
                        testTag = "slider_highlight"
                    )

                    GradingSliderCard(
                        label = strings.shadowLift,
                        value = grading.shadowLift,
                        range = 0f..1f,
                        enabled = grading.shadowEnabled,
                        displayPercent = (grading.shadowLift * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(shadowEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(shadowLift = v, presetName = "Custom") } },
                        testTag = "slider_shadow"
                    )

                    GradingSliderCard(
                        label = strings.dynamicContrast,
                        value = grading.dynamicContrast,
                        range = 0f..1f,
                        enabled = grading.contrastEnabled,
                        displayPercent = (grading.dynamicContrast * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(contrastEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(dynamicContrast = v, presetName = "Custom") } },
                        testTag = "slider_contrast"
                    )

                    GradingSliderCard(
                        label = strings.saturationVibrance,
                        value = grading.saturationVibrance,
                        range = 0f..1f,
                        enabled = grading.saturationEnabled,
                        displayPercent = (grading.saturationVibrance * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(saturationEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(saturationVibrance = v, presetName = "Custom") } },
                        testTag = "slider_saturation"
                    )

                    GradingSliderCard(
                        label = strings.brightnessExposure,
                        value = grading.brightnessExposure,
                        range = -1f..1f,
                        enabled = grading.brightnessEnabled,
                        displayPercent = (grading.brightnessExposure * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(brightnessEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(brightnessExposure = v, presetName = "Custom") } },
                        testTag = "slider_brightness"
                    )

                    GradingSliderCard(
                        label = strings.overallIntensity,
                        value = grading.overallIntensity,
                        range = 0f..1f,
                        enabled = grading.intensityEnabled,
                        displayPercent = (grading.overallIntensity * 100).roundToInt(),
                        onToggle = { on -> viewModel.updateGrading { it.copy(intensityEnabled = on, presetName = "Custom") } },
                        onValueChange = { v -> viewModel.updateGrading { it.copy(overallIntensity = v, presetName = "Custom") } },
                        testTag = "slider_intensity"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showExportSheet) {
        ExportSheet(
            viewModel = viewModel,
            onDismiss = {
                viewModel.dismissExportState()
                showExportSheet = false
            }
        )
    }
}

@Composable
private fun GradingSliderCard(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    displayPercent: Int,
    onToggle: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val glass = LocalLiquidGlassColors.current
    LiquidGlassCard(
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) glass.textPrimary else glass.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(glass.glassFillStrong)
                            .border(1.dp, glass.borderSubtle, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (displayPercent > 0 && range.start < 0f) "+$displayPercent%" else "$displayPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) glass.accentCyan else glass.textSecondary
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF050912),
                            checkedTrackColor = glass.accentCyan,
                            uncheckedThumbColor = glass.textSecondary,
                            uncheckedTrackColor = glass.glassFillStrong
                        ),
                        modifier = Modifier.testTag("${testTag}_toggle")
                    )
                }
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = glass.accentCyan,
                    activeTrackColor = glass.accentCyan,
                    inactiveTrackColor = glass.glassFillStrong,
                    disabledThumbColor = glass.textSecondary,
                    disabledActiveTrackColor = glass.textSecondary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs)
}
