package com.example.produceapp.network

import com.example.produceapp.data.*
import retrofit2.http.*

interface ProduceService {
    @GET("daily-prices")
    suspend fun getDailyPrices(
        @Query("keyword") keyword: String, 
        @Query("page") page: Int, 
        @Query("pageSize") pageSize: Int
    ): PaginatedResponse<ProduceDto>

    @GET("anomalies")
    suspend fun getPriceAnomalies(): List<PriceAnomalyDto>

    @GET("top-volume")
    suspend fun getTopVolumeCrops(): List<ProduceDto>

    @GET("history/{produceId}")
    suspend fun getPriceHistory(@Path("produceId") produceId: String): List<HistoricalPriceDto>

    @GET("forecast/{produceId}")
    suspend fun getPriceForecast(@Path("produceId") produceId: String): PricePredictionResponse

    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteAlertDto>
    
    @GET("seasonal")
    suspend fun getSeasonalCrops(): List<SeasonalCropDto>
}
