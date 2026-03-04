import Foundation

@MainActor
class ProduceViewModel: ObservableObject {
    @Published var items: [ProduceItem] = []
    @Published var markets: [String] = ["台北一"] // 預設市場列表
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    
    init() {
        Task { 
            await fetchMarkets()
            await fetchData(for: "台北一") 
        }
    }
    
    func fetchMarkets() async {
        guard let url = URL(string: "http://localhost:5000/api/markets") else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decodedMarkets = try JSONDecoder().decode([String].self, from: data)
            self.markets = decodedMarkets
        } catch {
            print("無法載入市場列表：\(error.localizedDescription)")
        }
    }
    
    func fetchData(for market: String) async {
        isLoading = true
        errorMessage = nil
        
        // 將市場名稱進行 URL 編碼
        guard let encodedMarket = market.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "http://localhost:5000/api/produce?market=\(encodedMarket)") else { return }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decodedData = try JSONDecoder().decode([ProduceItem].self, from: data)
            self.items = decodedData
        } catch {
            self.errorMessage = "載入失敗：\(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func fetchHistory(for id: String, currentPrice: Double) async -> [PriceHistory] {
        guard let url = URL(string: "http://localhost:5000/api/produce/\(id)/history?currentPrice=\(currentPrice)") else { return [] }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decodedHistory = try JSONDecoder().decode([PriceHistory].self, from: data)
            return decodedHistory
        } catch {
            print("無法載入歷史價格：\(error.localizedDescription)")
            return []
        }
    }
}
