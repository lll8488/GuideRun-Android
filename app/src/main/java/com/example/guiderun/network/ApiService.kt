package com.example.guiderun.network

import com.example.guiderun.data.model.Demand
import com.example.guiderun.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    // 1. GET：获取用户详情（模拟登录验证）
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Long): Response<User>

    // 2. GET：获取所有待接单需求（模拟从服务器拉取需求列表）
    @GET("posts")
    suspend fun getRemoteDemands(): Response<List<Demand>>

    // 3. POST：发布新需求（模拟向服务器提交需求）
    @POST("posts")
    suspend fun createRemoteDemand(@Body demand: Demand): Response<Demand>
}