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
            
            // 模擬取得高麗菜的歷史與預測資料 (供圖表展示)
            self.historicalData = [
                HistoricalPriceDto(date: "10/01", avgPrice: 22.5),
                HistoricalPriceDto(date: "10/02", avgPrice: 24.0),
                HistoricalPriceDto(date: "10/03", avgPrice: 23.5),
                HistoricalPriceDto(date: "10/04", avgPrice: 28.0),
                HistoricalPriceDto(date: "10/05", avgPrice: 35.0) // 暴漲點
            ]
            self.predictedData = [
                PricePredictionDto(date: "10/06", predictedPrice: 33.0),
                PricePredictionDto(date: "10/07", predictedPrice: 30.0),
                PricePredictionDto(date: "10/08", predictedPrice: 28.5)
            ]
            
            // 模擬取得我的收藏與追蹤資料
            self.favorites = [
                FavoriteAlertDto(cropCode: "LA1", cropName: "甘藍 (初秋)", currentPrice: 22.5, targetPrice: 25.0, isReached: true), // 已達標
                FavoriteAlertDto(cropCode: "FJ1", cropName: "番茄 (黑柿)", currentPrice: 45.0, targetPrice: 35.0, isReached: false),
                FavoriteAlertDto(cropCode: "SE1", cropName: "青蔥 (粉蔥)", currentPrice: 120.0, targetPrice: 80.0, isReached: false)
            ]
        } catch {
            print("Error fetching dashboard data: \(error)")
        }
    }
}
