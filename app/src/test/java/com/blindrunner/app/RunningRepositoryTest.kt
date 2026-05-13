package com.blindrunner.app

import com.blindrunner.app.data.local.dao.RunningRecordDao
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.data.remote.api.ApiService
import com.blindrunner.app.data.remote.model.PostResponse
import com.blindrunner.app.data.repository.RunningRepository
import com.blindrunner.app.domain.model.RunningRecord
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunningRepositoryTest {

    private lateinit var repository: RunningRepository

    @Before
    fun setUp() {
        val mockDao = mockk<RunningRecordDao>(relaxed = true)
        val mockApi = mockk<ApiService>(relaxed = true)
        repository = RunningRepository(mockDao, mockApi)
    }

    /** Test 1: PostResponse → Entity mapping is correct. */
    @Test
    fun postResponseToEntity_mapsFieldsCorrectly() {
        val post = PostResponse(
            userId = 1,
            id = 42,
            title = "奥森公园晨跑",
            body = "陪跑绳引导模式，5公里，45分钟"
        )

        val entity = repository.toEntity_forTest(post)

        assertNotNull(entity.location)
        assertEquals(42, entity.remoteId)
        assertTrue(entity.durationMinutes in 30..150)
        assertTrue(entity.distanceKm >= 2.0f)
    }

    /** Test 2: Entity → Domain model preserves all fields. */
    @Test
    fun entityToDomainModel_preservesAllFields() {
        val entity = RunningRecordEntity(
            id = 1,
            date = "2026-05-13",
            durationMinutes = 45,
            location = "朝阳公园南门",
            distanceKm = 5.2f,
            status = "completed",
            remoteId = 100
        )

        val domain = repository.toDomainModel_forTest(entity)

        assertEquals(entity.id, domain.id)
        assertEquals(entity.date, domain.date)
        assertEquals(entity.durationMinutes, domain.durationMinutes)
        assertEquals(entity.location, domain.location)
        assertEquals(entity.distanceKm, domain.distanceKm)
        assertEquals(entity.status, domain.status)
    }

    /** Test 3: Domain model → Entity round-trip preserves data. */
    @Test
    fun domainModelToEntity_preservesAllFields() {
        val domain = RunningRecord(
            id = 5,
            date = "2026-05-12",
            durationMinutes = 60,
            location = "天河体育中心",
            distanceKm = 10.0f,
            status = "pending"
        )

        val entity = repository.toEntity_forTest(domain)

        assertEquals(domain.id, entity.id)
        assertEquals(domain.date, entity.date)
        assertEquals(domain.durationMinutes, entity.durationMinutes)
        assertEquals(domain.location, entity.location)
        assertEquals(domain.distanceKm, entity.distanceKm)
        assertEquals(domain.status, entity.status)
    }
}
