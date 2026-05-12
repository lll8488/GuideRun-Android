package com.example.guiderun

import com.example.guiderun.data.dao.DemandDao
import com.example.guiderun.data.model.Demand
import com.example.guiderun.network.ApiService
import com.example.guiderun.repository.DemandRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.*
import org.junit.Assert.*
import retrofit2.Response

class RepositoryTest {
    // 测试方法4：测试Repository接单逻辑
    @Test
    fun testAcceptDemand() = runTest {
        // Mock依赖
        val mockDemandDao = mock(DemandDao::class.java)
        val mockApiService = mock(ApiService::class.java)
        val repository = DemandRepository(mockDemandDao, mockApiService)

        // 准备测试数据
        val testDemand = Demand(
            demandId = 1,
            publisherId = 1,
            runDate = "2026-05-12",
            runTime = "18:00",
            location = "广州白云公园",
            duration = 60,
            status = 0 // 待接单
        )

        // Mock Dao的查询和更新方法
        `when`(mockDemandDao.getDemandById(1)).thenReturn(testDemand)
        `when`(mockDemandDao.updateDemand(any())).thenReturn(Unit)

        // 执行接单操作
        val result = repository.acceptDemand(1, 2)

        // 验证结果
        assertTrue(result)
        // 验证Dao的update方法被调用，且参数正确
        verify(mockDemandDao).updateDemand(argThat {
            it.demandId == 1L && it.runnerId == 2L && it.status == 1
        })
    }
}
