package com.example.produceapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.produceapp.network.ProduceService

// 1. Room Database Entity (本地端快取)
@Entity(tableName = "produce_items")
data class ProduceEntity(
    @PrimaryKey val cropCode: String,
    val cropName: String,
    val averagePrice: Double,
    val date: String,
    val marketName: String,
    val transQuantity: Double = 0.0,
    val cacheType: String = "DEFAULT" // "TOP_VOLUME", "FAVORITE", "DEFAULT"
)

// 2. Room DAO (資料庫操作)
@Dao
interface ProduceDao {
    @Query("SELECT * FROM produce_items")
    fun getAllProduce(): Flow<List<ProduceEntity>>

    @Query("SELECT * FROM produce_items WHERE marketName = :market")
    fun getProduceByMarket(market: String): Flow<List<ProduceEntity>>

    @Query("SELECT * FROM produce_items WHERE cacheType = :type")
    fun getCachedProduceByType(type: String): Flow<List<ProduceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produce: List<ProduceEntity>)

    @Query("DELETE FROM produce_items")
    suspend fun clearAll()
    
    @Query("DELETE FROM produce_items WHERE cacheType = :type")
    suspend fun clearCacheByType(type: String)
}

// 4. Repository (Offline-First 邏輯)
class ProduceRepository(
    private val api: ProduceService,
    private val dao: ProduceDao
) {
    // 取得資料：先回傳本地 Room 資料，同時背景向 API 請求最新資料並更新 Room
    fun getProduceData(): Flow<List<ProduceEntity>> = dao.getAllProduce()

    fun getProduceDataByMarket(market: String): Flow<List<ProduceEntity>> = dao.getProduceByMarket(market)

    suspend fun refreshData() {
        try {
            // 1. 從後端 API 取得最新資料
            val response = api.getDailyPrices("", 1, 100)
            val networkData = response.data
            
            // 2. 轉換為 Room Entity
            val entities = networkData.map { dto ->
                ProduceEntity(
                    cropCode = dto.cropCode,
                    cropName = dto.cropName,
                    averagePrice = dto.avgPrice,
                    date = dto.date,
                    marketName = dto.marketName
                )
            }
            
            // 3. 更新本地資料庫 (Room 會自動觸發 Flow 更新 UI)
            dao.insertAll(entities)
        } catch (e: Exception) {
            // 處理網路錯誤 (例如無網路時，UI 依然會顯示 Room 內的舊資料)
            e.printStackTrace()
        }
    }
    
    // Proxy methods for ViewModel
    suspend fun getPriceAnomalies() = api.getPriceAnomalies()
    suspend fun getTopVolumeCrops() = api.getTopVolumeCrops()
    suspend fun getDailyPrices(keyword: String, page: Int, pageSize: Int) = api.getDailyPrices(keyword, page, pageSize)
    suspend fun getPriceHistory(produceId: String) = api.getPriceHistory(produceId)
    suspend fun getPriceForecast(produceId: String) = api.getPriceForecast(produceId)
    suspend fun getFavorites() = api.getFavorites()
    suspend fun getSeasonalCrops() = api.getSeasonalCrops()
}
