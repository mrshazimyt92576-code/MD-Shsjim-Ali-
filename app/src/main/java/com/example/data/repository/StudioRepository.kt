package com.example.data.repository

import android.content.Context
import com.example.data.local.HdrProjectDao
import com.example.data.local.HdrProjectEntity
import com.example.data.local.HdrStudioDatabase
import com.example.data.model.ExportResolution
import com.example.data.model.HdrGradingParams
import com.example.data.model.HdrProject
import com.example.data.model.ProjectStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class StudioRepository(private val dao: HdrProjectDao) {
    constructor(context: Context) : this(HdrStudioDatabase.getInstance(context).projectDao())

    fun observeProjects(): Flow<List<HdrProject>> =
        dao.observeProjects().map { entities -> entities.map { it.toDomain() } }

    suspend fun createProject(
        title: String,
        sourceUri: String,
        width: Int,
        height: Int,
        fps: Int,
        durationMs: Long,
        grading: HdrGradingParams = HdrGradingParams.DolbyCinema
    ): Result<HdrProject> {
        return try {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val safeTitle = title.trim().ifBlank { "HDR Clip" }.take(120)
            val safeUri = sourceUri.take(500)
            val entity = HdrProjectEntity(
                id = id,
                title = safeTitle,
                sourceUri = safeUri,
                width = width.coerceIn(1, 8192),
                height = height.coerceIn(1, 8192),
                fps = fps.coerceIn(1, 240),
                durationMs = durationMs.coerceIn(0L, 86_400_000L),
                status = ProjectStatus.EDITING.name,
                presetName = grading.presetName.take(40),
                highlightRecovery = grading.highlightRecovery.toDouble().coerceIn(0.0, 1.0),
                shadowLift = grading.shadowLift.toDouble().coerceIn(0.0, 1.0),
                dynamicContrast = grading.dynamicContrast.toDouble().coerceIn(0.0, 1.0),
                saturationVibrance = grading.saturationVibrance.toDouble().coerceIn(0.0, 1.0),
                brightnessExposure = grading.brightnessExposure.toDouble().coerceIn(-1.0, 1.0),
                overallIntensity = grading.overallIntensity.toDouble().coerceIn(0.0, 1.0),
                highlightEnabled = grading.highlightEnabled,
                shadowEnabled = grading.shadowEnabled,
                contrastEnabled = grading.contrastEnabled,
                saturationEnabled = grading.saturationEnabled,
                brightnessEnabled = grading.brightnessEnabled,
                intensityEnabled = grading.intensityEnabled,
                lastExportResolution = ExportResolution.SOURCE.name,
                lastExportFps = 0,
                lastExportUri = null,
                createdAtMs = now,
                updatedAtMs = now
            )
            dao.insertProject(entity)
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectGrading(
        projectId: String,
        grading: HdrGradingParams,
        status: ProjectStatus = ProjectStatus.EDITING
    ): Result<Unit> {
        return try {
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            dao.updateProjectGrading(
                projectId = projectId,
                status = status.name,
                presetName = grading.presetName.take(40),
                highlightRecovery = grading.highlightRecovery.toDouble().coerceIn(0.0, 1.0),
                shadowLift = grading.shadowLift.toDouble().coerceIn(0.0, 1.0),
                dynamicContrast = grading.dynamicContrast.toDouble().coerceIn(0.0, 1.0),
                saturationVibrance = grading.saturationVibrance.toDouble().coerceIn(0.0, 1.0),
                brightnessExposure = grading.brightnessExposure.toDouble().coerceIn(-1.0, 1.0),
                overallIntensity = grading.overallIntensity.toDouble().coerceIn(0.0, 1.0),
                highlightEnabled = grading.highlightEnabled,
                shadowEnabled = grading.shadowEnabled,
                contrastEnabled = grading.contrastEnabled,
                saturationEnabled = grading.saturationEnabled,
                brightnessEnabled = grading.brightnessEnabled,
                intensityEnabled = grading.intensityEnabled,
                updatedAtMs = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordProjectExport(
        projectId: String,
        status: ProjectStatus,
        resolution: ExportResolution,
        fps: Int,
        exportUri: String?
    ): Result<Unit> {
        return try {
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            dao.recordProjectExport(
                projectId = projectId,
                status = status.name,
                lastExportResolution = resolution.name,
                lastExportFps = if (fps in setOf(0, 30, 60, 120)) fps else 0,
                lastExportUri = exportUri?.take(500),
                updatedAtMs = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            dao.deleteProject(projectId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
