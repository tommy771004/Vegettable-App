using System.Text.Json.Serialization;

namespace ProduceApi.Models
{
    // 用於解析政府 API 的 DTO
    public class MoaProduceDto
    {
        [JsonPropertyName("作物代號")]
        public string CropCode { get; set; }

        [JsonPropertyName("作物名稱")]
        public string CropName { get; set; }

        [JsonPropertyName("市場代號")]
        public string MarketCode { get; set; }

        [JsonPropertyName("市場名稱")]
        public string MarketName { get; set; }

        [JsonPropertyName("平均價")]
        public double AvgPrice { get; set; }

        [JsonPropertyName("交易量")]
        public double TransQuantity { get; set; }

        [JsonPropertyName("交易日期")]
        public string Date { get; set; }
    }

    // 統一的資料傳輸物件 (DTO)，確保前後端欄位完全一致
    public class ProduceDto
    {
        public string CropCode { get; set; }
        public string CropName { get; set; }
        public string MarketCode { get; set; }
        public string MarketName { get; set; }
        public double AvgPrice { get; set; }
        public double TransQuantity { get; set; }
        public string Date { get; set; }

        // [Bug Fix] 新增 id 欄位：對應 iOS ProduceDto.id 與 Android ProduceDto.stableId
        // 確保 test-api.sh 可以從 daily-prices 回應中解析出 produceId，
        // 進而測試 history/{id} 與 forecast/{id} 端點。
        public string Id => $"{CropCode}-{MarketCode}-{Date}";
    }

    public class PaginatedResponse<T>
    {
        public int CurrentPage { get; set; }
        public int TotalPages { get; set; }
        public int TotalItems { get; set; }
        public System.Collections.Generic.List<T> Data { get; set; }
    }

    // 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts) 的 DTO
    public class FavoriteAlertDto
    {
        public string ProduceId { get; set; }
        public string ProduceName { get; set; }
        public double TargetPrice { get; set; }
        public double CurrentPrice { get; set; }
        public bool IsAlertTriggered { get; set; }
    }

    // 新增功能：社群回報機制 (Community Retail Price) 的 DTO
    public class CommunityPriceDto
    {
        public string CropCode { get; set; }
        public string CropName { get; set; }
        public string MarketName { get; set; }
        public double RetailPrice { get; set; }
        public string ReportDate { get; set; }
    }

    // 新增功能：當季盛產日曆 (Seasonal Crop Calendar) 的 DTO
    public class SeasonalCropDto
    {
        public string CropCode { get; set; }
        public string CropName { get; set; }
        public string Season { get; set; }
        public string Description { get; set; }
    }

    // 使用者貢獻統計 DTO (GET /api/produce/user-stats)
    // [Bug Fix] 原本 GetUserStats 直接回傳 UserStat 實體，
    //   1. 暴露敏感欄位 FcmToken、UserId
    //   2. 缺少 reportCount 欄位，iOS/Android UserStatsDto 需要此欄位
    // 修正：新增此 DTO，回傳時僅包含客戶端所需欄位。
    public class UserStatsDto
    {
        public int ContributionPoints { get; set; }
        public string Level { get; set; }
        public int ReportCount { get; set; }
    }

    // 市場比價 DTO (GET /api/produce/compare/{cropName})
    // 對應 iOS MarketComparisonDto 與 Android MarketCompareDto
    public class MarketCompareDto
    {
        public string MarketName { get; set; }
        public double AvgPrice { get; set; }
        public double TransQuantity { get; set; }
        public string Date { get; set; }
    }

    // 新增功能：價格異常警告 (Price Anomaly Detection)
    public class PriceAnomalyDto
    {
        public string CropCode { get; set; }
        public string CropName { get; set; }
        public double CurrentPrice { get; set; }
        public double PreviousPrice { get; set; }
        public double IncreasePercentage { get; set; }
        public string AlertMessage { get; set; }
    }
}
