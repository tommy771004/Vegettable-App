import Foundation

class ProduceService {
    static let shared = ProduceService()
    private let baseURL = "https://api.yourbackend.com/api/produce"
    
    // 模擬使用者的 Device ID 或 UUID
    private let deviceId = "user-device-uuid-12345"
    
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
}
