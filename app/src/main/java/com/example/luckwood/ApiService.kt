package com.example.luckwood

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
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

    @retrofit2.http.GET("/api/v1/lottery/ssq/last")
    suspend fun getSSQLastDraw(): SSQLastDrawResponse

    @retrofit2.http.GET("/api/v1/lottery/dlt/last")
    suspend fun getDLTLastDraw(): DLTLastDrawResponse

    @GET("/api/v1/lottery/kl8/last")
    suspend fun getKL8LastDraw(): KL8LastDrawResponse

    @POST("/api/v1/lottery/picks")
    suspend fun savePicks(@Body request: SavePicksRequest): SavePicksResponse

    @GET("/api/v1/lottery/picks")
    suspend fun getPicks(
        @Query("lottery_type") lotteryType: String? = null,
        @Query("issue_code") issueCode: String? = null,
        @Query("status") status: String? = null,
        @Query("batch_id") batchId: String? = null
    ): PicksListResponse

    @GET("/api/v1/lottery/picks/check")
    suspend fun checkPicks(
        @Query("lottery_type") lotteryType: String,
        @Query("issue_code") issueCode: String
    ): PickCheckResponse

    @DELETE("/api/v1/lottery/picks/{id}")
    suspend fun deletePick(@Path("id") id: Int): DeletePickResponse
}

// Retrofit实例
object RetrofitClient {
    private const val BASE_URL = "http://39.101.76.38:8057/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY logging can dump huge payloads and amplify main-thread jank under load.
        level = HttpLoggingInterceptor.Level.BASIC
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

