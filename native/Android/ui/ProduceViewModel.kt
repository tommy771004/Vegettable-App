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

// =============================================================================
// ProduceViewModel.kt — Android MVVM 架構中的 ViewModel 層
//
// 架構說明：
//   MVVM 資料流：
//   UI (Composable) → 呼叫 ViewModel 方法 → ViewModel 呼叫 ProduceService
//   → API 回應 → ViewModel 更新 StateFlow → UI 自動重組 (Recomposition)
//
//   StateFlow vs LiveData：
//   - StateFlow 是 Kotlin Coroutines 的響應式資料容器，生命週期感知更好
//   - collectAsState() 讓 Composable 訂閱 StateFlow，資料變更時自動重新渲染
//   - MutableStateFlow (私有) + StateFlow (公開) = 封裝原則，
//     只有 ViewModel 自身可以修改狀態，UI 只能讀取
//
// Hilt 依賴注入：
//   @HiltViewModel + @Inject constructor 讓 Hilt 自動管理 ViewModel 的建立與依賴注入
//   ProduceService 由 Hilt 在 AppModule 中提供，不需要手動 new
//
// 對應 iOS ProduceViewModel.swift：
//   @MainActor class ProduceViewModel     = @HiltViewModel class ProduceViewModel
//   @Published var dailyPrices            = MutableStateFlow _dailyPrices
//   enum Resource<T>                      = sealed class Resource<T>
//   Task { await fetchDashboardData() }   = init { fetchDashboardData() }
// =============================================================================

/**
 * 農產品資料 ViewModel
 * 使用 Hilt 依賴注入，由 Hilt 管理生命週期（與 Activity/Fragment 綁定）
 */
@HiltViewModel
class ProduceViewModel @Inject constructor(
    // ProduceService 由 Hilt AppModule 提供
    // 注意：目前直接注入 ProduceService，建議未來改為注入 ProduceRepository 以支援離線快取
    private val produceService: ProduceService
) : ViewModel() {

    // MARK: - StateFlow 狀態定義
    // 每個 MutableStateFlow 對應一個 API 請求的三態狀態機
    // 初始值均為 Resource.Loading()，讓 UI 立即顯示骨架屏

    /** 我的收藏清單 + 是否達到目標提醒價格 */
    private val _favorites = MutableStateFlow<Resource<List<FavoriteAlertDto>>>(Resource.Loading())
    val favorites: StateFlow<Resource<List<FavoriteAlertDto>>> = _favorites

    /** 今日農產品批發價格列表（分頁，預設第 1 頁） */
    private val _dailyPrices = MutableStateFlow<Resource<List<ProduceDto>>>(Resource.Loading())
    val dailyPrices: StateFlow<Resource<List<ProduceDto>>> = _dailyPrices

    /** 今日交易量前 10 名農產品 */
    private val _topVolume = MutableStateFlow<Resource<List<ProduceDto>>>(Resource.Loading())
    val topVolume: StateFlow<Resource<List<ProduceDto>>> = _topVolume

    /** 單日漲幅超過 50% 的價格異常警告清單 */
    private val _anomalies = MutableStateFlow<Resource<List<PriceAnomalyDto>>>(Resource.Loading())
    val anomalies: StateFlow<Resource<List<PriceAnomalyDto>>> = _anomalies

    /** 高麗菜（LA1）歷史 30 天價格，供折線圖展示 */
    private val _historicalData = MutableStateFlow<Resource<List<HistoricalPriceDto>>>(Resource.Loading())
    val historicalData: StateFlow<Resource<List<HistoricalPriceDto>>> = _historicalData

    /** 明日預測價格（由 7 日移動平均線趨勢計算） */
    private val _predictedData = MutableStateFlow<Resource<List<PricePredictionDto>>>(Resource.Loading())
    val predictedData: StateFlow<Resource<List<PricePredictionDto>>> = _predictedData

    /** 當季盛產農作物清單 */
    private val _seasonalCrops = MutableStateFlow<Resource<List<SeasonalCropDto>>>(Resource.Loading())
    val seasonalCrops: StateFlow<Resource<List<SeasonalCropDto>>> = _seasonalCrops

    /** 颱風/豪大雨天氣預警 */
    private val _weatherAlerts = MutableStateFlow<Resource<WeatherAlertDto>>(Resource.Loading())
    val weatherAlerts: StateFlow<Resource<WeatherAlertDto>> = _weatherAlerts

    /** 今日省錢食譜推薦 */
    private val _budgetRecipes = MutableStateFlow<Resource<List<BudgetRecipeDto>>>(Resource.Loading())
    val budgetRecipes: StateFlow<Resource<List<BudgetRecipeDto>>> = _budgetRecipes

    // MARK: - 初始化

    /**
     * ViewModel 建立時自動載入首頁所需的所有資料
     * viewModelScope 確保 Coroutine 在 ViewModel 銷毀時自動取消，避免記憶體洩漏
     */
    init {
        fetchDashboardData()
    }

    // MARK: - 資料載入方法

    /**
     * 載入首頁所需的所有資料區塊
     * 使用單一 try-catch 包裝所有請求，任一失敗則所有狀態都設為 Error
     *
     * 注意：目前是循序執行（一個完成才執行下一個）。
     * 未來優化方向：使用 async { } + awaitAll() 讓多個 API 並行執行，縮短載入時間。
     */
    fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                // 1. 載入價格異常警告（漲幅 > 50% 的農產品）
                _anomalies.value = Resource.Success(produceService.getPriceAnomalies())

                // 2. 載入今日熱門交易（交易量前 10 名）
                _topVolume.value = Resource.Success(produceService.getTopVolumeCrops())

                // 3. 載入當季盛產日曆
                _seasonalCrops.value = Resource.Success(produceService.getSeasonalCrops())

                // 4. 載入今日菜價（第 1 頁，20 筆）
                val response = produceService.getDailyPrices("", 1, 20)
                _dailyPrices.value = Resource.Success(response.data)

                // 5. 載入高麗菜（LA1）歷史趨勢圖表資料
                //    高麗菜是台灣消費量最大的葉菜類，作為首頁示範圖表
                val history = produceService.getPriceHistory("LA1")
                _historicalData.value = Resource.Success(history)

                // 6. 計算明日預測價格
                //    Up   → 預測比近 7 日均價高 5%
                //    Down/Stable → 預測比近 7 日均價低 5%
                val forecast = produceService.getPriceForecast("LA1")
                val nextDayPrice = if (forecast.trend == "Up") {
                    forecast.recentAverage * 1.05
                } else {
                    forecast.recentAverage * 0.95
                }
                _predictedData.value = Resource.Success(listOf(
                    PricePredictionDto("Next Day", nextDayPrice)
                ))

                // 7. 載入我的收藏及是否達到目標提醒價
                _favorites.value = Resource.Success(produceService.getFavorites())

            } catch (e: Exception) {
                // 任一 API 失敗時，將所有狀態設為 Error
                e.printStackTrace()
                val errorMsg = e.message ?: "網路連線失敗，請稍後再試"
                _anomalies.value      = Resource.Error(errorMsg)
                _topVolume.value      = Resource.Error(errorMsg)
                _seasonalCrops.value  = Resource.Error(errorMsg)
                _dailyPrices.value    = Resource.Error(errorMsg)
                _historicalData.value = Resource.Error(errorMsg)
                _predictedData.value  = Resource.Error(errorMsg)
                _favorites.value      = Resource.Error(errorMsg)
            }

            // 天氣預警與省錢食譜獨立載入，不影響主要資料
            try {
                _weatherAlerts.value = Resource.Success(produceService.getWeatherAlerts())
            } catch (e: Exception) {
                _weatherAlerts.value = Resource.Error("天氣預警載入失敗")
            }
            try {
                _budgetRecipes.value = Resource.Success(produceService.getBudgetRecipes())
            } catch (e: Exception) {
                _budgetRecipes.value = Resource.Error("食譜推薦載入失敗")
            }
        }
    }

    /**
     * 語音搜尋：依農產品名稱查詢當日均價（供長輩語音模式使用）
     *
     * @param name 農產品名稱（例："番茄"）
     * @return 當日均價（元/公斤），查詢失敗時返回 null
     */
    suspend fun searchCropPrice(name: String): Double? {
        return try {
            val response = produceService.getDailyPrices(name, 1, 1)
            response.data.firstOrNull()?.avgPrice
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 社群零售價回報：回傳 true 表示成功，false 表示失敗
     * 成功後後端累積 5 點貢獻點數
     */
    suspend fun submitCommunityPrice(
        marketName: String,
        cropName: String,
        retailPrice: Double
    ): Boolean {
        return try {
            produceService.submitCommunityPrice(
                CommunityPriceDto(
                    cropCode = cropName,   // 後端支援名稱模糊比對
                    cropName = cropName,
                    marketName = marketName,
                    retailPrice = retailPrice
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 取得使用者貢獻統計（點數、等級、回報次數）
     * @return UserStatsDto 或 null（若載入失敗）
     */
    suspend fun getUserStats(): UserStatsDto? {
        return try {
            produceService.getUserStats()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 各市場同一農產品比價查詢
     * @param cropName 農產品名稱（例："高麗菜"）
     * @return 比價清單（依均價由低到高排序），失敗回傳空清單
     */
    suspend fun getMarketComparison(cropName: String): List<MarketCompareDto> {
        return try {
            produceService.getMarketComparison(cropName).sortedBy { it.avgPrice }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
