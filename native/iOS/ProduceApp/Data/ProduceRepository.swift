import Foundation
import SwiftData

// 1. SwiftData Model (本地端快取)
@Model
class ProduceEntity {
    @Attribute(.unique) var cropCode: String
    var cropName: String
    var averagePrice: Double
    var date: String
    var marketName: String
    
    init(cropCode: String, cropName: String, averagePrice: Double, date: String, marketName: String) {
        self.cropCode = cropCode
        self.cropName = cropName
        self.averagePrice = averagePrice
        self.date = date
        self.marketName = marketName
    }
}

// 2. API DTO (政府 Open Data)
struct ProduceDTO: Codable {
    let 作物代號: String
    let 作物名稱: String
    let 平均價: Double
    let 交易日期: String
    let 市場名稱: String
}

// 3. Repository (Offline-First 邏輯)
@MainActor
class ProduceRepository: ObservableObject {
    private let modelContext: ModelContext
    
    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    // 從政府 API 取得最新資料並寫入 SwiftData
    func refreshData() async {
        guard let url = URL(string: "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?$top=100") else { return }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decodedData = try JSONDecoder().decode([ProduceDTO].self, from: data)
            
            // 清除舊資料 (簡化範例)
            try modelContext.delete(model: ProduceEntity.self)
            
            // 寫入新資料
            for dto in decodedData {
                let entity = ProduceEntity(
                    cropCode: dto.作物代號,
                    cropName: dto.作物名稱,
                    averagePrice: dto.平均價,
                    date: dto.交易日期,
                    marketName: dto.市場名稱
                )
                modelContext.insert(entity)
            }
            
            try modelContext.save()
            
        } catch {
            print("API 請求或資料庫寫入失敗: \(error)")
            // 失敗時，UI 依然會透過 @Query 讀取 SwiftData 內的舊資料 (Offline-First)
        }
    }
}
