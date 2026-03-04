import Foundation
import SwiftData

// 新增功能：離線快取機制 (Offline Caching)
// SwiftData Model，用於在無網路時快取「今日熱門農產品」與「我的收藏」
@Model
class ProduceModel {
    @Attribute(.unique) var id: String // 組合鍵，例如 cacheType + cropCode
    var cropCode: String
    var cropName: String
    var marketCode: String
    var marketName: String
    var avgPrice: Double
    var transQuantity: Double
    var date: String
    
    // 標記此資料屬於哪種快取 (例如: "TOP_VOLUME", "FAVORITE")
    var cacheType: String
    
    init(cropCode: String, cropName: String, marketCode: String, marketName: String, avgPrice: Double, transQuantity: Double, date: String, cacheType: String) {
        self.id = "\(cacheType)_\(cropCode)_\(marketCode)"
        self.cropCode = cropCode
        self.cropName = cropName
        self.marketCode = marketCode
        self.marketName = marketName
        self.avgPrice = avgPrice
        self.transQuantity = transQuantity
        self.date = date
        self.cacheType = cacheType
    }
}
