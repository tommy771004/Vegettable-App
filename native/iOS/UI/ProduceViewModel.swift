import Foundation
import Combine

enum Resource<T> {
    case loading
    case success(T)
    case failure(Error)
}

// 補充 DTO 定義 (供圖表使用)
struct HistoricalPriceDto: Identifiable, Decodable {
    let id = UUID()
    let date: String
    let avgPrice: Double
    
    enum CodingKeys: String, CodingKey {
        case date, avgPrice
    }
}

struct PricePredictionDto: Identifiable {
    let id = UUID()
    let date: String
    let predictedPrice: Double
}

struct FavoriteAlertDto: Identifiable, Decodable {
    let id = UUID()
    let cropCode: String
    let cropName: String
    let currentPrice: Double
    let targetPrice: Double
    let isReached: Bool
    
    enum CodingKeys: String, CodingKey {
        case cropCode, cropName, currentPrice, targetPrice, isReached
    }
}

@MainActor
class ProduceViewModel: ObservableObject {
    @Published var favorites: Resource<[FavoriteAlertDto]> = .loading
    @Published var dailyPrices: Resource<[ProduceDto]> = .loading
    @Published var topVolume: Resource<[ProduceDto]> = .loading
    @Published var anomalies: Resource<[PriceAnomalyDto]> = .loading
    
    // 新增：歷史價格與預測資料
    @Published var historicalData: Resource<[HistoricalPriceDto]> = .loading
    @Published var predictedData: Resource<[PricePredictionDto]> = .loading
    
    private let produceService: ProduceServiceProtocol
    
    init(service: ProduceServiceProtocol = ProduceService.shared) {
        self.produceService = service
        Task {
            await fetchDashboardData()
        }
    }
    
    func fetchDashboardData() async {
        // Reset states to loading
        self.anomalies = .loading
        self.topVolume = .loading
        self.dailyPrices = .loading
        self.historicalData = .loading
        self.predictedData = .loading
        self.favorites = .loading
        
        do {
            let anomaliesData = try await produceService.getPriceAnomalies()
            self.anomalies = .success(anomaliesData)
        } catch {
            self.anomalies = .failure(error)
        }
        
        do {
            let topVolumeData = try await produceService.getTopVolumeCrops()
            self.topVolume = .success(topVolumeData)
        } catch {
            self.topVolume = .failure(error)
        }
        
        do {
            let response = try await produceService.getDailyPrices(keyword: "", page: 1, pageSize: 20)
            self.dailyPrices = .success(response.data)
        } catch {
            self.dailyPrices = .failure(error)
        }
        
        do {
            // 取得高麗菜的歷史與預測資料 (供圖表展示)
            let history = try await produceService.fetchPriceHistory(produceId: "LA1")
            self.historicalData = .success(history)
            
            let forecast = try await produceService.getForecast(produceId: "LA1")
            let nextDayPrice = forecast.trend == "Up" ? forecast.recentAverage * 1.05 : forecast.recentAverage * 0.95
            self.predictedData = .success([
                PricePredictionDto(date: "Next Day", predictedPrice: nextDayPrice)
            ])
        } catch {
            self.historicalData = .failure(error)
            self.predictedData = .failure(error)
        }
        
        do {
            // 取得我的收藏與追蹤資料
            let favoritesData = try await produceService.getFavorites()
            self.favorites = .success(favoritesData)
        } catch {
            self.favorites = .failure(error)
        }
    }
}
