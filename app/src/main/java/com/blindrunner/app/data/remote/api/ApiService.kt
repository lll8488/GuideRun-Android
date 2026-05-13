package com.blindrunner.app.data.remote.api

import com.blindrunner.app.data.remote.model.CreatePostRequest
import com.blindrunner.app.data.remote.model.PostResponse
import com.blindrunner.app.data.remote.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    /** Fetch all posts — simulates fetching running records from the server. */
    @GET("posts")
    suspend fun getPosts(): List<PostResponse>

    /** Fetch a single user by ID. */
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): UserResponse

    /** Create a new post — simulates syncing a running record to the server. */
    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): PostResponse
}
