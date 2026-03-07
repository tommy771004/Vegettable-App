package com.example.produceapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.produceapp.network.ProduceService

// =============================================================================
// ProduceRepository.kt — Android 資料層：Room 本地快取 + Retrofit API 整合
//
// 此檔案包含三個部分：
//   1. ProduceEntity — Room 資料庫實體（本地快取的資料表結構）
//   2. ProduceDao     — Room DAO（資料庫 CRUD 操作介面）
//   3. ProduceRepository — 離線優先邏輯（整合 API + Room）
//
// 架構圖：
//   ProduceViewModel
//        ↓ 呼叫
//   ProduceRepository ← 資料協調層
//        ↓ 優先              ↓ 備援
//   ProduceService (Retrofit)  ProduceDao (Room)
//        ↓                          ↓
//   後端 API (網路)         SQLite 本地資料庫
//
// 離線優先策略 (Offline-First)：
//   getProduceData() 回傳 Flow<List<ProduceEntity>>，
//   UI 立即顯示 Room 中的本地資料（可能是空的或舊的），
//   同時 refreshData() 在背景從 API 拉取最新資料，
//   Room 更新後 Flow 自動 emit 新值，UI 無感刷新。
//
// 對應 iOS：
//   ProduceEntity = iOS CoreData Entity (NSManagedObject)
//   ProduceDao    = iOS CoreDataStore CRUD 方法
//   ProduceRepository = iOS ProduceRepository.swift
// =============================================================================

// MARK: - 1. Room Entity（本地快取資料表結構）
/**
 * 農產品本地快取 Entity，對應 SQLite 資料表 "produce_items"
 *
 * cacheType 欄位說明：
 *   用於區分不同來源的快取資料，支援選擇性清除：
 *   - "DEFAULT"    → 一般批發價格列表
 *   - "TOP_VOLUME" → 今日熱門交易前 10 名
 *   - "FAVORITE"   → 我的收藏列表
 */
@Entity(tableName = "produce_items")
data class ProduceEntity(
    @PrimaryKey val cropCode: String,  // 農產品代碼（唯一主鍵，例："LA1"）
    val cropName: String,              // 農產品名稱（例："高麗菜"）
    val averagePrice: Double,          // 批發均價（元/公斤）
    val date: String,                  // 交易日期（格式：yyyy-MM-dd）
    val marketName: String,            // 市場名稱（例："台北第一果菜"）
    val transQuantity: Double = 0.0,   // 交易量（公斤），預設 0
    val cacheType: String = "DEFAULT"  // 快取類型，用於選擇性清除特定類型快取
)

// MARK: - 2. Room DAO（資料庫操作介面）
/**
 * ProduceDao 定義所有與 "produce_items" 資料表的 CRUD 操作
 * Room 會在編譯時自動生成實作程式碼
 */
@Dao
interface ProduceDao {

    /**
     * 取得所有農產品快取資料（作為 Flow，資料變更時自動通知觀察者）
     * Flow<List<T>> 是 Room 的「響應式查詢」，相當於 iOS 的 NSFetchedResultsController
     */
    @Query("SELECT * FROM produce_items")
    fun getAllProduce(): Flow<List<ProduceEntity>>

    /**
     * 依市場名稱篩選農產品（例：查看台北市場的農產品）
     */
    @Query("SELECT * FROM produce_items WHERE marketName = :market")
    fun getProduceByMarket(market: String): Flow<List<ProduceEntity>>

    /**
     * 依快取類型篩選（例：只取 TOP_VOLUME 類型的記錄）
     */
    @Query("SELECT * FROM produce_items WHERE cacheType = :type")
    fun getCachedProduceByType(type: String): Flow<List<ProduceEntity>>

    /**
     * 批次插入或更新農產品資料
     * OnConflictStrategy.REPLACE：若 cropCode 相同，舊記錄直接被新記錄取代（Upsert 語義）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produce: List<ProduceEntity>)

    /** 清除所有快取資料（例：使用者登出時） */
    @Query("DELETE FROM produce_items")
    suspend fun clearAll()

    /** 清除特定類型的快取（例：只清除熱門交易的快取，保留其他類型） */
    @Query("DELETE FROM produce_items WHERE cacheType = :type")
    suspend fun clearCacheByType(type: String)
}

// MARK: - 3. ProduceRepository（離線優先資料協調層）
/**
 * ProduceRepository 是 ViewModel 與資料層之間的橋樑
 * 實作離線優先策略：先顯示 Room 資料，背景從 API 更新
 *
 * @param api Retrofit ProduceService 實例（由 Hilt 注入）
 * @param dao Room DAO 實例（由 Hilt 注入）
 */
class ProduceRepository(
    private val api: ProduceService,
    private val dao: ProduceDao
) {

    // MARK: - 離線優先資料流

    /**
     * 取得所有農產品的 Flow（Room 資料流）
     * UI 訂閱此 Flow 後，Room 資料有任何變動都會自動更新 UI，無需手動重新整理
     */
    fun getProduceData(): Flow<List<ProduceEntity>> = dao.getAllProduce()

    /**
     * 依市場名稱過濾的資料流
     */
    fun getProduceDataByMarket(market: String): Flow<List<ProduceEntity>> =
        dao.getProduceByMarket(market)

    /**
     * 從後端 API 拉取最新資料並更新 Room 資料庫
     * 此方法在背景執行，Room 更新後 Flow 自動 emit 新資料給 UI
     *
     * 設計說明：
     *   ViewModel 先呼叫 getProduceData() 開始觀察 Flow（此時 UI 顯示本地舊資料），
     *   再呼叫 refreshData() 觸發背景更新（更新完成後 UI 自動刷新）。
     *   這樣的設計稱為「先顯示舊資料、背景刷新」(Stale-While-Revalidate)。
     */
    suspend fun refreshData() {
        try {
            // 1. 從後端 API 取得最新資料（第 1 頁，最多 100 筆）
            val response = api.getDailyPrices("", 1, 100)

            // 2. 將 ProduceDto 轉換為 Room Entity
            val entities = response.data.map { dto ->
                ProduceEntity(
                    cropCode = dto.cropCode,
                    cropName = dto.cropName,
                    averagePrice = dto.avgPrice,
                    date = dto.date,
                    marketName = dto.marketName,
                    transQuantity = dto.transQuantity,
                    cacheType = "DEFAULT"
                )
            }

            // 3. 批次寫入 Room（OnConflictStrategy.REPLACE 確保 Upsert 語義）
            //    Room 寫入後，getAllProduce() 的 Flow 自動 emit 新資料
            dao.insertAll(entities)

        } catch (e: Exception) {
            // 網路錯誤時靜默失敗：UI 繼續顯示 Room 中的舊資料，不 crash App
            e.printStackTrace()
        }
    }

    // MARK: - Proxy 方法（直接代理到 API，不走 Room 快取）
    // 這些 API 回傳的資料比較動態（如異常偵測、預測），不適合長期快取

    /** 取得價格異常警告（單日漲幅 > 50%） */
    suspend fun getPriceAnomalies() = api.getPriceAnomalies()

    /** 取得今日交易量前 10 名 */
    suspend fun getTopVolumeCrops() = api.getTopVolumeCrops()

    /** 取得分頁批發價格 */
    suspend fun getDailyPrices(keyword: String, page: Int, pageSize: Int) =
        api.getDailyPrices(keyword, page, pageSize)

    /** 取得指定農產品歷史 30 天價格（供圖表使用） */
    suspend fun getPriceHistory(produceId: String) = api.getPriceHistory(produceId)

    /** 取得 7 日移動平均線趨勢預測 */
    suspend fun getPriceForecast(produceId: String) = api.getPriceForecast(produceId)

    /** 取得我的收藏清單及是否達到目標提醒價 */
    suspend fun getFavorites() = api.getFavorites()

    /** 取得當季盛產農作物清單 */
    suspend fun getSeasonalCrops() = api.getSeasonalCrops()
}
