import Foundation

class ProduceRepository {
    static let shared = ProduceRepository()
    
    private let apiService = ProduceService.shared
    private let localDb = ProduceCoreDataStore.shared
    
    private init() {}
    
    // 邏輯修正與優化 2：Repository Pattern (單一資料來源模式)
    // 解決問題：之前 API (ProduceService) 和本地資料庫 (ProduceCoreDataStore) 是分開的。
    // 手機在沒網路時會直接 Crash。現在透過 Repository 統一管理：先打 API，成功就存入本地 DB；失敗就讀取本地 DB。
    func getDailyPrices(keyword: String = "", page: Int = 1, completion: @escaping (Result<PaginatedResponse<ProduceDto>, Error>) -> Void) {
        
        // 1. 向後端 API 請求最新資料
        apiService.fetchProduceData(keyword: keyword, page: page) { result in
            switch result {
            case .success(let response):
                // 2. 成功取得資料後，非同步寫入 CoreData 本地資料庫 (快取)
                self.localDb.saveProduceList(response.data)
                
                // 3. 將最新資料回傳給 UI (ViewModel / ViewController)
                completion(.success(response))
                
            case .failure(let error):
                // 4. 如果斷網或 API 伺服器掛掉，改從 CoreData 讀取最後一次的快取資料 (離線模式)
                if let cachedData = self.localDb.getProduceList(keyword: keyword, page: page) {
                    let offlineResponse = PaginatedResponse(currentPage: page, totalPages: 1, totalItems: cachedData.count, data: cachedData)
                    completion(.success(offlineResponse))
                } else {
                    completion(.failure(error))
                }
            }
        }
    }
}
