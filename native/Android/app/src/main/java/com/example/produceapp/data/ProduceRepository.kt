package com.example.produceapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Room Database Entity (本地端快取)
@Entity(tableName = "produce_items")
data class ProduceEntity(
    @PrimaryKey val cropCode: String,
    val cropName: String,
    val averagePrice: Double,
    val date: String,
    val marketName: String
)

// 2. Room DAO (資料庫操作)
@Dao
interface ProduceDao {
    @Query("SELECT * FROM produce_items")
    fun getAllProduce(): Flow<List<ProduceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produce: List<ProduceEntity>)

    @Query("DELETE FROM produce_items")
    suspend fun clearAll()
}

// 3. Retrofit API Interface (政府 Open Data)
// 台灣農產品交易行情 API 範例
data class ProduceDto(
    val 作物代號: String,
    val 作物名稱: String,
    val 平均價: Double,
    val 交易日期: String,
    val 市場名稱: String
)

interface AgriApi {
    @GET("Service/OpenData/FromM/FarmTransData.aspx")
    suspend fun getDailyProduce(
        @Query("top") top: Int = 100 // 取得前 100 筆
    ): List<ProduceDto>
}

// 4. Repository (Offline-First 邏輯)
class ProduceRepository(
    private val api: AgriApi,
    private val dao: ProduceDao
) {
    // 取得資料：先回傳本地 Room 資料，同時背景向 API 請求最新資料並更新 Room
    fun getProduceData(): Flow<List<ProduceEntity>> = dao.getAllProduce()

    suspend fun refreshData() {
        try {
            // 1. 從政府 API 取得最新資料
            val networkData = api.getDailyProduce()
            
            // 2. 轉換為 Room Entity
            val entities = networkData.map { dto ->
                ProduceEntity(
                    cropCode = dto.作物代號,
                    cropName = dto.作物名稱,
                    averagePrice = dto.平均價,
                    date = dto.交易日期,
                    marketName = dto.市場名稱
                )
            }
            
            // 3. 更新本地資料庫 (Room 會自動觸發 Flow 更新 UI)
            dao.insertAll(entities)
        } catch (e: Exception) {
            // 處理網路錯誤 (例如無網路時，UI 依然會顯示 Room 內的舊資料)
            e.printStackTrace()
        }
    }
}
