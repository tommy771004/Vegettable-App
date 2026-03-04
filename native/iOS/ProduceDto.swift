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
