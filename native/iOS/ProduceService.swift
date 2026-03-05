import Foundation

class ProduceService {
    static let shared = ProduceService()
    // 使用真實的 App URL
    private let baseURL = "https://ais-dev-gyv3my74fwisdg5piudwph-424197195798.asia-east1.run.app/api/produce"
    
    // 使用真實的 Device ID (UUID)
    private var deviceId: String {
        if let uuid = UserDefaults.standard.string(forKey: "device_uuid") {
            return uuid
        } else {
            let uuid = UUID().uuidString
            UserDefaults.standard.set(uuid, forKey: "device_uuid")
            return uuid
        }
    }
    
    private init() {}
    
    // 邏輯修正：加入分頁與搜尋參數，並使用 Codable 自動將 JSON 轉為 Swift Struct (ProduceDto)
    // 解決問題：原本只回傳 Data，App 端需要自己手動解析 JSON，容易出錯且欄位不一致。
    func fetchProduceData(keyword: String = "", page: Int = 1, completion: @escaping (Result<PaginatedResponse<ProduceDto>, Error>) -> Void) {
        let encodedKeyword = keyword.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "\(baseURL)/daily-prices?keyword=\(encodedKeyword)&page=\(page)&pageSize=20"
        
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id") // 注入身分驗證 Header
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { 
                completion(.failure(error))
                return 
            }
            
            guard let data = data else { return }
            
            do {
                let decoder = JSONDecoder()
                // 自動將後端的 JSON 映射到 Swift 的 PaginatedResponse<ProduceDto>
                let result = try decoder.decode(PaginatedResponse<ProduceDto>.self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    func fetchPriceHistory(produceId: String, completion: @escaping (Result<Data, Error>) -> Void) {
        let urlString = "\(baseURL)/history/\(produceId)"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id") // 注入身分驗證 Header
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            if let data = data { completion(.success(data)) }
        }.resume()
    }
    
    // 新增功能：市場比價 (Market Comparison)
    func comparePrices(cropName: String, completion: @escaping (Result<[ProduceDto], Error>) -> Void) {
        let encodedCropName = cropName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "\(baseURL)/compare/\(encodedCropName)"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([ProduceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // 新增功能：價格預測與趨勢分析 (Price Forecasting)
    func getForecast(produceId: String, completion: @escaping (Result<Data, Error>) -> Void) {
        let urlString = "\(baseURL)/forecast/\(produceId)"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            if let data = data { completion(.success(data)) }
        }.resume()
    }
    
    // 新增功能：熱門交易農產品 (Top Volume Crops)
    func getTopVolumeCrops(completion: @escaping (Result<[ProduceDto], Error>) -> Void) {
        let urlString = "\(baseURL)/top-volume"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([ProduceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    // 邏輯修正：移除 body 中的 userId，因為已經放在 Header 中了
    func syncFavorite(produceId: String, targetPrice: Double, completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "\(baseURL)/favorites") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id") // 注入身分驗證 Header
        
        let body: [String: Any] = [
            "produceId": produceId,
            "targetPrice": targetPrice
        ]
        
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 || httpResponse.statusCode == 201 else {
                completion(false)
                return
            }
            completion(true)
        }.resume()
    }
    
    // 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts)
    func getFavorites(completion: @escaping (Result<[FavoriteAlertDto], Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/favorites") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([FavoriteAlertDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    // 新增功能：移除收藏 (Remove Favorite)
    func removeFavorite(produceId: String, completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "\(baseURL)/favorites/\(produceId)") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                completion(false)
                return
            }
            completion(true)
        }.resume()
    }

    // 新增功能：社群回報機制 (Community Retail Price)
    func reportCommunityPrice(priceDto: CommunityPriceDto, completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "\(baseURL)/community-price") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(deviceId, forHTTPHeaderField: "X-User-Id")
        
        do {
            let encoder = JSONEncoder()
            request.httpBody = try encoder.encode(priceDto)
        } catch {
            completion(false)
            return
        }
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 || httpResponse.statusCode == 201 else {
                completion(false)
                return
            }
            completion(true)
        }.resume()
    }

    func getCommunityPrices(cropCode: String, completion: @escaping (Result<[CommunityPriceDto], Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/community-price/\(cropCode)") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([CommunityPriceDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // 新增功能：當季盛產日曆 (Seasonal Crop Calendar)
    func getSeasonalCrops(completion: @escaping (Result<[SeasonalCropDto], Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/seasonal") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([SeasonalCropDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // 新增功能：價格異常警告 (Price Anomaly Detection)
    func getPriceAnomalies(completion: @escaping (Result<[PriceAnomalyDto], Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/anomalies") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([PriceAnomalyDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // 新增功能：天氣預警 (Weather Alerts)
    func getWeatherAlerts(completion: @escaping (Result<WeatherAlertDto, Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/weather-alerts") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode(WeatherAlertDto.self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // 新增功能：省錢食譜 (Budget Recipes)
    func getBudgetRecipes(completion: @escaping (Result<[BudgetRecipeDto], Error>) -> Void) {
        guard let url = URL(string: "\(baseURL)/budget-recipes") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            guard let data = data else { return }
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode([BudgetRecipeDto].self, from: data)
                completion(.success(result))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // Async/Await wrappers
    func getDailyPrices(keyword: String = "", page: Int = 1, pageSize: Int = 20) async throws -> PaginatedResponse<ProduceDto> {
        return try await withCheckedThrowingContinuation { continuation in
            fetchProduceData(keyword: keyword, page: page) { result in
                continuation.resume(with: result)
            }
        }
    }

    func getPriceAnomalies() async throws -> [PriceAnomalyDto] {
        return try await withCheckedThrowingContinuation { continuation in
            getPriceAnomalies { result in
                continuation.resume(with: result)
            }
        }
    }

    func getTopVolumeCrops() async throws -> [ProduceDto] {
        return try await withCheckedThrowingContinuation { continuation in
            getTopVolumeCrops { result in
                continuation.resume(with: result)
            }
        }
    }

    func fetchPriceHistory(produceId: String) async throws -> [HistoricalPriceDto] {
        return try await withCheckedThrowingContinuation { continuation in
            fetchPriceHistory(produceId: produceId) { result in
                switch result {
                case .success(let data):
                    do {
                        let decoder = JSONDecoder()
                        let dtos = try decoder.decode([HistoricalPriceDto].self, from: data)
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

    func getForecast(produceId: String) async throws -> PricePredictionResponse {
        return try await withCheckedThrowingContinuation { continuation in
            getForecast(produceId: produceId) { result in
                switch result {
                case .success(let data):
                    do {
                        let decoder = JSONDecoder()
                        let response = try decoder.decode(PricePredictionResponse.self, from: data)
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

    func getFavorites() async throws -> [FavoriteAlertDto] {
        return try await withCheckedThrowingContinuation { continuation in
            getFavorites { result in
                continuation.resume(with: result)
            }
        }
    }
}
