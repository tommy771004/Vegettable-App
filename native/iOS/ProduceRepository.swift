import Foundation

// =============================================================================
// ProduceRepository.swift — Repository Pattern：離線優先 (Offline-First) 資料層
//
// 架構說明：
//   Repository Pattern 在 Service 和 ViewModel 之間加入一個「資料協調層」：
//
//   ┌──────────────────────────────────────────────────────────────┐
//   │  ProduceViewModel (UI 狀態)                                  │
//   │       ↓ 呼叫                                                  │
//   │  ProduceRepository (本檔案) ← 資料協調層                     │
//   │       ↓ 優先             ↓ 備援                               │
//   │  ProduceService (API)   ProduceCoreDataStore (本地快取)       │
//   └──────────────────────────────────────────────────────────────┘
//
// 離線優先策略 (Offline-First)：
//   1. 優先呼叫後端 API 取得最新資料
//   2. 成功後：將資料寫入 CoreData 本地快取 (覆蓋舊資料)
//   3. 失敗後 (無網路 / 伺服器錯誤)：讀取 CoreData 快取，確保 App 仍可運作
//
// 為何使用 Repository Pattern 而非直接呼叫 Service：
//   - 單一測試點：Mock Repository 即可測試 ViewModel，無需啟動真實 API
//   - 離線容忍：網路中斷時 App 不 crash，顯示上次快取的資料
//   - 關注點分離：ViewModel 不需要知道資料來自 API 還是 CoreData
// =============================================================================

class ProduceRepository {

    // MARK: - Singleton
    static let shared = ProduceRepository()

    // MARK: - Dependencies
    /// 網路層：負責所有 HTTP 請求 (含 JWT 認證)
    private let apiService = ProduceService.shared

    /// 本地快取層：使用 CoreData 儲存離線資料
    private let localDb = ProduceCoreDataStore.shared

    private init() {}

    // MARK: - 今日批發價格 (Offline-First)

    /// 取得今日農產品批發價格，支援關鍵字搜尋與分頁
    ///
    /// 執行流程：
    ///   1. 呼叫 ProduceService 打後端 API
    ///   2. 成功 → 存入 CoreData 快取 → 回傳最新資料給 UI
    ///   3. 失敗 → 從 CoreData 讀取上次快取 → 以「離線模式」回傳給 UI
    ///   4. 快取也沒有 → 回傳原始錯誤，讓 UI 顯示錯誤畫面
    ///
    /// - Parameters:
    ///   - keyword: 搜尋關鍵字 (農產品名稱 or 市場名稱)
    ///   - page: 頁碼 (從 1 開始)
    ///   - completion: 成功回傳 PaginatedResponse<ProduceDto>，失敗回傳 Error
    func getDailyPrices(keyword: String = "", page: Int = 1,
                        completion: @escaping (Result<PaginatedResponse<ProduceDto>, Error>) -> Void) {
        // Step 1：嘗試從後端 API 取得最新資料
        apiService.fetchProduceData(keyword: keyword, page: page) { [weak self] result in
            guard let self = self else { return }

            switch result {
            case .success(let response):
                // Step 2：API 成功 → 非同步將資料寫入 CoreData 快取
                // 注意：儲存操作在背景執行，不阻塞 UI Thread
                self.localDb.saveProduceList(response.data)

                // 立即將最新資料回傳給 UI
                completion(.success(response))

            case .failure(let error):
                // Step 3：API 失敗 (例：無網路) → 嘗試讀取 CoreData 快取
                if let cachedData = self.localDb.getProduceList(keyword: keyword, page: page) {
                    // 找到快取資料 → 以「離線模式」包裝回傳
                    // TotalPages = 1 因為本地快取沒有完整的分頁元資料
                    let offlineResponse = PaginatedResponse<ProduceDto>(
                        currentPage: page,
                        totalPages: 1,
                        totalItems: cachedData.count,
                        data: cachedData
                    )
                    completion(.success(offlineResponse))
                } else {
                    // Step 4：連快取都沒有 → 回傳原始錯誤，UI 顯示錯誤畫面 + 重試按鈕
                    completion(.failure(error))
                }
            }
        }
    }
}
