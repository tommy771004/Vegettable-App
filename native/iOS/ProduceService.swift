import Foundation

// =============================================================================
// ProduceService.swift — 網路層：所有後端 API 呼叫的唯一入口
//
// 架構說明：
//   此檔案負責所有 HTTP 通訊，採用「Service + Repository」雙層架構：
//   - ProduceService (本檔案)：負責 HTTP 請求組裝、認證 Header 注入、JSON 解碼
//   - ProduceRepository：負責「先讀快取、失敗再打 API」的離線優先策略
//
// JWT 認證整合 (本次更新)：
//   原本：每個 Request 帶 "X-User-Id: {deviceId}" Header (任何人可偽造)
//   現在：先呼叫 POST /auth/token 換取 JWT → 所有 Request 帶 "Authorization: Bearer {token}"
//
//   JWT 生命週期管理：
//   1. App 首次啟動 → 呼叫 ensureJwtToken() 以 deviceId 換取 JWT → 存入 UserDefaults
//   2. 之後每次 API 請求：從 UserDefaults 讀取 JWT → 加到 Authorization Header
//   3. 若收到 401 Unauthorized → 清除舊 Token → 重新呼叫 ensureJwtToken() 取得新 Token
//
// 設計決策 — Completion Handler vs async/await：
//   目前同時提供兩種介面：
//   - Completion Handler (舊式)：維持向下相容，UI 畫面仍可使用
//   - async/await (新式)：透過 withCheckedThrowingContinuation 包裝
//   未來建議全面遷移到 async/await，去除 Completion Handler 層。
// =============================================================================

// MARK: - 服務協定 (Testability)
/// ProduceServiceProtocol 定義所有非同步 API 方法的介面
/// 實際使用 ProduceService；單元測試時可替換成 MockProduceService
protocol ProduceServiceProtocol {
    func getDailyPrices(keyword: String, page: Int, pageSize: Int) async throws -> PaginatedResponse<ProduceDto>
    func getPriceAnomalies() async throws -> [PriceAnomalyDto]
    func getTopVolumeCrops() async throws -> [ProduceDto]
    func fetchPriceHistory(produceId: String) async throws -> [HistoricalPriceDto]
    func getForecast(produceId: String) async throws -> PricePredictionResponse
    func getFavorites() async throws -> [FavoriteAlertDto]
}

// MARK: - JWT Token Response
/// POST /auth/token 的回應 Model
private struct TokenResponse: Decodable {
    let token: String       // JWT 字串 (eyJhbGc...)
    let expiresAt: String   // 過期時間 ISO8601 字串
    let deviceId: String    // 確認使用的 DeviceId
}

// MARK: - ProduceService
class ProduceService: ProduceServiceProtocol {

    // MARK: Singleton
    /// 全域單例，整個 App 共用同一個 URLSession 連線池
    static let shared = ProduceService()

    // MARK: Properties
    /// 後端 API Base URL，從 Configuration.swift 讀取 (支援開發/生產環境切換)
    private let baseURL = Configuration.apiBaseUrl

    /// JWT 相關 UserDefaults 鍵名常數
    private enum StorageKeys {
        static let deviceUUID  = "device_uuid"   // 裝置唯一識別碼
        static let jwtToken    = "jwt_token"     // 儲存的 JWT 字串
    }

    // MARK: - 裝置 ID
    /// 裝置唯一識別碼：首次取得後永久儲存在 UserDefaults
    /// 用途：呼叫 /auth/token 時作為使用者身份識別
    /// 生產環境建議改用 iOS Keychain 儲存，更安全不易被清除
    private var deviceId: String {
        if let uuid = UserDefaults.standard.string(forKey: StorageKeys.deviceUUID) {
            return uuid
        }
        let uuid = UUID().uuidString
        UserDefaults.standard.set(uuid, forKey: StorageKeys.deviceUUID)
        return uuid
    }

    /// JWT Token：從 UserDefaults 讀取，若不存在則返回 nil
    private var jwtToken: String? {
        get { UserDefaults.standard.string(forKey: StorageKeys.jwtToken) }
        set { UserDefaults.standard.set(newValue, forKey: StorageKeys.jwtToken) }
    }

    private init() {}

    // MARK: - JWT Token 管理

    /// 確保 JWT Token 有效：若無 Token 則自動向後端申請新的
    /// 建議在 AppDelegate 或 App.init 中呼叫一次，確保 Token 在所有 API 呼叫前準備好
    func ensureJwtToken() async {
        guard jwtToken == nil else { return } // 已有 Token，不重複申請
        await refreshJwtToken()
    }

    /// 向後端 /auth/token 申請新的 JWT Token 並儲存
    private func refreshJwtToken() async {
        let authURL = baseURL.replacingOccurrences(of: "/api/produce", with: "/auth/token")
        guard let url = URL(string: authURL) else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Body: { "deviceId": "UUID字串" }
        let body: [String: Any] = ["deviceId": deviceId]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let tokenResponse = try JSONDecoder().decode(TokenResponse.self, from: data)
            // 將 JWT 存入 UserDefaults，後續所有請求直接讀取
            jwtToken = tokenResponse.token
        } catch {
            // Token 申請失敗，記錄錯誤但不 crash App
            // 後續 API 請求會因 401 失敗並觸發重新申請
            print("[ProduceService] Failed to get JWT token: \(error)")
        }
    }

    // MARK: - 請求建構輔助方法

    /// 建立帶有 JWT 認證 Header 的 URLRequest
    /// - Parameters:
    ///   - urlString: 完整的 API URL 字串
    ///   - method: HTTP 方法，預設 "GET"
    /// - Returns: 配置好 Authorization Header 的 URLRequest，或 nil (URL 無效時)
    private func makeAuthenticatedRequest(urlString: String, method: String = "GET") -> URLRequest? {
        guard let url = URL(string: urlString) else { return nil }

        var request = URLRequest(url: url)
        request.httpMethod = method

        // [JWT 更新] 使用 Bearer Token 替代原本的 X-User-Id Header
        // Bearer Token 由後端 HMAC-SHA256 簽章，無法被偽造
        if let token = jwtToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        return request
    }

    // MARK: - Completion Handler API (舊式，供部分 UI 直接使用)

    /// 取得今日農產品批發價格列表 (支援分頁與關鍵字搜尋)
    /// - Parameters:
    ///   - keyword: 搜尋關鍵字 (例："番茄")，空字串表示不過濾
    ///   - page: 頁碼，從 1 開始
    ///   - pageSize: 每頁筆數，預設 20
    ///   - completion: 成功回傳 PaginatedResponse<ProduceDto>，失敗回傳 Error
    func fetchProduceData(keyword: String = "", page: Int = 1, pageSize: Int = 20,
                          completion: @escaping (Result<PaginatedResponse<ProduceDto>, Error>) -> Void) {
        let encodedKeyword = keyword.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "\(baseURL)/daily-prices?keyword=\(encodedKeyword)&page=\(page)&pageSize=\(pageSize)"

        guard let request = makeAuthenticatedRequest(urlString: urlString) else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            // 若收到 401，清除 Token 後通知 UI 重新整理（下次呼叫會重新申請 Token）
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 401 {
                self.jwtToken = nil
            }
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode(PaginatedResponse<ProduceDto>.self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得指定農產品的歷史 30 天價格記錄 (供折線圖使用)
    func fetchPriceHistory(produceId: String, completion: @escaping (Result<Data, Error>) -> Void) {
        let urlString = "\(baseURL)/history/\(produceId)"
        guard let request = makeAuthenticatedRequest(urlString: urlString) else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            if let data = data { completion(.success(data)) }
        }.resume()
    }

    /// 查詢指定農產品在全台各市場的今日價格，由低到高排序 (市場比價功能)
    func comparePrices(cropName: String, completion: @escaping (Result<[ProduceDto], Error>) -> Void) {
        let encodedCropName = cropName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "\(baseURL)/compare/\(encodedCropName)"
        guard let request = makeAuthenticatedRequest(urlString: urlString) else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([ProduceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得農產品 7 日移動平均線趨勢預測 (原始 Data 版本，供內部包裝使用)
    func getForecast(produceId: String, completion: @escaping (Result<Data, Error>) -> Void) {
        let urlString = "\(baseURL)/forecast/\(produceId)"
        guard let request = makeAuthenticatedRequest(urlString: urlString) else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            if let data = data { completion(.success(data)) }
        }.resume()
    }

    /// 取得今日交易量前 10 名農產品
    func getTopVolumeCrops(completion: @escaping (Result<[ProduceDto], Error>) -> Void) {
        let urlString = "\(baseURL)/top-volume"
        guard let request = makeAuthenticatedRequest(urlString: urlString) else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([ProduceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 新增或更新我的收藏 (設定目標提醒價格)
    /// - Parameters:
    ///   - produceId: 農產品代碼 (例："LA1" = 高麗菜)
    ///   - targetPrice: 目標提醒價格 (元/公斤)，達到此價格時後端會發 FCM 推播
    func syncFavorite(produceId: String, targetPrice: Double, completion: @escaping (Bool) -> Void) {
        guard var request = makeAuthenticatedRequest(urlString: "\(baseURL)/favorites", method: "POST") else { return }
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Body 只需要 produceId 和 targetPrice，UserId 已在 JWT Token 中
        let body: [String: Any] = ["produceId": produceId, "targetPrice": targetPrice]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        URLSession.shared.dataTask(with: request) { _, response, _ in
            guard let http = response as? HTTPURLResponse else { completion(false); return }
            completion(http.statusCode == 200 || http.statusCode == 201)
        }.resume()
    }

    /// 取得我的收藏清單及是否達到目標提醒價
    func getFavorites(completion: @escaping (Result<[FavoriteAlertDto], Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/favorites") else { return }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([FavoriteAlertDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 移除收藏
    func removeFavorite(produceId: String, completion: @escaping (Bool) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/favorites/\(produceId)", method: "DELETE") else { return }

        URLSession.shared.dataTask(with: request) { _, response, _ in
            guard let http = response as? HTTPURLResponse else { completion(false); return }
            completion(http.statusCode == 200)
        }.resume()
    }

    /// 社群零售價回報：回報在超市/傳統市場看到的實際零售價
    /// 每次成功回報後端會自動增加 5 點積分
    func reportCommunityPrice(priceDto: CommunityPriceDto, completion: @escaping (Bool) -> Void) {
        guard var request = makeAuthenticatedRequest(urlString: "\(baseURL)/community-price", method: "POST") else { return }
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        do {
            request.httpBody = try JSONEncoder().encode(priceDto)
        } catch {
            completion(false)
            return
        }

        URLSession.shared.dataTask(with: request) { _, response, _ in
            guard let http = response as? HTTPURLResponse else { completion(false); return }
            completion(http.statusCode == 200 || http.statusCode == 201)
        }.resume()
    }

    /// 查詢社群回報的零售價記錄
    func getCommunityPrices(cropCode: String, completion: @escaping (Result<[CommunityPriceDto], Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/community-price/\(cropCode)") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([CommunityPriceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得當季盛產農作物清單 (依當前月份過濾)
    func getSeasonalCrops(completion: @escaping (Result<[SeasonalCropDto], Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/seasonal") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([SeasonalCropDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得單日漲幅超過 50% 的價格異常農產品清單
    func getPriceAnomalies(completion: @escaping (Result<[PriceAnomalyDto], Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/anomalies") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([PriceAnomalyDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得颱風/豪大雨預警資訊 (後端即時抓取中央氣象署 RSS)
    func getWeatherAlerts(completion: @escaping (Result<WeatherAlertDto, Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/weather-alerts") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode(WeatherAlertDto.self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// 取得今日省錢食譜推薦 (根據今日價格跌幅最大的農產品自動配對)
    func getBudgetRecipes(completion: @escaping (Result<[BudgetRecipeDto], Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/budget-recipes") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([BudgetRecipeDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - ProduceServiceProtocol (async/await 包裝層)
    // 以下方法透過 withCheckedThrowingContinuation 將 Completion Handler 包裝成 async/await
    // 供 ProduceViewModel (使用 async/await 的 @MainActor 環境) 呼叫

    /// async/await 版本：取得今日批發價格 (供 ProduceViewModel 使用)
    func getDailyPrices(keyword: String = "", page: Int = 1, pageSize: Int = 20) async throws -> PaginatedResponse<ProduceDto> {
        try await withCheckedThrowingContinuation { continuation in
            fetchProduceData(keyword: keyword, page: page, pageSize: pageSize) {
                continuation.resume(with: $0)
            }
        }
    }

    /// async/await 版本：取得價格異常列表
    func getPriceAnomalies() async throws -> [PriceAnomalyDto] {
        try await withCheckedThrowingContinuation { continuation in
            getPriceAnomalies { continuation.resume(with: $0) }
        }
    }

    /// async/await 版本：取得熱門交易農產品
    func getTopVolumeCrops() async throws -> [ProduceDto] {
        try await withCheckedThrowingContinuation { continuation in
            getTopVolumeCrops { continuation.resume(with: $0) }
        }
    }

    /// async/await 版本：取得歷史價格 (內部先解析 Data 再返回 DTO)
    func fetchPriceHistory(produceId: String) async throws -> [HistoricalPriceDto] {
        try await withCheckedThrowingContinuation { continuation in
            fetchPriceHistory(produceId: produceId) { result in
                switch result {
                case .success(let data):
                    do {
                        let dtos = try JSONDecoder().decode([HistoricalPriceDto].self, from: data)
                        continuation.resume(returning: dtos)
                    } catch {
                        continuation.resume(throwing: error)
                    }
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// async/await 版本：取得趨勢預測
    func getForecast(produceId: String) async throws -> PricePredictionResponse {
        try await withCheckedThrowingContinuation { continuation in
            getForecast(produceId: produceId) { result in
                switch result {
                case .success(let data):
                    do {
                        let response = try JSONDecoder().decode(PricePredictionResponse.self, from: data)
                        continuation.resume(returning: response)
                    } catch {
                        continuation.resume(throwing: error)
                    }
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// async/await 版本：取得我的收藏列表
    func getFavorites() async throws -> [FavoriteAlertDto] {
        try await withCheckedThrowingContinuation { continuation in
            getFavorites { continuation.resume(with: $0) }
        }
    }

    // MARK: - 使用者統計

    /// 取得使用者貢獻點數、等級與回報次數
    /// 對應後端 GET /api/produce/user-stats
    func getUserStats(completion: @escaping (Result<UserStatsDto, Error>) -> Void) {
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/user-stats") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode(UserStatsDto.self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// async/await 版本：取得使用者統計
    func getUserStats() async throws -> UserStatsDto {
        try await withCheckedThrowingContinuation { continuation in
            getUserStats { continuation.resume(with: $0) }
        }
    }

    // MARK: - 市場比價

    /// 查詢指定農產品在各市場的批發均價
    /// 對應後端 GET /api/produce/compare/{cropName}
    /// - Parameter cropName: 農產品名稱（例："高麗菜"），後端進行模糊比對
    func getMarketComparison(cropName: String, completion: @escaping (Result<[MarketComparisonDto], Error>) -> Void) {
        let encoded = cropName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? cropName
        guard let request = makeAuthenticatedRequest(urlString: "\(baseURL)/compare/\(encoded)") else { return }

        URLSession.shared.dataTask(with: request) { data, _, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let result = try JSONDecoder().decode([MarketComparisonDto].self, from: data)
                completion(.success(result.sorted { $0.avgPrice < $1.avgPrice }))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    /// async/await 版本：取得市場比價清單（已由低到高排序）
    func getMarketComparison(cropName: String) async throws -> [MarketComparisonDto] {
        try await withCheckedThrowingContinuation { continuation in
            getMarketComparison(cropName: cropName) { continuation.resume(with: $0) }
        }
    }
}
