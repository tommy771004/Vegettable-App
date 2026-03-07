package com.example.produceapp.network

import com.example.produceapp.data.*
import retrofit2.http.*

// =============================================================================
// ProduceService.kt — Retrofit API 介面定義
//
// 架構說明：
//   Retrofit 透過 Java/Kotlin 介面 + 註解自動生成 HTTP 請求實作。
//   此介面定義所有與後端溝通的 API 端點，無需手動撰寫 URLRequest。
//
//   每個方法對應一個後端 API 端點：
//   - @GET("path") → HTTP GET 請求
//   - @POST("path") → HTTP POST 請求
//   - @Path("id") → URL 路徑參數 (例：/history/{produceId})
//   - @Query("key") → URL Query String 參數 (例：?keyword=番茄&page=1)
//   - @Body → 將物件序列化成 JSON Request Body
//   - suspend → 支援 Kotlin Coroutines (在 ViewModel 的 viewModelScope.launch 中呼叫)
//
// JWT 認證說明：
//   所有端點的 Authorization Header 由 RetrofitClient 中的 JwtInterceptor 自動注入，
//   不需要在每個方法上手動添加 @Header。這樣確保所有 API 請求都帶有 JWT Token。
//
// 端點與後端 ProduceController.cs 對應關係：
//   getDailyPrices  ↔  GET  /api/produce/daily-prices
//   getPriceAnomalies ↔ GET /api/produce/anomalies
//   getTopVolumeCrops ↔ GET /api/produce/top-volume
//   getPriceHistory  ↔  GET  /api/produce/history/{produceId}
//   getPriceForecast ↔  GET  /api/produce/forecast/{produceId}
//   getFavorites     ↔  GET  /api/produce/favorites
//   getSeasonalCrops ↔  GET  /api/produce/seasonal
// =============================================================================

/**
 * 農產品後端 API Retrofit 介面
 * 由 RetrofitClient.getInstance(context) 建立實作並注入 JWT Interceptor
 */
interface ProduceService {

    /**
     * 取得今日農產品批發價格列表（分頁 + 關鍵字搜尋）
     * 對應後端：GET /api/produce/daily-prices
     *
     * @param keyword 搜尋關鍵字（農產品名稱 or 市場名稱），空字串表示不過濾
     * @param page    頁碼，從 1 開始
     * @param pageSize 每頁筆數，建議 20（避免回傳 2000+ 筆導致 OOM）
     * @return 分頁回應，包含當頁資料及總頁數
     */
    @GET("daily-prices")
    suspend fun getDailyPrices(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PaginatedResponse<ProduceDto>

    /**
     * 取得單日漲幅超過 50% 的農產品異常警告清單
     * 對應後端：GET /api/produce/anomalies
     * 需要 JWT 認證 ([Authorize] 端點)
     */
    @GET("anomalies")
    suspend fun getPriceAnomalies(): List<PriceAnomalyDto>

    /**
     * 取得今日交易量前 10 名農產品
     * 對應後端：GET /api/produce/top-volume
     * 讓使用者了解目前市場上最熱銷、最當季的農產品
     */
    @GET("top-volume")
    suspend fun getTopVolumeCrops(): List<ProduceDto>

    /**
     * 取得指定農產品的歷史 30 天均價記錄
     * 對應後端：GET /api/produce/history/{produceId}
     * 供 PriceTrendChart 折線圖使用
     *
     * @param produceId 農產品代碼（例："LA1" = 高麗菜）
     */
    @GET("history/{produceId}")
    suspend fun getPriceHistory(@Path("produceId") produceId: String): List<HistoricalPriceDto>

    /**
     * 取得農產品 7 日移動平均線趨勢預測
     * 對應後端：GET /api/produce/forecast/{produceId}
     * 後端使用最近 14 天歷史資料計算趨勢方向（Up/Down/Stable）
     *
     * @param produceId 農產品代碼
     * @return 趨勢預測結果，包含近 7 日均價、前 7 日均價、趨勢方向
     */
    @GET("forecast/{produceId}")
    suspend fun getPriceForecast(@Path("produceId") produceId: String): PricePredictionResponse

    /**
     * 取得我的收藏清單及目標提醒價格狀態
     * 對應後端：GET /api/produce/favorites
     * 需要 JWT 認證：UserId 從 Token 中取得，確保只能查看自己的收藏
     *
     * @return 收藏列表，每項包含 isAlertTriggered 欄位（是否已達目標價）
     */
    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteAlertDto>

    /**
     * 取得當季盛產農作物（依當前月份過濾）
     * 對應後端：GET /api/produce/seasonal
     * 資料來源：後端 SeasonalCrops 資料表（由 DbInitializer 植入種子資料）
     */
    @GET("seasonal")
    suspend fun getSeasonalCrops(): List<SeasonalCropDto>

    /**
     * 取得颱風/豪大雨預警（後端即時抓取中央氣象署 RSS）
     * 對應後端：GET /api/produce/weather-alerts
     */
    @GET("weather-alerts")
    suspend fun getWeatherAlerts(): WeatherAlertDto

    /**
     * 取得今日省錢食譜推薦（根據今日跌幅最大農產品配對）
     * 對應後端：GET /api/produce/budget-recipes
     */
    @GET("budget-recipes")
    suspend fun getBudgetRecipes(): List<BudgetRecipeDto>

    /**
     * 社群零售價回報（使用者親眼看到的超市/市場零售價）
     * 對應後端：POST /api/produce/community-price
     * 每次成功回報累積 5 點貢獻點數
     */
    @POST("community-price")
    suspend fun submitCommunityPrice(@Body body: CommunityPriceDto): Any

    /**
     * 查詢使用者貢獻統計（點數、等級、回報次數）
     * 對應後端：GET /api/produce/user-stats
     */
    @GET("user-stats")
    suspend fun getUserStats(): UserStatsDto

    /**
     * 各市場同一農產品的批發價比較
     * 對應後端：GET /api/produce/compare/{cropName}
     *
     * @param cropName 農產品名稱（例："高麗菜"），後端進行模糊比對
     */
    @GET("compare/{cropName}")
    suspend fun getMarketComparison(@Path("cropName") cropName: String): List<MarketCompareDto>

    /**
     * 新增或更新收藏農產品及目標到價提醒
     * 對應後端：POST /api/produce/favorites
     * JWT 識別使用者身份，targetPrice 為觸發 FCM 推播的門檻價
     */
    @POST("favorites")
    suspend fun addFavorite(@Body body: AddFavoriteDto): Any
}