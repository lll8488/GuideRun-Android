package com.blindrunner.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunningRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RunningRecordEntity>)

    @Update
    suspend fun update(record: RunningRecordEntity)

    @Delete
    suspend fun delete(record: RunningRecordEntity)

    @Query("SELECT * FROM running_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<RunningRecordEntity>>

    @Query("SELECT * FROM running_records ORDER BY date DESC")
    suspend fun getAllRecordsRaw(): List<RunningRecordEntity>

    @Query("SELECT * FROM running_records WHERE id = :id")
    suspend fun getRecordById(id: Long): RunningRecordEntity?

    @Query("SELECT * FROM running_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<RunningRecordEntity>>

    @Query("SELECT * FROM running_records WHERE status = :status ORDER BY date DESC")
    fun getRecordsByStatus(status: String): Flow<List<RunningRecordEntity>>

    @Query("DELETE FROM running_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM running_records")
    suspend fun getRecordCount(): Int
}
