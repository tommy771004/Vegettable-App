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
    }

    public class PaginatedResponse<T>
    {
        public int CurrentPage { get; set; }
        public int TotalPages { get; set; }
        public int TotalItems { get; set; }
        public System.Collections.Generic.List<T> Data { get; set; }
    }
}
