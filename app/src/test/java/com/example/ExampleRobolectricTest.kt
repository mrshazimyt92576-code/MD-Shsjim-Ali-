package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HdrStudioDatabase
import com.example.data.model.ExportResolution
import com.example.data.model.HdrGradingParams
import com.example.data.model.ProjectStatus
import com.example.data.repository.StudioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appMetadata_matchesHdrStudiosVersion2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("HDR Studios", context.getString(R.string.app_name))
        assertEquals("2.0.0", BuildConfig.VERSION_NAME)
        assertEquals(10, BuildConfig.VERSION_CODE)
    }

    @Test
    fun roomStudioRepository_persistsUpdatesAndDeletesProjects() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HdrStudioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StudioRepository(db.projectDao())
            val created = repo.createProject(
                title = "Sunset Drone",
                sourceUri = "content://media/external/video/101",
                width = 3840,
                height = 2160,
                fps = 60,
                durationMs = 12_000L
            ).getOrThrow()

            var list = repo.observeProjects().first()
            assertEquals(1, list.size)
            assertEquals("Sunset Drone", list.first().title)

            repo.updateProjectGrading(created.id, HdrGradingParams.VividHdr, ProjectStatus.EDITING).getOrThrow()
            repo.recordProjectExport(
                projectId = created.id,
                status = ProjectStatus.EXPORTED,
                resolution = ExportResolution.UHD_4K,
                fps = 60,
                exportUri = "content://media/external/video/202"
            ).getOrThrow()

            list = repo.observeProjects().first()
            assertEquals(ProjectStatus.EXPORTED, list.first().projectStatus)
            assertEquals("Vivid HDR", list.first().presetName)
            assertEquals(60, list.first().lastExportFps)

            repo.deleteProject(created.id).getOrThrow()
            list = repo.observeProjects().first()
            assertTrue(list.isEmpty())
        } finally {
            db.close()
        }
    }
}
