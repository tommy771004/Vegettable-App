import Foundation
import Combine

// 補充 DTO 定義 (供圖表使用)
struct HistoricalPriceDto: Identifiable {
    let id = UUID()
    let date: String
    let avgPrice: Double
}
struct PricePredictionDto: Identifiable {
    let id = UUID()
    let date: String
    let predictedPrice: Double
}
struct FavoriteAlertDto: Identifiable {
    let id = UUID()
    let cropCode: String
    let cropName: String
    let currentPrice: Double
    let targetPrice: Double
    let isReached: Bool
}

@MainActor
class ProduceViewModel: ObservableObject {
    @Published var favorites: [FavoriteAlertDto] = []

    @Published var dailyPrices: [ProduceDto] = []
    @Published var topVolume: [ProduceDto] = []
    @Published var anomalies: [PriceAnomalyDto] = []
    
    // 新增：歷史價格與預測資料
    @Published var historicalData: [HistoricalPriceDto] = []
    @Published var predictedData: [PricePredictionDto] = []
    
    private let produceService = ProduceService()
    
    init() {
        Task {
            await fetchDashboardData()
        }
    }
    
    func fetchDashboardData() async {
        do {
            self.anomalies = try await produceService.getPriceAnomalies()
            self.topVolume = try await produceService.getTopVolumeCrops()
            let response = try await produceService.getDailyPrices(keyword: "", page: 1, pageSize: 20)
            self.dailyPrices = response.data
            
            // 取得高麗菜的歷史與預測資料 (供圖表展示)
            self.historicalData = try await produceService.fetchPriceHistory(produceId: "LA1")
            
            let forecast = try await produceService.getForecast(produceId: "LA1")
            let nextDayPrice = forecast.trend == "Up" ? forecast.recentAverage * 1.05 : forecast.recentAverage * 0.95
            self.predictedData = [
                PricePredictionDto(date: "Next Day", predictedPrice: nextDayPrice)
            ]
            
            // 取得我的收藏與追蹤資料
            self.favorites = try await produceService.getFavorites()
        } catch {
            print("Error fetching dashboard data: \(error)")
        }
    }
}
