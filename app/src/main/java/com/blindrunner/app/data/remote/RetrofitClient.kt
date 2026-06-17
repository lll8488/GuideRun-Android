package com.blindrunner.app.data.remote

import com.blindrunner.app.data.remote.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 模拟器用 10.0.2.2 访问宿主机 localhost
    // 真机用电脑的局域网 IP（如 192.168.x.x）
    // jsonplaceholder 作为降级/测试
    private const val EMULATOR_URL = "http://10.0.2.2:8080/"
    private const val FALLBACK_URL = "https://jsonplaceholder.typicode.com/"

    private var currentBaseUrl = EMULATOR_URL

    fun setServerUrl(url: String) {
        currentBaseUrl = if (url.endsWith("/")) url else "$url/"
    }

    fun getServerUrl(): String = currentBaseUrl

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun getApiService(): ApiService {
        val current = apiService
        if (current != null) return current
        synchronized(this) {
            val r = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val s = r.create(ApiService::class.java)
            apiService = s
            retrofit = r
            return s
        }
    }
}
