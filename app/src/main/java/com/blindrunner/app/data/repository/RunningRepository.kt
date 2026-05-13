package com.blindrunner.app.data.repository

import com.blindrunner.app.data.local.dao.RunningRecordDao
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.data.remote.api.ApiService
import com.blindrunner.app.data.remote.model.CreatePostRequest
import com.blindrunner.app.data.remote.model.PostResponse
import com.blindrunner.app.domain.model.RunningRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RunningRepository(
    private val runningRecordDao: RunningRecordDao,
    private val apiService: ApiService
) {

    /** Get all records from local DB as a Flow. */
    fun getAllRecords(): Flow<List<RunningRecord>> {
        return runningRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /** Get records filtered by date range from local DB. */
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<RunningRecord>> {
        return runningRecordDao.getRecordsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Local-first strategy: read from DB first; if empty, fetch from network and cache.
     * Follows the Repository Pattern principle — single source of truth.
     */
    suspend fun getRecords(forceRefresh: Boolean = false): List<RunningRecord> {
        val localRecords = runningRecordDao.getRecordCount()
        return if (forceRefresh || localRecords == 0) {
            val posts = apiService.getPosts()
            val entities = posts.map { it.toEntity() }
            runningRecordDao.insertAll(entities)
            entities.map { it.toDomainModel() }
        } else {
            runningRecordDao.getAllRecordsRaw().map { it.toDomainModel() }
        }
    }

    /** Fetch records from the remote API and cache them in the local database. */
    suspend fun refreshRecordsFromNetwork(): Result<List<RunningRecord>> {
        return try {
            val posts = apiService.getPosts()
            val entities = posts.map { it.toEntity() }
            runningRecordDao.insertAll(entities)
            Result.success(entities.map { it.toDomainModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sync a single running record to the remote API and save locally. */
    suspend fun syncRecordToRemote(record: RunningRecord): Result<RunningRecord> {
        return try {
            val request = CreatePostRequest(
                title = "Run at ${record.location}",
                body = "Duration: ${record.durationMinutes}min, Distance: ${record.distanceKm}km",
                userId = 1
            )
            val response = apiService.createPost(request)
            val entity = record.toEntity().copy(remoteId = response.id)
            val id = runningRecordDao.insert(entity)
            Result.success(entity.copy(id = id).toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertLocal(record: RunningRecord): Long {
        return runningRecordDao.insert(record.toEntity())
    }

    suspend fun deleteById(id: Long) {
        runningRecordDao.deleteById(id)
    }

    suspend fun getRecordCount(): Int {
        return runningRecordDao.getRecordCount()
    }

    // ── Mapping helpers (testable) ──

    fun PostResponse.toEntity(): RunningRecordEntity {
        return RunningRecordEntity(
            date = "2026-05-${id % 30 + 1}".padStart(10, '0'),
            durationMinutes = (title.length * 2) % 90 + 30,
            location = title.take(50),
            distanceKm = ((body.length % 10) + 2).toFloat(),
            status = if (id % 3 == 0) "completed" else "pending",
            remoteId = id
        )
    }

    fun RunningRecordEntity.toDomainModel(): RunningRecord {
        return RunningRecord(
            id = id,
            date = date,
            durationMinutes = durationMinutes,
            location = location,
            distanceKm = distanceKm,
            status = status
        )
    }

    fun RunningRecord.toEntity(): RunningRecordEntity {
        return RunningRecordEntity(
            id = id,
            date = date,
            durationMinutes = durationMinutes,
            location = location,
            distanceKm = distanceKm,
            status = status
        )
    }

    // ── Test-accessible wrappers ──

    fun toEntity_forTest(post: PostResponse): RunningRecordEntity = post.toEntity()
    fun toDomainModel_forTest(entity: RunningRecordEntity): RunningRecord = entity.toDomainModel()
    fun toEntity_forTest(record: RunningRecord): RunningRecordEntity = record.toEntity()
}
