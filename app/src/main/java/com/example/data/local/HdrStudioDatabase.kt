package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ExportResolution
import com.example.data.model.HdrProject
import com.example.data.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "hdr_projects")
data class HdrProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUri: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMs: Long,
    val status: String,
    val presetName: String,
    val highlightRecovery: Double,
    val shadowLift: Double,
    val dynamicContrast: Double,
    val saturationVibrance: Double,
    val brightnessExposure: Double,
    val overallIntensity: Double,
    val highlightEnabled: Boolean,
    val shadowEnabled: Boolean,
    val contrastEnabled: Boolean,
    val saturationEnabled: Boolean,
    val brightnessEnabled: Boolean,
    val intensityEnabled: Boolean,
    val lastExportResolution: String,
    val lastExportFps: Int,
    val lastExportUri: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun toDomain(): HdrProject = HdrProject(
        id = id,
        title = title,
        sourceUri = sourceUri,
        width = width,
        height = height,
        fps = fps,
        durationMs = durationMs,
        status = status,
        presetName = presetName,
        highlightRecovery = highlightRecovery,
        shadowLift = shadowLift,
        dynamicContrast = dynamicContrast,
        saturationVibrance = saturationVibrance,
        brightnessExposure = brightnessExposure,
        overallIntensity = overallIntensity,
        highlightEnabled = highlightEnabled,
        shadowEnabled = shadowEnabled,
        contrastEnabled = contrastEnabled,
        saturationEnabled = saturationEnabled,
        brightnessEnabled = brightnessEnabled,
        intensityEnabled = intensityEnabled,
        lastExportResolution = lastExportResolution,
        lastExportFps = lastExportFps,
        lastExportUri = lastExportUri,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

@Dao
interface HdrProjectDao {
    @Query("SELECT * FROM hdr_projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<HdrProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: HdrProjectEntity)

    @Query(
        """
        UPDATE hdr_projects SET
            status = :status,
            presetName = :presetName,
            highlightRecovery = :highlightRecovery,
            shadowLift = :shadowLift,
            dynamicContrast = :dynamicContrast,
            saturationVibrance = :saturationVibrance,
            brightnessExposure = :brightnessExposure,
            overallIntensity = :overallIntensity,
            highlightEnabled = :highlightEnabled,
            shadowEnabled = :shadowEnabled,
            contrastEnabled = :contrastEnabled,
            saturationEnabled = :saturationEnabled,
            brightnessEnabled = :brightnessEnabled,
            intensityEnabled = :intensityEnabled,
            updatedAtMs = :updatedAtMs
        WHERE id = :projectId
        """
    )
    suspend fun updateProjectGrading(
        projectId: String,
        status: String,
        presetName: String,
        highlightRecovery: Double,
        shadowLift: Double,
        dynamicContrast: Double,
        saturationVibrance: Double,
        brightnessExposure: Double,
        overallIntensity: Double,
        highlightEnabled: Boolean,
        shadowEnabled: Boolean,
        contrastEnabled: Boolean,
        saturationEnabled: Boolean,
        brightnessEnabled: Boolean,
        intensityEnabled: Boolean,
        updatedAtMs: Long
    )

    @Query(
        """
        UPDATE hdr_projects SET
            status = :status,
            lastExportResolution = :lastExportResolution,
            lastExportFps = :lastExportFps,
            lastExportUri = COALESCE(:lastExportUri, lastExportUri),
            updatedAtMs = :updatedAtMs
        WHERE id = :projectId
        """
    )
    suspend fun recordProjectExport(
        projectId: String,
        status: String,
        lastExportResolution: String,
        lastExportFps: Int,
        lastExportUri: String?,
        updatedAtMs: Long
    )

    @Query("DELETE FROM hdr_projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)
}

@Database(
    entities = [HdrProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HdrStudioDatabase : RoomDatabase() {
    abstract fun projectDao(): HdrProjectDao

    companion object {
        @Volatile
        private var INSTANCE: HdrStudioDatabase? = null

        fun getInstance(context: Context): HdrStudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HdrStudioDatabase::class.java,
                    "hdr_studios_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
