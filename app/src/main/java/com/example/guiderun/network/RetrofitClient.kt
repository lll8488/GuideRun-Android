package com.example.guiderun.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 使用JSONPlaceholder作为公共Mock API
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    // 懒加载Retrofit实例
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 提供ApiService实例
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}