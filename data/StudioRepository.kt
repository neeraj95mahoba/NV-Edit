package com.example.data

import com.example.model.AspectRatioPreset
import com.example.model.StoryboardScene
import com.example.model.StudioCatalog
import com.example.model.VideoProjectDocument
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ProjectSummary(
    val id: String,
    val title: String,
    val aspectRatio: AspectRatioPreset,
    val durationMs: Long,
    val updatedAtMs: Long,
    val thumbnailDrawableKey: String?,
    val thumbnailUri: String?,
    val resolutionLabel: String = "4K",
    val fpsLabel: String = "60fps",
    val clipCount: Int = 1,
    val estimatedSizeMb: Float = 12.4f
)

class StudioRepository(
    private val projectDao: ProjectDao,
    private val storyboardDao: StoryboardDao
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val docAdapter = moshi.adapter(VideoProjectDocument::class.java)

    val projectsFlow: Flow<List<ProjectSummary>> = projectDao.observeAll().map { entities ->
        entities.map { entity ->
            val parsedDoc = runCatching { docAdapter.fromJson(entity.documentJson) }.getOrNull()
            val resLabel = parsedDoc?.resolution?.label ?: "4K"
            val fpsVal = parsedDoc?.fps?.fps ?: 60
            val durSec = (entity.durationMs / 1000f).coerceAtLeast(1f)
            val mbps = when (resLabel) {
                "4K" -> if (fpsVal >= 60) 32f else 22f
                "2K" -> if (fpsVal >= 60) 18f else 13f
                "1080p" -> if (fpsVal >= 60) 12f else 8f
                else -> 5f
            }
            val sizeMb = ((durSec * mbps) / 8f).coerceAtLeast(1.2f)
            ProjectSummary(
                id = entity.id,
                title = entity.title,
                aspectRatio = runCatching { AspectRatioPreset.valueOf(entity.aspectRatioName) }
                    .getOrDefault(AspectRatioPreset.PORTRAIT_9_16),
                durationMs = entity.durationMs,
                updatedAtMs = entity.updatedAtMs,
                thumbnailDrawableKey = entity.thumbnailDrawableKey,
                thumbnailUri = entity.thumbnailUri,
                resolutionLabel = resLabel,
                fpsLabel = parsedDoc?.fps?.label ?: "60fps",
                clipCount = parsedDoc?.clips?.size ?: 1,
                estimatedSizeMb = sizeMb
            )
        }
    }

    val storyboardsFlow: Flow<List<StoryboardScene>> = storyboardDao.observeAll().map { entities ->
        entities.map {
            StoryboardScene(
                id = it.id,
                orderIndex = it.orderIndex,
                title = it.title,
                scriptNotes = it.scriptNotes,
                durationSec = it.durationSec,
                cameraAngle = it.cameraAngle,
                lightingTag = it.lightingTag,
                isFilmed = it.isFilmed
            )
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (storyboardDao.count() == 0) {
            storyboardDao.upsertAll(
                StudioCatalog.defaultStoryboards.map { scene ->
                    StoryboardEntity(
                        id = scene.id,
                        orderIndex = scene.orderIndex,
                        title = scene.title,
                        scriptNotes = scene.scriptNotes,
                        durationSec = scene.durationSec,
                        cameraAngle = scene.cameraAngle,
                        lightingTag = scene.lightingTag,
                        isFilmed = scene.isFilmed
                    )
                }
            )
        }
    }

    suspend fun saveProject(
        document: VideoProjectDocument,
        preserveTimestamp: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val stamped = if (preserveTimestamp) document else document.copy(updatedAtMs = System.currentTimeMillis())
        val json = docAdapter.toJson(stamped)
        val firstMediaClip = stamped.clips.firstOrNull { !it.mediaUri.isNullOrBlank() }
        val entity = ProjectEntity(
            id = stamped.id,
            title = stamped.title,
            aspectRatioName = stamped.aspectRatio.name,
            durationMs = stamped.totalDurationMs,
            updatedAtMs = stamped.updatedAtMs,
            thumbnailDrawableKey = stamped.thumbnailDrawableKey,
            thumbnailUri = stamped.thumbnailUri ?: firstMediaClip?.mediaUri,
            documentJson = json
        )
        projectDao.upsert(entity)
    }

    suspend fun loadProject(id: String): VideoProjectDocument? = withContext(Dispatchers.IO) {
        val entity = projectDao.getById(id) ?: return@withContext null
        runCatching { docAdapter.fromJson(entity.documentJson) }.getOrNull()
    }

    suspend fun renameProject(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = loadProject(id) ?: return@withContext
        val cleanTitle = newTitle.trim().ifBlank { existing.title }
        saveProject(existing.copy(title = cleanTitle))
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteById(id)
    }

    suspend fun duplicateProject(id: String): VideoProjectDocument? = withContext(Dispatchers.IO) {
        val original = loadProject(id) ?: return@withContext null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${original.title} Copy",
            updatedAtMs = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    suspend fun upsertStoryboard(scene: StoryboardScene) = withContext(Dispatchers.IO) {
        storyboardDao.upsert(
            StoryboardEntity(
                id = scene.id,
                orderIndex = scene.orderIndex,
                title = scene.title,
                scriptNotes = scene.scriptNotes,
                durationSec = scene.durationSec,
                cameraAngle = scene.cameraAngle,
                lightingTag = scene.lightingTag,
                isFilmed = scene.isFilmed
            )
        )
    }

    suspend fun saveStoryboardOrder(scenes: List<StoryboardScene>) = withContext(Dispatchers.IO) {
        storyboardDao.upsertAll(
            scenes.mapIndexed { idx, scene ->
                StoryboardEntity(
                    id = scene.id,
                    orderIndex = idx,
                    title = scene.title,
                    scriptNotes = scene.scriptNotes,
                    durationSec = scene.durationSec,
                    cameraAngle = scene.cameraAngle,
                    lightingTag = scene.lightingTag,
                    isFilmed = scene.isFilmed
                )
            }
        )
    }

    suspend fun deleteStoryboard(id: String) = withContext(Dispatchers.IO) {
        storyboardDao.deleteById(id)
    }
}
