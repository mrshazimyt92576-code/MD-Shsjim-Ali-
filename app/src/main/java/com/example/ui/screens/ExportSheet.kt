package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExportFps
import com.example.data.model.ExportResolution
import com.example.localization.LocalStrings
import com.example.media.ExportState
import com.example.ui.components.LiquidGlassCapsuleButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val resolution by viewModel.exportResolution.collectAsStateWithLifecycle()
    val fpsOption by viewModel.exportFps.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startExport(context)
    }

    val hardwareGuardWarning = viewModel.checkHardwareCompatibility(resolution, fpsOption)

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportState.Running) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = glass.backdropBase.copy(alpha = 0.94f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.exportSettingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
                if (exportState !is ExportState.Running) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.cancel,
                            tint = glass.textSecondary
                        )
                    }
                }
            }

            when (val state = exportState) {
                is ExportState.Running -> {
                    LiquidGlassCard(strong = true) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.exportingTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "${state.progressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.accentCyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                color = glass.accentCyan,
                                trackColor = glass.glassFillStrong,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                            LiquidGlassCapsuleButton(
                                onClick = { viewModel.cancelExport() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cancel_export_button")
                            ) {
                                Text(
                                    text = strings.cancelExport,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.accentCoral
                                )
                            }
                        }
                    }
                }

                is ExportState.Completed -> {
                    LiquidGlassCard(strong = true) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = strings.exportCompleteTitle,
                                tint = glass.accentEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = strings.exportCompleteTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = strings.savedToGalleryMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiquidGlassCapsuleButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, state.galleryUri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "HDR Studios Master: ${state.displayName} (${state.galleryUri})"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, strings.sharePreviewLink)
                                        )
                                    },
                                    accented = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_export_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = strings.sharePreviewLink,
                                        tint = Color(0xFF050912),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.sharePreviewLink,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050912)
                                    )
                                }

                                LiquidGlassCapsuleButton(
                                    onClick = {
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(state.galleryUri, "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(viewIntent)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    modifier = Modifier.testTag("open_gallery_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInNew,
                                        contentDescription = strings.openGallery,
                                        tint = glass.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            LiquidGlassCapsuleButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.done,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }
                }

                else -> {
                    if (state is ExportState.Failed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(glass.accentCoral.copy(alpha = 0.16f))
                                .border(1.dp, glass.accentCoral.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.accentCoral
                            )
                        }
                    }

                    // Resolution Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.resolutionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ExportResolution.SOURCE to strings.resSource,
                                ExportResolution.FHD_1080P to strings.res1080p,
                                ExportResolution.UHD_4K to strings.res4k
                            ).forEach { (res, label) ->
                                val active = resolution == res
                                LiquidGlassCapsuleButton(
                                    onClick = { viewModel.selectResolution(res) },
                                    accented = active,
                                    height = 42.dp,
                                    horizontalPadding = 12.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_res_${res.name}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) Color(0xFF050912) else glass.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Frame Rate Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.frameRateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ExportFps.SOURCE to strings.fpsSource,
                                ExportFps.FPS_30 to "30",
                                ExportFps.FPS_60 to "60",
                                ExportFps.FPS_120 to "120"
                            ).forEach { (fps, label) ->
                                val active = fpsOption == fps
                                LiquidGlassCapsuleButton(
                                    onClick = { viewModel.selectFps(fps) },
                                    accented = active,
                                    height = 42.dp,
                                    horizontalPadding = 10.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_fps_${fps.value}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) Color(0xFF050912) else glass.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (hardwareGuardWarning != null) {
                        LiquidGlassCard(padding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = glass.accentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${strings.hardwareWarningPrefix} $hardwareGuardWarning",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.accentGold
                                )
                            }
                        }
                    }

                    LiquidGlassCapsuleButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.startExport(context)
                            } else {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        accented = true,
                        height = 54.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_export_button")
                    ) {
                        Text(
                            text = strings.startExport,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050912)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
