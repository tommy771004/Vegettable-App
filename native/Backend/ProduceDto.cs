namespace ProduceApi.Models
{
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
