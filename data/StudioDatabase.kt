package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val aspectRatioName: String,
    val durationMs: Long,
    val updatedAtMs: Long,
    val thumbnailDrawableKey: String?,
    val thumbnailUri: String?,
    val documentJson: String
)

@Entity(tableName = "storyboard_scenes")
data class StoryboardEntity(
    @PrimaryKey val id: String,
    val orderIndex: Int,
    val title: String,
    val scriptNotes: String,
    val durationSec: Int,
    val cameraAngle: String,
    val lightingTag: String,
    val isFilmed: Boolean
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface StoryboardDao {
    @Query("SELECT * FROM storyboard_scenes ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<StoryboardEntity>>

    @Query("SELECT COUNT(*) FROM storyboard_scenes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StoryboardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<StoryboardEntity>)

    @Query("DELETE FROM storyboard_scenes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [ProjectEntity::class, StoryboardEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun storyboardDao(): StoryboardDao

    companion object {
        @Volatile
        private var instance: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "cutpulse_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
