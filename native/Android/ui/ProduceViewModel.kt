package com.example.produceapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.produceapp.data.ProduceDto
import com.example.produceapp.data.PriceAnomalyDto
import com.example.produceapp.network.ProduceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 補充 DTO 定義 (供圖表使用)
data class HistoricalPriceDto(val date: String, val avgPrice: Float)
data class PricePredictionDto(val date: String, val predictedPrice: Float)
data class FavoriteAlertDto(val cropCode: String, val cropName: String, val currentPrice: Float, val targetPrice: Float, val isReached: Boolean)

class ProduceViewModel(private val produceService: ProduceService) : ViewModel() {
    private val _favorites = MutableStateFlow<List<FavoriteAlertDto>>(emptyList())
    val favorites: StateFlow<List<FavoriteAlertDto>> = _favorites


    private val _dailyPrices = MutableStateFlow<List<ProduceDto>>(emptyList())
    val dailyPrices: StateFlow<List<ProduceDto>> = _dailyPrices

    private val _topVolume = MutableStateFlow<List<ProduceDto>>(emptyList())
    val topVolume: StateFlow<List<ProduceDto>> = _topVolume

    private val _anomalies = MutableStateFlow<List<PriceAnomalyDto>>(emptyList())
    val anomalies: StateFlow<List<PriceAnomalyDto>> = _anomalies

    // 新增：歷史價格與預測資料
    private val _historicalData = MutableStateFlow<List<HistoricalPriceDto>>(emptyList())
    val historicalData: StateFlow<List<HistoricalPriceDto>> = _historicalData

    private val _predictedData = MutableStateFlow<List<PricePredictionDto>>(emptyList())
    val predictedData: StateFlow<List<PricePredictionDto>> = _predictedData

    private val _seasonalCrops = MutableStateFlow<List<SeasonalCropDto>>(emptyList())
    val seasonalCrops: StateFlow<List<SeasonalCropDto>> = _seasonalCrops

    init {
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                _anomalies.value = produceService.getPriceAnomalies()
                _topVolume.value = produceService.getTopVolumeCrops()
                _seasonalCrops.value = produceService.getSeasonalCrops()
                
                val response = produceService.getDailyPrices("", 1, 20)
                _dailyPrices.value = response.data

                // 取得高麗菜的歷史與預測資料 (供圖表展示)
                // 這裡暫時寫死 "LA1" (甘藍/高麗菜) 作為範例，實際應由 UI 傳入
                val history = produceService.getPriceHistory("LA1")
                _historicalData.value = history
                
                val forecast = produceService.getPriceForecast("LA1")
                // Convert forecast response to list of PricePredictionDto if needed
                // Assuming forecast.trend and message are used elsewhere or we just map a simple prediction
                // For now, let's just map the recent average as a prediction point for simplicity or create a dummy prediction based on trend
                // Since the API returns a summary, we might need to adjust the UI or API to return a list of predicted points.
                // For this step, I will create a simple prediction point based on the trend.
                val nextDayPrice = if (forecast.trend == "Up") forecast.recentAverage * 1.05 else forecast.recentAverage * 0.95
                _predictedData.value = listOf(
                    PricePredictionDto("Next Day", nextDayPrice.toFloat())
                )

                // 取得我的收藏與追蹤資料
                _favorites.value = produceService.getFavorites()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun searchCropPrice(name: String): Double? {
        return try {
            val response = produceService.getDailyPrices(name, 1, 1)
            response.data.firstOrNull()?.avgPrice
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
