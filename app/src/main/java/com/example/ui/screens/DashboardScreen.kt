package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.HdrProject
import com.example.data.model.ProjectStatus
import com.example.localization.LocalStrings
import com.example.media.VideoMetadataHelper
import com.example.ui.components.DolbyVisionHeaderBadge
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCapsuleButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiState
import java.io.File

@Composable
fun DashboardScreen(
    viewModel: StudioViewModel,
    onOpenProject: (HdrProject) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingDeleteProject by remember { mutableStateOf<HdrProject?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importVideoAndCreateProject(context, uri, onOpenProject)
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            snackbarHostState.showSnackbar(statusBanner!!)
            viewModel.clearBanner()
        }
    }

    LiquidGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Frosted Glass Header with App Name + v2.0.0 Badge
                item {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
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
                                Image(
                                    painter = painterResource(id = R.drawable.img_hdr_icon_fg),
                                    contentDescription = strings.appTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, glass.borderHighlight, RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = strings.appTitle,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = glass.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(glass.accentCyan.copy(alpha = 0.20f))
                                                .border(1.dp, glass.accentCyan.copy(alpha = 0.6f), CircleShape)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = strings.versionBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glass.accentCyan
                                            )
                                        }
                                    }
                                    Text(
                                        text = strings.studioSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = strings.settingsTitle,
                                    tint = glass.textPrimary
                                )
                            }
                        }
                    }
                }

                // 2. Hero Capsule / Glass Button: "Select Video"
                item {
                    LiquidGlassCard(
                        strong = true,
                        shape = RoundedCornerShape(30.dp),
                        padding = PaddingValues(20.dp),
                        onClick = {
                            if (!isImporting) {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_video_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DolbyVisionHeaderBadge()
                            Spacer(modifier = Modifier.height(14.dp))
                            LiquidGlassCapsuleButton(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                enabled = !isImporting,
                                accented = true,
                                height = 56.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_video_button")
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color(0xFF050912),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AddCircle,
                                        contentDescription = strings.selectVideo,
                                        tint = Color(0xFF050912),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.selectVideo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF050912)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.selectVideoSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                    }
                }

                // 3. Hardware Performance & Codec Capability Indicator
                item {
                    val profile = viewModel.deviceCodecProfile
                    val resText = if (maxOf(profile.maxWidth, profile.maxHeight) >= 3840) "4K" else "1080p"
                    val fpsText = if (resText == "4K") "${profile.maxFps4k} FPS" else "${profile.maxFps1080p} FPS"
                    LiquidGlassCard(
                        shape = CircleShape,
                        padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = glass.accentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (profile.hevcSupported) strings.hevcReady else strings.deviceHardwareStatus,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                            }
                            Text(
                                text = "${strings.maxSupportedPrefix} $resText • $fpsText",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.accentCyan
                            )
                        }
                    }
                }

                // 4. Recent Projects Section Header
                item {
                    Text(
                        text = strings.recentProjects,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // 5. Recent Projects List
                when (val state = projectsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = glass.accentCyan)
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            LiquidGlassCard {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.accentCoral
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                LiquidGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(28.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VideoLibrary,
                                            contentDescription = null,
                                            tint = glass.accentCyan.copy(alpha = 0.7f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = strings.noProjectsTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = glass.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = strings.noProjectsSubtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.textSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.data, key = { it.id }) { project ->
                                RecentProjectCard(
                                    project = project,
                                    onOpen = {
                                        if (VideoMetadataHelper.isUriReadable(context, project.sourceUri)) {
                                            onOpenProject(project)
                                        } else {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        }
                                    },
                                    onReset = { viewModel.resetProjectGrades(project.id) },
                                    onDelete = { pendingDeleteProject = project }
                                )
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    AnimatedVisibility(
        visible = pendingDeleteProject != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        pendingDeleteProject?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDeleteProject = null },
                title = { Text(strings.deleteConfirmTitle) },
                text = { Text("${target.title}\n\n${strings.deleteConfirmBody}") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteProject(target.id)
                            pendingDeleteProject = null
                        }
                    ) {
                        Text(strings.deleteAction, color = glass.accentCoral)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteProject = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}

@Composable
private fun RecentProjectCard(
    project: HdrProject,
    onOpen: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current

    val thumbBitmap = remember(project.id) {
        val file = File(context.filesDir, "thumbnails/${project.id}.jpg")
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    val statusText = when (project.projectStatus) {
        ProjectStatus.READY -> strings.statusReady
        ProjectStatus.EDITING -> strings.statusEditing
        ProjectStatus.EXPORTED -> strings.statusExported
        ProjectStatus.FAILED -> strings.statusFailed
    }
    val statusColor = when (project.projectStatus) {
        ProjectStatus.EXPORTED -> glass.accentEmerald
        ProjectStatus.EDITING -> glass.accentCyan
        ProjectStatus.FAILED -> glass.accentCoral
        ProjectStatus.READY -> glass.accentGold
    }

    LiquidGlassCard(
        onClick = onOpen,
        padding = PaddingValues(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, glass.borderSubtle, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = project.title,
                        tint = glass.accentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${project.resolutionLabel} • ${project.fps} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = strings.resetAction,
                    tint = glass.textSecondary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = strings.deleteAction,
                    tint = glass.accentCoral
                )
            }
        }
    }
}
