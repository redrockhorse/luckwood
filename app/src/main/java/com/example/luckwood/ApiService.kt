package com.example.luckwood

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// API接口定义
interface FootballApiService {
    @POST("/api/v1/analysis/future-matches")
    suspend fun getFutureMatches(@Body request: MatchRequest): ApiResponse
    
    @retrofit2.http.GET("/api/v1/lottery/ssq")
    suspend fun getSSQLuckyNumbers(
        @retrofit2.http.Query("n") n: Int = 5
    ): SSQResponse
    
    @retrofit2.http.GET("/api/v1/lottery/dlt")
    suspend fun getDLTLuckyNumbers(
        @retrofit2.http.Query("n") n: Int = 3
    ): DLTResponse
}

// Retrofit实例
object RetrofitClient {
    private const val BASE_URL = "http://39.101.76.38:8057/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: FootballApiService = retrofit.create(FootballApiService::class.java)
}

