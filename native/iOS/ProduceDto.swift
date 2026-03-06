import Foundation

// 統一的資料傳輸物件 (DTO)，確保前後端欄位完全一致
struct ProduceDto: Codable {
    let cropCode: String
    let cropName: String
    let marketCode: String
    let marketName: String
    let avgPrice: Double
    let transQuantity: Double
    let date: String
}

struct PaginatedResponse<T: Codable>: Codable {
    let currentPage: Int
    let totalPages: Int
    let totalItems: Int
    let data: [T]
}

// 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts) 的 DTO
struct FavoriteAlertDto: Codable {
    let produceId: String
    let produceName: String
    let targetPrice: Double
    let currentPrice: Double
    let isAlertTriggered: Bool
}

// 新增功能：社群回報機制 (Community Retail Price) 的 DTO
struct CommunityPriceDto: Codable {
    let cropCode: String
    let cropName: String
    let marketName: String
    let retailPrice: Double
    let reportDate: String? // Optional since it might not be sent when reporting
}

// 新增功能：當季盛產日曆 (Seasonal Crop Calendar) 的 DTO
struct SeasonalCropDto: Codable {
    let cropCode: String
    let cropName: String
    let season: String
    let description: String
}

// 新增功能：價格異常警告 (Price Anomaly Detection) 的 DTO
struct PriceAnomalyDto: Codable {
    let cropCode: String
    let cropName: String
    let currentPrice: Double
    let previousPrice: Double
    let increasePercentage: Double
    let alertMessage: String
}

// 價格預測回應 DTO (對應後端 /forecast/{produceId} API)
struct PricePredictionResponse: Codable {
    let recentAverage: Double
    let previousAverage: Double
    let trend: String
    let message: String
}

// 天氣預警 DTO (對應後端 /weather-alerts API)
struct WeatherAlertDto: Codable {
    let alertType: String
    let severity: String?
    let title: String?
    let message: String?
    let affectedCrops: [String]?
}

// 省錢食譜 DTO (對應後端 /budget-recipes API)
struct BudgetRecipeDto: Codable {
    let recipeName: String
    let mainIngredients: [String]
    let reason: String
    let imageUrl: String
    let steps: [String]
}
