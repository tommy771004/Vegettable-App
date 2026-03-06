import Foundation
import Combine

// =============================================================================
// ProduceViewModel.swift — MVVM 架構中的 ViewModel 層
//
// 架構說明：
//   此 ViewModel 遵循 MVVM 設計模式：
//   - View (SwiftUI) 只負責顯示，不包含任何業務邏輯
//   - ViewModel 持有所有「狀態」(@Published)，並向 ProduceService 發出請求
//   - @MainActor 確保所有 @Published 變數的更新都在主執行緒 (UI Thread) 執行，
//     避免因跨執行緒更新 UI 導致 Runtime Crash
//
// 資料流向：
//   View → ViewModel.fetchDashboardData() → ProduceService (async/await) → 後端 API
//   → [解析成 DTO] → ViewModel.@Published 更新 → View 自動重新渲染
// =============================================================================

// MARK: - 資源狀態包裝器
/// Resource<T> 是三態狀態機，代表任一非同步資料請求的三種可能狀態：
/// - loading：正在載入中，UI 顯示骨架屏 (Skeleton Loader)
/// - success：載入成功，UI 顯示實際資料
/// - failure：載入失敗，UI 顯示錯誤訊息 + 重試按鈕
enum Resource<T> {
    case loading
    case success(T)
    case failure(Error)
}

// MARK: - 圖表專用本地 DTO
/// 歷史價格資料點，供 Swift Charts 折線圖使用
/// 注意：id 使用 UUID() 是因為 Charts 需要 Identifiable，
///      後端回傳的歷史資料沒有唯一 id 欄位
struct HistoricalPriceDto: Identifiable, Decodable {
    let id = UUID()
    let date: String     // 日期字串 (例："2024-01-15")
    let avgPrice: Double // 該日均價 (元/公斤)

    // CodingKeys 確保 UUID 的 id 不被 JSONDecoder 嘗試解析 (因為 JSON 中沒有 id 欄位)
    enum CodingKeys: String, CodingKey {
        case date, avgPrice
    }
}

/// 價格預測資料點，供折線圖顯示「未來預測」段落使用
/// 此 struct 為純本地計算，不從後端直接解析
struct PricePredictionDto: Identifiable {
    let id = UUID()
    let date: String             // 預測日期標籤 (例："Next Day")
    let predictedPrice: Double   // 預測均價 (元/公斤)
}

// 注意：FavoriteAlertDto 已移至 ProduceDto.swift 集中管理，
//       此處不再重複定義，以避免 Swift 編譯器 "redefinition" 錯誤。

// MARK: - ViewModel
/// ProduceViewModel 是所有 UI 畫面的核心資料中心
/// 使用 @MainActor 確保執行緒安全
@MainActor
class ProduceViewModel: ObservableObject {

    // MARK: Published States
    // 每個 @Published 對應一個 API 請求的三態狀態
    // 當任一狀態改變，SwiftUI 會自動重新渲染有訂閱此狀態的 View

    /// 我的收藏清單 + 是否達到目標提醒價格
    @Published var favorites: Resource<[FavoriteAlertDto]> = .loading

    /// 今日農產品批發價格列表 (分頁，預設第 1 頁，每頁 20 筆)
    @Published var dailyPrices: Resource<[ProduceDto]> = .loading

    /// 今日交易量前 10 名的熱門農產品
    @Published var topVolume: Resource<[ProduceDto]> = .loading

    /// 單日漲幅超過 50% 的價格異常警告清單
    @Published var anomalies: Resource<[PriceAnomalyDto]> = .loading

    /// 高麗菜 (LA1) 歷史 30 天價格資料，供折線圖展示
    @Published var historicalData: Resource<[HistoricalPriceDto]> = .loading

    /// 明日預測價格，由 7 日均線趨勢計算得出
    @Published var predictedData: Resource<[PricePredictionDto]> = .loading

    // MARK: Dependencies
    /// 透過 Protocol Injection 注入服務，方便單元測試時替換成 MockProduceService
    private let produceService: ProduceServiceProtocol

    // MARK: Init
    /// 初始化時自動觸發首頁資料載入
    /// - Parameter service: 實作 ProduceServiceProtocol 的服務實例，預設為正式 API
    init(service: ProduceServiceProtocol = ProduceService.shared) {
        self.produceService = service
        Task {
            // 在初始化完成後立即在背景非同步載入資料
            await fetchDashboardData()
        }
    }

    // MARK: - 載入首頁所有資料
    /// 並行或循序載入首頁所需的所有資料區塊
    /// 每個 do-catch 區塊獨立處理錯誤，確保某一 API 失敗不影響其他區塊
    func fetchDashboardData() async {
        // 重置所有狀態為 loading，讓 UI 顯示骨架屏
        self.anomalies = .loading
        self.topVolume = .loading
        self.dailyPrices = .loading
        self.historicalData = .loading
        self.predictedData = .loading
        self.favorites = .loading

        // 載入「價格異常警告」：漲幅超過 50% 的農產品
        do {
            let anomaliesData = try await produceService.getPriceAnomalies()
            self.anomalies = .success(anomaliesData)
        } catch {
            self.anomalies = .failure(error)
        }

        // 載入「今日熱門交易」：交易量前 10 名
        do {
            let topVolumeData = try await produceService.getTopVolumeCrops()
            self.topVolume = .success(topVolumeData)
        } catch {
            self.topVolume = .failure(error)
        }

        // 載入「今日菜價」：第 1 頁，每頁 20 筆
        do {
            let response = try await produceService.getDailyPrices(keyword: "", page: 1, pageSize: 20)
            self.dailyPrices = .success(response.data)
        } catch {
            self.dailyPrices = .failure(error)
        }

        // 載入「高麗菜 7 日趨勢圖表」
        // 選擇高麗菜 (LA1) 作為首頁示範，因為是台灣消費量最大的葉菜類
        do {
            let history = try await produceService.fetchPriceHistory(produceId: "LA1")
            self.historicalData = .success(history)

            // 根據趨勢方向計算明日預測價格：
            //   Up → 明日預測比近 7 日均價高 5%
            //   Down / Stable → 明日預測比近 7 日均價低 5%
            let forecast = try await produceService.getForecast(produceId: "LA1")
            let nextDayPrice = forecast.trend == "Up"
                ? forecast.recentAverage * 1.05
                : forecast.recentAverage * 0.95
            self.predictedData = .success([
                PricePredictionDto(date: "Next Day", predictedPrice: nextDayPrice)
            ])
        } catch {
            self.historicalData = .failure(error)
            self.predictedData = .failure(error)
        }

        // 載入「我的收藏」：使用者追蹤的農產品及是否達到目標價
        do {
            let favoritesData = try await produceService.getFavorites()
            self.favorites = .success(favoritesData)
        } catch {
            self.favorites = .failure(error)
        }
    }
}
