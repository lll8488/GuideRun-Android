package com.example.guiderun.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.guiderun.data.model.RunningRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningRecordDao {
    // 插入跑步记录
    @Insert
    suspend fun insertRecord(record: RunningRecord): Long

    // 删除记录
    @Delete
    suspend fun deleteRecord(record: RunningRecord)

    // 查询用户的所有跑步记录（按时间倒序）
    @Query("SELECT * FROM running_record WHERE userId = :userId ORDER BY startTime DESC")
    fun getUserRecords(userId: Long): Flow<List<RunningRecord>>

    // 查询指定时间范围内的记录
    @Query("SELECT * FROM running_record WHERE userId = :userId AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getRecordsByTimeRange(userId: Long, startTime: Long, endTime: Long): List<RunningRecord>
}