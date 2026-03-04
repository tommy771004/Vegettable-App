using System.Text.Json;
using Microsoft.Extensions.Caching.Memory;

var builder = WebApplication.CreateBuilder(args);

// 註冊 HttpClient 與 MemoryCache
builder.Services.AddHttpClient();
builder.Services.AddMemoryCache();

// 允許 CORS (讓跨網域測試更方便)
builder.Services.AddCors(options => {
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();
app.UseCors();

// 支援多市場查詢的 API (預設為台北一)
app.MapGet("/api/produce", async (string? market, IHttpClientFactory clientFactory, IMemoryCache cache) =>
{
    // 如果沒有傳入 market 參數，預設使用「台北一」
    var targetMarket = string.IsNullOrWhiteSpace(market) ? "台北一" : market;
    
    // 根據市場名稱建立獨立的 Cache Key
    var cacheKey = $"ProduceData_{targetMarket}";
    
    // 1. 嘗試從快取讀取
    if (!cache.TryGetValue(cacheKey, out List<ProduceItem>? produceList))
    {
        produceList = new List<ProduceItem>();
        var client = clientFactory.CreateClient();
        
        try
        {
            // 2. 呼叫農業部真實 API (農產品交易行情)
            var response = await client.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx");
            using var document = JsonDocument.Parse(response);
            
            foreach (var element in document.RootElement.EnumerateArray())
            {
                var marketName = element.GetProperty("市場名稱").GetString();
                var cropName = element.GetProperty("作物名稱").GetString();
                
                // 根據傳入的市場名稱進行過濾，並過濾掉空名稱的防呆處理
                if (marketName == targetMarket && !string.IsNullOrWhiteSpace(cropName))
                {
                    var price = element.GetProperty("平均價").GetDouble();
                    var code = element.GetProperty("作物代號").GetString() ?? "";
                    
                    // 簡單分類邏輯 (F開頭通常是水果)
                    var category = code.StartsWith("F") ? "水果" : "蔬菜";
                    
                    produceList.Add(new ProduceItem
                    {
                        Id = code,
                        Name = cropName,
                        Category = category,
                        CurrentPrice = price,
                        Level = price > 80 ? "Expensive" : (price < 30 ? "Cheap" : "Fair")
                    });
                }
            }
            
            // 3. 寫入快取，設定 1 小時過期
            var cacheOptions = new MemoryCacheEntryOptions()
                .SetAbsoluteExpiration(TimeSpan.FromHours(1));
            cache.Set(cacheKey, produceList, cacheOptions);
        }
        catch (Exception ex)
        {
            return Results.Problem("無法取得農業部資料：" + ex.Message);
        }
    }

    // 回傳前 100 筆避免資料量過大導致手機端卡頓
    return Results.Ok(produceList?.Take(100));
});

// 取得所有可用市場列表的 API
app.MapGet("/api/markets", () =>
{
    var markets = new[]
    {
        "台北一", "台北二", "板橋區", "三重區", "宜蘭市", 
        "桃農", "台中市", "豐原區", "溪湖鎮", "西螺鎮", 
        "高雄市", "鳳山區", "屏東市", "台東市"
    };
    return Results.Ok(markets);
});

// 新增：取得單一品項歷史價格趨勢的 API
app.MapGet("/api/produce/{id}/history", (string id, double currentPrice) =>
{
    // 這裡我們模擬一個真實的歷史價格 API
    // 實務上，這裡應該去查詢資料庫 (例如 SQL Server 或 PostgreSQL) 撈取過去 7 天的歷史紀錄
    
    var history = new List<PriceHistory>();
    var today = DateTime.Now;
    var random = new Random();

    for (int i = 6; i >= 0; i--)
    {
        var date = today.AddDays(-i);
        // 模擬價格波動 (-5 到 +5 之間)
        var variance = (random.NextDouble() * 10) - 5;
        var price = Math.Max(1.0, currentPrice + variance);
        
        history.Add(new PriceHistory
        {
            Date = date.ToString("MM/dd"),
            Price = Math.Round(price, 1)
        });
    }

    return Results.Ok(history);
});

app.Run();

// 資料模型
public class ProduceItem
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double CurrentPrice { get; set; }
    public string Level { get; set; } = "";
}

public class PriceHistory
{
    public string Date { get; set; } = "";
    public double Price { get; set; }
}
