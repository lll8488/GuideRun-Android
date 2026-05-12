package com.example.guiderun.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.guiderun.data.model.Demand
import kotlinx.coroutines.flow.Flow

@Dao
interface DemandDao {
    // 插入需求
    @Insert
    suspend fun insertDemand(demand: Demand): Long

    // 更新需求状态（接单/完成/取消）
    @Update
    suspend fun updateDemand(demand: Demand)

    // 删除需求
    @Delete
    suspend fun deleteDemand(demand: Demand)

    // 查询所有待接单需求（按发布时间倒序）
    @Query("SELECT * FROM demand WHERE status = 0 ORDER BY createTime DESC")
    fun getPendingDemands(): Flow<List<Demand>>

    // 根据发布者ID查询我的发布
    @Query("SELECT * FROM demand WHERE publisherId = :publisherId ORDER BY createTime DESC")
    fun getMyPublishedDemands(publisherId: Long): Flow<List<Demand>>

    // 根据需求ID查询详情
    @Query("SELECT * FROM demand WHERE demandId = :demandId LIMIT 1")
    suspend fun getDemandById(demandId: Long): Demand?
}