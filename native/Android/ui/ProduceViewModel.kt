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

class ProduceViewModel(private val produceService: ProduceService) : ViewModel() {
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

    init {
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                _anomalies.value = produceService.getPriceAnomalies()
                _topVolume.value = produceService.getTopVolumeCrops()
                
                val response = produceService.getDailyPrices("", 1, 20)
                _dailyPrices.value = response.data

                // 模擬取得高麗菜的歷史與預測資料 (供圖表展示)
                _historicalData.value = listOf(
                    HistoricalPriceDto("10/01", 22.5f),
                    HistoricalPriceDto("10/02", 24.0f),
                    HistoricalPriceDto("10/03", 23.5f),
                    HistoricalPriceDto("10/04", 28.0f),
                    HistoricalPriceDto("10/05", 35.0f) // 暴漲點
                )
                _predictedData.value = listOf(
                    PricePredictionDto("10/06", 33.0f),
                    PricePredictionDto("10/07", 30.0f),
                    PricePredictionDto("10/08", 28.5f)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
