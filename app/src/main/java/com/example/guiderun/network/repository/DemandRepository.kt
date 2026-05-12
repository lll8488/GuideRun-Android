package com.example.guiderun.repository

import com.example.guiderun.data.dao.DemandDao
import com.example.guiderun.data.model.Demand
import com.example.guiderun.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class DemandRepository(
    private val demandDao: DemandDao,
    private val apiService: ApiService
) {
    // 获取待接单需求：先返回本地数据，再从网络更新本地
    fun getPendingDemands(): Flow<List<Demand>> = flow {
        // 第一步：先发射本地数据库的数据
        val localDemands = demandDao.getPendingDemands().first()
        emit(localDemands)

        // 第二步：从网络请求最新数据
        try {
            val response = apiService.getRemoteDemands()
            if (response.isSuccessful && response.body() != null) {
                val remoteDemands = response.body()!!
                // 过滤出待接单状态的需求，插入本地数据库
                val pendingRemoteDemands = remoteDemands.filter { it.status == 0 }
                pendingRemoteDemands.forEach { demandDao.insertDemand(it) }
                // 发射更新后的本地数据
                emit(demandDao.getPendingDemands().first())
            }
        } catch (e: Exception) {
            // 网络请求失败，继续使用本地数据
            e.printStackTrace()
        }
    }

    // 发布需求：先提交到服务器，成功后再保存到本地
    suspend fun publishDemand(demand: Demand): Long {
        return try {
            // 先提交到远程服务器
            val response = apiService.createRemoteDemand(demand)
            if (response.isSuccessful && response.body() != null) {
                val remoteDemand = response.body()!!
                // 服务器返回带ID的需求，保存到本地
                demandDao.insertDemand(remoteDemand)
            } else {
                // 服务器提交失败，直接保存到本地
                demandDao.insertDemand(demand)
            }
        } catch (e: Exception) {
            // 网络异常，直接保存到本地
            demandDao.insertDemand(demand)
        }
    }

    // 接单：更新本地需求状态
    suspend fun acceptDemand(demandId: Long, runnerId: Long): Boolean {
        val demand = demandDao.getDemandById(demandId) ?: return false
        val updatedDemand = demand.copy(
            runnerId = runnerId,
            status = 1, // 已接单
            updateTime = System.currentTimeMillis()
        )
        demandDao.updateDemand(updatedDemand)
        return true
    }
}