import Foundation

struct ProduceItem: Identifiable, Codable {
    let id: String
    let name: String
    let category: String
    let currentPrice: Double
    let level: String
}

// 模擬歷史資料用的結構
struct PriceHistory: Identifiable {
    let id = UUID()
    let date: String
    let price: Double
}
