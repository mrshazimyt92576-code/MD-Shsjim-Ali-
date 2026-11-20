package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeviceCodecProfile
import com.example.data.model.ExportFps
import com.example.data.model.ExportResolution
import com.example.data.model.HdrGradingParams
import com.example.data.model.HdrProject
import com.example.data.model.ProjectStatus
import com.example.data.repository.StudioRepository
import com.example.media.CodecCapabilityChecker
import com.example.media.ExportState
import com.example.media.HdrShaderController
import com.example.media.HdrShaderRenderState
import com.example.media.VideoExportManager
import com.example.media.VideoMetadataHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: StudioRepository,
    initialProject: HdrProject,
    val deviceCodecProfile: DeviceCodecProfile
) : ViewModel() {

    private val _project = MutableStateFlow(initialProject)
    val project: StateFlow<HdrProject> = _project.asStateFlow()

    private val _grading = MutableStateFlow(initialProject.toGradingParams())
    val grading: StateFlow<HdrGradingParams> = _grading.asStateFlow()

    private val _splitPosition = MutableStateFlow(0.5f)
    val splitPosition: StateFlow<Float> = _splitPosition.asStateFlow()

    private val _splitEnabled = MutableStateFlow(true)
    val splitEnabled: StateFlow<Boolean> = _splitEnabled.asStateFlow()

    private val _hdrEnhanced = MutableStateFlow(true)
    val hdrEnhanced: StateFlow<Boolean> = _hdrEnhanced.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(initialProject.durationMs.coerceAtLeast(1L))
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _exportResolution = MutableStateFlow(ExportResolution.fromRaw(initialProject.lastExportResolution))
    val exportResolution: StateFlow<ExportResolution> = _exportResolution.asStateFlow()

    private val _exportFps = MutableStateFlow(ExportFps.fromValue(initialProject.lastExportFps))
    val exportFps: StateFlow<ExportFps> = _exportFps.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _isAnalyzingBalance = MutableStateFlow(false)
    val isAnalyzingBalance: StateFlow<Boolean> = _isAnalyzingBalance.asStateFlow()

    val shaderController = HdrShaderController(
        HdrShaderRenderState(
            params = initialProject.toGradingParams(),
            splitPosition = 0.5f,
            splitEnabled = true,
            hdrEnhanced = true
        )
    )

    private var exportManager: VideoExportManager? = null
    private var saveDebounceJob: Job? = null

    fun updateGrading(transform: (HdrGradingParams) -> HdrGradingParams) {
        val next = transform(_grading.value)
        _grading.value = next
        if (!_hdrEnhanced.value) {
            _hdrEnhanced.value = true
        }
        syncShader()
        schedulePersist(next)
    }

    fun applyPreset(preset: HdrGradingParams) {
        _grading.value = preset
        if (!_hdrEnhanced.value) {
            _hdrEnhanced.value = true
        }
        syncShader()
        schedulePersist(preset)
    }

    fun resetGrading() {
        applyPreset(HdrGradingParams.Neutral)
    }

    fun runAutoBalance(context: Context) {
        val uriStr = _project.value.sourceUri
        if (uriStr.isBlank() || _isAnalyzingBalance.value) return
        _isAnalyzingBalance.value = true
        viewModelScope.launch {
            try {
                val analyzed = VideoMetadataHelper.computeAutoBalance(
                    context = context,
                    uri = Uri.parse(uriStr),
                    positionMs = _positionMs.value
                )
                applyPreset(analyzed)
            } finally {
                _isAnalyzingBalance.value = false
            }
        }
    }

    fun updateSplitPosition(pos: Float) {
        _splitPosition.value = pos.coerceIn(0.05f, 0.95f)
        syncShader()
    }

    fun toggleSplitEnabled() {
        if (!_hdrEnhanced.value) {
            _hdrEnhanced.value = true
            _splitEnabled.value = true
        } else {
            _splitEnabled.value = !_splitEnabled.value
        }
        syncShader()
    }

    fun setHdrEnhanced(enabled: Boolean) {
        if (_hdrEnhanced.value == enabled) return
        _hdrEnhanced.value = enabled
        syncShader()
    }

    fun toggleHdrEnhanced() {
        setHdrEnhanced(!_hdrEnhanced.value)
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updatePlaybackPosition(posMs: Long, durMs: Long) {
        _positionMs.value = posMs.coerceAtLeast(0L)
        if (durMs > 0L) _durationMs.value = durMs
    }

    fun selectResolution(resolution: ExportResolution) {
        _exportResolution.value = resolution
    }

    fun selectFps(fps: ExportFps) {
        _exportFps.value = fps
    }

    fun checkHardwareCompatibility(resolution: ExportResolution, fpsOption: ExportFps): String? {
        val p = _project.value
        val (targetW, targetH) = CodecCapabilityChecker.computeTargetDimensions(
            p.width,
            p.height,
            resolution.maxHeight
        )
        val requestedFps = if (fpsOption == ExportFps.SOURCE) p.fps else fpsOption.value
        if (!deviceCodecProfile.canEncode(targetW, targetH, requestedFps)) {
            val maxSafeFps = if (maxOf(targetW, targetH) >= 3840) deviceCodecProfile.maxFps4k else deviceCodecProfile.maxFps1080p
            return "${targetW}×${targetH} @ ${requestedFps}fps exceeds encoder limit (auto-guarded to ≤${maxSafeFps}fps)"
        }
        return null
    }

    fun startExport(context: Context) {
        val p = _project.value
        if (p.sourceUri.isBlank()) return
        val appCtx = context.applicationContext
        val manager = exportManager ?: VideoExportManager(appCtx).also { exportManager = it }

        manager.startExport(
            sourceUri = Uri.parse(p.sourceUri),
            sourceWidth = p.width,
            sourceHeight = p.height,
            sourceFps = p.fps,
            grading = _grading.value,
            resolution = _exportResolution.value,
            fpsOption = _exportFps.value,
            title = p.title,
            onStateChanged = { state ->
                _exportState.value = state
                if (state is ExportState.Completed) {
                    viewModelScope.launch {
                        repository.recordProjectExport(
                            projectId = p.id,
                            status = ProjectStatus.EXPORTED,
                            resolution = _exportResolution.value,
                            fps = _exportFps.value.value,
                            exportUri = state.galleryUri.toString()
                        )
                    }
                } else if (state is ExportState.Failed) {
                    viewModelScope.launch {
                        repository.recordProjectExport(
                            projectId = p.id,
                            status = ProjectStatus.FAILED,
                            resolution = _exportResolution.value,
                            fps = _exportFps.value.value,
                            exportUri = null
                        )
                    }
                }
            }
        )
    }

    fun cancelExport() {
        exportManager?.cancelExport()
        _exportState.value = ExportState.Cancelled
    }

    fun dismissExportState() {
        _exportState.value = ExportState.Idle
    }

    private fun syncShader() {
        shaderController.update(
            params = _grading.value,
            splitPosition = _splitPosition.value,
            splitEnabled = _splitEnabled.value,
            hdrEnhanced = _hdrEnhanced.value
        )
    }

    private fun schedulePersist(params: HdrGradingParams) {
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(450L)
            repository.updateProjectGrading(
                projectId = _project.value.id,
                grading = params,
                status = ProjectStatus.EDITING
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        exportManager?.cancelExport()
    }

    companion object {
        fun factory(
            context: Context,
            project: HdrProject
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = context.applicationContext
                return EditorViewModel(
                    repository = StudioRepository(appCtx),
                    initialProject = project,
                    deviceCodecProfile = CodecCapabilityChecker.inspectDevice(appCtx)
                ) as T
            }
        }
    }
}
