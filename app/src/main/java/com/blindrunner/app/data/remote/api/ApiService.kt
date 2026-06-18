package com.blindrunner.app.data.remote.api

import com.blindrunner.app.data.remote.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // ====== 认证 ======
    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    // ====== 用户 ======
    @GET("user/{phone}")
    suspend fun getUser(@Path("phone") phone: String): ApiResponse

    // ====== 需求 ======
    @POST("demands")
    suspend fun publishDemand(@Body request: DemandRequest): ApiResponse

    @GET("demands")
    suspend fun getDemands(): DemandListResponse

    @POST("demands/{id}/accept")
    suspend fun acceptDemand(@Path("id") id: Long, @Body request: AcceptDemandRequest): ApiResponse

    @GET("demands/owner/{phone}")
    suspend fun getOwnerDemands(@Path("phone") phone: String): DemandListResponse

    // ====== 跑步记录 ======
    @POST("running/records")
    suspend fun saveRunningRecord(@Body request: RunningRecordRequest): ApiResponse

    @GET("running/records/{phone}")
    suspend fun getRunningRecords(@Path("phone") phone: String): RunningRecordListResponse

    // ====== 考核 ======
    @POST("exam/submit")
    suspend fun submitExam(@Body request: ExamSubmitRequest): ApiResponse

    // ====== 排行榜 ======
    @GET("leaderboard")
    suspend fun getLeaderboard(): LeaderboardResponse

    // ====== 旧的 JSONPlaceholder（保持兼容） ======
    @GET("posts")
    suspend fun getPosts(): List<PostResponse>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): UserResponse

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): PostResponse
}
