import Foundation

class ProduceService {
    static let shared = ProduceService()
    private let baseURL = "https://api.yourbackend.com/api/produce"
    
    private init() {}
    
    // 邏輯修正：加入分頁與搜尋參數，並使用 Codable 自動將 JSON 轉為 Swift Struct (ProduceDto)
    // 解決問題：原本只回傳 Data，App 端需要自己手動解析 JSON，容易出錯且欄位不一致。
    func fetchProduceData(keyword: String = "", page: Int = 1, completion: @escaping (Result<PaginatedResponse<ProduceDto>, Error>) -> Void) {
        let encodedKeyword = keyword.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlString = "\(baseURL)/daily-prices?keyword=\(encodedKeyword)&page=\(page)&pageSize=20"
        
        guard let url = URL(string: urlString) else { return }
        
        URLSession.shared.dataTask(with: url) { data, response, error in
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
        
        URLSession.shared.dataTask(with: url) { data, response, error in
            if let error = error { completion(.failure(error)); return }
            if let data = data { completion(.success(data)) }
        }.resume()
    }
    
    func syncFavorite(userId: String, produceId: String, targetPrice: Double, completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "\(baseURL)/favorites") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "userId": userId,
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
