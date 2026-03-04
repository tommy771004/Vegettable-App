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
