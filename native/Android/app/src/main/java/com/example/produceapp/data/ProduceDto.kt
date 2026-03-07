package com.example.produceapp.data

import com.google.gson.annotations.SerializedName

data class ProduceDto(
    @SerializedName("cropCode") val cropCode: String,
    @SerializedName("cropName") val cropName: String,
    @SerializedName("avgPrice") val avgPrice: Double,
    @SerializedName("date") val date: String,
    @SerializedName("marketName") val marketName: String,
    @SerializedName("transQuantity") val transQuantity: Double
)

data class PaginatedResponse<T>(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val data: List<T>
)

data class FavoriteAlertDto(
    val produceId: String,
    val produceName: String,
    val targetPrice: Double,
    val currentPrice: Double,
    val isAlertTriggered: Boolean
)

data class PriceAnomalyDto(
    val cropCode: String,
    val cropName: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val increasePercentage: Double,
    val alertMessage: String
)

data class SeasonalCropDto(
    val cropCode: String,
    val cropName: String,
    val season: String,
    val description: String
)

data class HistoricalPriceDto(
    val date: String,
    val avgPrice: Double  // Double 與後端/iOS 一致，避免精度損失
)

data class PricePredictionDto(
    val date: String,
    val predictedPrice: Double  // Double 與後端/iOS 一致
)

data class WeatherAlertDto(
    val alertType: String,
    val severity: String?,
    val title: String?,
    val message: String?,
    val affectedCrops: List<String>?
)

data class BudgetRecipeDto(
    val recipeName: String,
    val mainIngredients: List<String>,
    val reason: String,
    val imageUrl: String,
    val steps: List<String>
)

data class PricePredictionResponse(
    val recentAverage: Double,
    val previousAverage: Double,
    val trend: String,
    val message: String
)
