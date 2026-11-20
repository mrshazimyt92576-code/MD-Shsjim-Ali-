package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.model.DeviceCodecProfile
import com.example.data.model.HdrGradingParams
import com.example.data.model.HdrProject
import com.example.data.model.ProjectStatus
import com.example.data.model.ThemeMode
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.StudioRepository
import com.example.localization.AppLanguage
import com.example.media.CodecCapabilityChecker
import com.example.media.VideoMetadataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StudioViewModel(
    private val repository: StudioRepository,
    private val preferences: PreferencesRepository,
    val deviceCodecProfile: DeviceCodecProfile
) : ViewModel() {

    val projectsState: StateFlow<UiState<List<HdrProject>>> = repository.observeProjects()
        .map<List<HdrProject>, UiState<List<HdrProject>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing projects", error)
            emit(UiState.Error(error.message ?: "Failed to load projects"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun importVideoAndCreateProject(
        context: Context,
        uri: Uri,
        onProjectCreated: (HdrProject) -> Unit
    ) {
        if (_isImporting.value) return
        _isImporting.value = true
        viewModelScope.launch {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                val meta = VideoMetadataHelper.inspectVideo(context, uri)
                val initialGrading = HdrGradingParams.DolbyCinema
                val createResult = repository.createProject(
                    title = meta.title,
                    sourceUri = uri.toString(),
                    width = meta.width,
                    height = meta.height,
                    fps = meta.fps,
                    durationMs = meta.durationMs,
                    grading = initialGrading
                )
                createResult.onSuccess { project ->
                    VideoMetadataHelper.extractAndCacheThumbnail(context, uri, project.id)
                    onProjectCreated(project)
                }.onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to create project"
                }
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun resetProjectGrades(projectId: String) {
        viewModelScope.launch {
            val result = repository.updateProjectGrading(
                projectId = projectId,
                grading = HdrGradingParams.Neutral,
                status = ProjectStatus.READY
            )
            result.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Reset failed"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val result = repository.deleteProject(projectId)
            result.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferences.setLanguage(language)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    companion object {
        private const val TAG = "StudioVM"

        fun factory(
            context: Context,
            preferences: PreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val appCtx = context.applicationContext
                return StudioViewModel(
                    repository = StudioRepository(appCtx),
                    preferences = preferences,
                    deviceCodecProfile = CodecCapabilityChecker.inspectDevice(appCtx)
                ) as T
            }
        }
    }
}
