import Foundation

// =============================================================================
// ProduceDto.swift — 所有資料傳輸物件 (DTO) 的唯一定義檔案
//
// 架構說明：
//   此檔案是整個 iOS 專案 DTO 的「Single Source of Truth（單一真實來源）」。
//   所有 struct 均實作 Codable，讓 JSONDecoder 可以直接將後端 JSON 自動轉換成
//   Swift Struct，無需手動解析 JSON key，大幅減少錯誤。
//   每個 DTO 的欄位名稱與後端 C# Model 欄位名稱一致 (camelCase)。
// =============================================================================

// MARK: - 農產品基本資料
/// 對應後端 ProduceDto.cs，包含一筆農產品的日期、市場、價格資訊
struct ProduceDto: Codable {
    let cropCode: String      // 農產品代碼 (例："LA1" = 高麗菜)
    let cropName: String      // 農產品名稱 (例："高麗菜")
    let marketCode: String    // 市場代碼 (例："110" = 台北第一果菜市場)
    let marketName: String    // 市場名稱 (例："台北第一果菜")
    let avgPrice: Double      // 當日平均價格 (元/公斤)
    let transQuantity: Double // 當日交易量 (公斤)
    let date: String          // 交易日期 (格式：yyyy-MM-dd)
}

// MARK: - 分頁回應包裝器
/// 通用分頁回應容器，搭配 T 泛型讓任何 DTO 都可以被分頁包裝
/// 對應後端 PaginatedResponse<T>.cs
struct PaginatedResponse<T: Codable>: Codable {
    let currentPage: Int  // 當前頁碼 (從 1 開始)
    let totalPages: Int   // 總頁數
    let totalItems: Int   // 總資料筆數
    let data: [T]         // 當前頁的資料陣列
}

// MARK: - 收藏與價格提醒
/// 我的收藏與價格提醒 DTO，對應後端 FavoriteAlertDto.cs
/// 注意：此為「唯一」定義，ProduceViewModel.swift 中的重複定義已移除
///      以避免 Swift 編譯器 "redefinition of struct" 錯誤
struct FavoriteAlertDto: Codable, Identifiable {
    // Identifiable 所需的唯一 id，使用 UUID 自動生成，不需要從後端傳遞
    var id: UUID { UUID() }

    let produceId: String       // 農產品代碼 (與 UserFavorite.ProduceId 對應)
    let produceName: String     // 農產品名稱
    let targetPrice: Double     // 使用者設定的目標提醒價格 (元/公斤)
    let currentPrice: Double    // 今日最新市場平均價格 (元/公斤)
    let isAlertTriggered: Bool  // 是否已達到目標價格 (currentPrice <= targetPrice)

    // 自訂 CodingKeys：確保 Swift camelCase 與 JSON camelCase 欄位名稱正確對應
    enum CodingKeys: String, CodingKey {
        case produceId, produceName, targetPrice, currentPrice, isAlertTriggered
    }
}

// MARK: - 社群回報
/// 社群零售價回報 DTO，用於 POST /community-price API
/// 使用者在超市/市場親眼看到的零售價，回報給社群共享
struct CommunityPriceDto: Codable {
    let cropCode: String     // 農產品代碼
    let cropName: String     // 農產品名稱
    let marketName: String   // 回報地點 (例："全聯福利中心信義店")
    let retailPrice: Double  // 親眼看到的零售價 (元/公斤 或 元/把)
    let reportDate: String?  // 回報時間 (後端自動填入，前端可省略)
}

// MARK: - 當季盛產日曆
/// 當季盛產農作物 DTO，對應後端 SeasonalCropDto.cs
struct SeasonalCropDto: Codable {
    let cropCode: String    // 農產品代碼
    let cropName: String    // 農產品名稱 (例："番茄")
    let season: String      // 盛產季節名稱 (例："冬春季 12-3月")
    let description: String // 盛產特色描述 (例："此時甜度最高，價格最實惠")
}

// MARK: - 價格異常偵測
/// 價格異常警告 DTO，當某農產品單日漲幅超過 50% 時觸發
struct PriceAnomalyDto: Codable {
    let cropCode: String            // 農產品代碼
    let cropName: String            // 農產品名稱
    let currentPrice: Double        // 今日均價 (元/公斤)
    let previousPrice: Double       // 昨日均價 (元/公斤)
    let increasePercentage: Double  // 漲幅百分比 (例：75.5 表示漲了 75.5%)
    let alertMessage: String        // 人性化警告訊息 (例："⚠️ 高麗菜 單日暴漲 75%！")
}

// MARK: - 價格預測
/// 價格趨勢預測回應 DTO，對應後端 /forecast/{produceId} API
/// 後端使用 7 日移動平均線來計算趨勢方向
struct PricePredictionResponse: Codable {
    let recentAverage: Double   // 最近 7 天均價 (元/公斤)
    let previousAverage: Double // 前 7 天均價 (元/公斤)
    let trend: String           // 趨勢方向："Up" / "Down" / "Stable"
    let message: String         // 人性化趨勢說明文字
}

// MARK: - 天氣預警
/// 颱風/豪大雨預警 DTO，對應後端 /weather-alerts API
/// 後端會即時抓取中央氣象署 RSS，解析是否有颱風或豪雨警報
struct WeatherAlertDto: Codable {
    let alertType: String       // 警報類型："None" / "Typhoon" / "HeavyRain"
    let severity: String?       // 嚴重程度："Low" / "Medium" / "High"
    let title: String?          // 警報標題 (例："⚠️ 颱風警報發布！")
    let message: String?        // 詳細說明 (包含建議採購策略)
    let affectedCrops: [String]? // 預計受影響的農作物清單
}

// MARK: - 省錢食譜
/// 省錢食譜 DTO，對應後端 /budget-recipes API
/// 後端根據今日價格跌幅最大的農產品，自動推薦相關食譜
struct BudgetRecipeDto: Codable {
    let recipeName: String          // 食譜名稱 (例："番茄炒蛋")
    let mainIngredients: [String]   // 主要食材清單
    let reason: String              // 推薦理由 (例："今日番茄跌 30%，只要 15 元/公斤！")
    let imageUrl: String            // 食譜縮圖 URL 或 emoji 代替
    let steps: [String]             // 烹飪步驟陣列
}

// MARK: - 使用者貢獻統計
/// 使用者貢獻點數與等級 DTO，對應後端 /user-stats API
/// 每次回報社群物價可獲得 5 點；點數累積達門檻即升級
struct UserStatsDto: Codable {
    let contributionPoints: Int  // 累積貢獻點數 (每次回報 +5)
    let level: String            // 使用者等級："新手菜鳥" / "精打細算" / "市場達人"
    let reportCount: Int         // 累計回報次數
}

// MARK: - 市場比價
/// 各市場同一農產品批發均價 DTO，對應後端 /compare/{cropName} API
/// 由低到高排序後可讓使用者選擇最便宜的市場前往採購
struct MarketComparisonDto: Codable {
    let marketName: String      // 市場名稱 (例："台北第一果菜")
    let avgPrice: Double        // 當日批發均價 (元/公斤)
    let transQuantity: Double   // 當日交易量 (公斤)
    let date: String            // 交易日期 (yyyy-MM-dd)
}
