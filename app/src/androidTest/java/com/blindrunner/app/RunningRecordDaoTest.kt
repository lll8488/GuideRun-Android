package com.blindrunner.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.blindrunner.app.data.local.AppDatabase
import com.blindrunner.app.data.local.dao.RunningRecordDao
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunningRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RunningRecordDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = database.runningRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryRecord() = runTest {
        val record = RunningRecordEntity(
            date = "2026-05-13",
            durationMinutes = 45,
            location = "朝阳公园南门",
            distanceKm = 5.2f,
            status = "completed"
        )

        val insertedId = dao.insert(record)
        val retrieved = dao.getRecordById(insertedId)

        assertNotNull(retrieved)
        assertEquals("2026-05-13", retrieved!!.date)
        assertEquals(45, retrieved.durationMinutes)
        assertEquals("朝阳公园南门", retrieved.location)
        assertEquals(5.2f, retrieved.distanceKm)
        assertEquals("completed", retrieved.status)
    }

    @Test
    fun insertAndDeleteRecord() = runTest {
        val record = RunningRecordEntity(
            date = "2026-05-14",
            durationMinutes = 60,
            location = "奥森公园北门",
            distanceKm = 8.0f,
            status = "pending"
        )
        val insertedId = dao.insert(record)
        assertNotNull(dao.getRecordById(insertedId))

        dao.deleteById(insertedId)

        assertNull(dao.getRecordById(insertedId))
    }

    @Test
    fun queryByStatus_returnsOnlyMatchingRecords() = runTest {
        dao.insertAll(
            listOf(
                RunningRecordEntity(
                    date = "2026-05-10", durationMinutes = 30,
                    location = "地点A", distanceKm = 3.0f, status = "completed"
                ),
                RunningRecordEntity(
                    date = "2026-05-11", durationMinutes = 45,
                    location = "地点B", distanceKm = 5.0f, status = "pending"
                ),
                RunningRecordEntity(
                    date = "2026-05-12", durationMinutes = 60,
                    location = "地点C", distanceKm = 7.0f, status = "completed"
                )
            )
        )

        val completedRecords = dao.getRecordsByStatus("completed").first()

        assertEquals(2, completedRecords.size)
        assertTrue(completedRecords.all { it.status == "completed" })
    }

    @Test
    fun queryByDateRange_returnsFilteredRecords() = runTest {
        dao.insert(
            RunningRecordEntity(
                date = "2026-05-10", durationMinutes = 30,
                location = "A", distanceKm = 3.0f, status = "completed"
            )
        )
        dao.insert(
            RunningRecordEntity(
                date = "2026-05-12", durationMinutes = 45,
                location = "B", distanceKm = 5.0f, status = "completed"
            )
        )
        dao.insert(
            RunningRecordEntity(
                date = "2026-05-15", durationMinutes = 60,
                location = "C", distanceKm = 8.0f, status = "completed"
            )
        )

        val filtered = dao.getRecordsByDateRange("2026-05-09", "2026-05-13").first()

        assertEquals(2, filtered.size)
        assertEquals("2026-05-10", filtered[0].date)
        assertEquals("2026-05-12", filtered[1].date)
    }
}
