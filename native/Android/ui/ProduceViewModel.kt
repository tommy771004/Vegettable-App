package com.example.produceapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.produceapp.data.*
import com.example.produceapp.network.ProduceService
import com.example.produceapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProduceViewModel @Inject constructor(
    private val produceService: ProduceService
) : ViewModel() {

    private val _favorites = MutableStateFlow<Resource<List<FavoriteAlertDto>>>(Resource.Loading())
    val favorites: StateFlow<Resource<List<FavoriteAlertDto>>> = _favorites

    private val _dailyPrices = MutableStateFlow<Resource<List<ProduceDto>>>(Resource.Loading())
    val dailyPrices: StateFlow<Resource<List<ProduceDto>>> = _dailyPrices

    private val _topVolume = MutableStateFlow<Resource<List<ProduceDto>>>(Resource.Loading())
    val topVolume: StateFlow<Resource<List<ProduceDto>>> = _topVolume

    private val _anomalies = MutableStateFlow<Resource<List<PriceAnomalyDto>>>(Resource.Loading())
    val anomalies: StateFlow<Resource<List<PriceAnomalyDto>>> = _anomalies

    private val _historicalData = MutableStateFlow<Resource<List<HistoricalPriceDto>>>(Resource.Loading())
    val historicalData: StateFlow<Resource<List<HistoricalPriceDto>>> = _historicalData

    private val _predictedData = MutableStateFlow<Resource<List<PricePredictionDto>>>(Resource.Loading())
    val predictedData: StateFlow<Resource<List<PricePredictionDto>>> = _predictedData

    private val _seasonalCrops = MutableStateFlow<Resource<List<SeasonalCropDto>>>(Resource.Loading())
    val seasonalCrops: StateFlow<Resource<List<SeasonalCropDto>>> = _seasonalCrops

    init {
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                _anomalies.value = Resource.Success(produceService.getPriceAnomalies())
                _topVolume.value = Resource.Success(produceService.getTopVolumeCrops())
                _seasonalCrops.value = Resource.Success(produceService.getSeasonalCrops())
                
                val response = produceService.getDailyPrices("", 1, 20)
                _dailyPrices.value = Resource.Success(response.data)

                // 取得高麗菜的歷史與預測資料 (供圖表展示)
                val history = produceService.getPriceHistory("LA1")
                _historicalData.value = Resource.Success(history)
                
                val forecast = produceService.getPriceForecast("LA1")
                val nextDayPrice = if (forecast.trend == "Up") forecast.recentAverage * 1.05 else forecast.recentAverage * 0.95
                _predictedData.value = Resource.Success(listOf(
                    PricePredictionDto("Next Day", nextDayPrice.toFloat())
                ))

                // 取得我的收藏與追蹤資料
                _favorites.value = Resource.Success(produceService.getFavorites())
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: "Unknown error"
                _anomalies.value = Resource.Error(errorMsg)
                _topVolume.value = Resource.Error(errorMsg)
                _seasonalCrops.value = Resource.Error(errorMsg)
                _dailyPrices.value = Resource.Error(errorMsg)
                _historicalData.value = Resource.Error(errorMsg)
                _predictedData.value = Resource.Error(errorMsg)
                _favorites.value = Resource.Error(errorMsg)
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
