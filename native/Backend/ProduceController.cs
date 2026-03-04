using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Threading.Tasks;
using System.Linq;
using System.Collections.Generic;
using ProduceApi.Services;
using ProduceApi.Data;
using ProduceApi.Models;
using System.Text.Json;

namespace ProduceApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class ProduceController : ControllerBase
    {
        private readonly ProduceService _produceService;
        private readonly ProduceDbContext _dbContext;

        public ProduceController(ProduceService produceService, ProduceDbContext dbContext)
        {
            _produceService = produceService;
            _dbContext = dbContext;
        }

        // 邏輯修正：加入分頁 (Pagination) 與關鍵字搜尋 (Search)
        // 解決問題：原本一次回傳 2000+ 筆資料會導致手機 App 記憶體爆掉 (OOM) 且載入極慢。
        [HttpGet("daily-prices")]
        public async Task<IActionResult> GetDailyPrices([FromQuery] string keyword = "", [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
        {
            try
            {
                string rawData = await _produceService.FetchProduceDataAsync("ALL");
                
                // 假設解析政府 API 的 JSON 格式
                var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<MoaProduceDto>();
                
                var allItems = moaItems.Select(m => new ProduceDto {
                    CropCode = m.CropCode,
                    CropName = m.CropName,
                    MarketCode = m.MarketCode,
                    MarketName = m.MarketName,
                    AvgPrice = m.AvgPrice,
                    TransQuantity = m.TransQuantity,
                    Date = m.Date
                }).ToList();

                // 1. 搜尋過濾
                if (!string.IsNullOrEmpty(keyword))
                {
                    allItems = allItems.Where(x => x.CropName.Contains(keyword) || x.MarketName.Contains(keyword)).ToList();
                }

                // 2. 分頁處理
                int totalItems = allItems.Count;
                var pagedData = allItems
                    .Skip((page - 1) * pageSize)
                    .Take(pageSize)
                    .ToList();

                var response = new PaginatedResponse<ProduceDto>
                {
                    CurrentPage = page,
                    TotalPages = (int)System.Math.Ceiling(totalItems / (double)pageSize),
                    TotalItems = totalItems,
                    Data = pagedData
                };

                return Ok(response);
            }
            catch (System.Exception ex)
            {
                return StatusCode(500, $"Internal server error: {ex.Message}");
            }
        }

        [HttpGet("history/{produceId}")]
        public async Task<IActionResult> GetPriceHistory(string produceId)
        {
            var history = await _dbContext.PriceHistories
                .Where(p => p.ProduceId == produceId)
                .OrderByDescending(p => p.RecordDate)
                .Take(30)
                .ToListAsync();

            return Ok(history);
        }

        // 新增功能：市場比價 (Market Comparison)
        // 允許使用者查詢特定農產品在全台各市場的今日價格，並由低到高排序
        [HttpGet("compare/{cropName}")]
        public async Task<IActionResult> GetMarketComparison(string cropName)
        {
            try
            {
                string rawData = await _produceService.FetchProduceDataAsync("ALL");
                var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<MoaProduceDto>();

                var allItems = moaItems.Select(m => new ProduceDto {
                    CropCode = m.CropCode,
                    CropName = m.CropName,
                    MarketCode = m.MarketCode,
                    MarketName = m.MarketName,
                    AvgPrice = m.AvgPrice,
                    TransQuantity = m.TransQuantity,
                    Date = m.Date
                }).ToList();

                var comparisonData = allItems
                    .Where(x => x.CropName.Equals(cropName, System.StringComparison.OrdinalIgnoreCase))
                    .OrderBy(x => x.AvgPrice)
                    .ToList();

                return Ok(comparisonData);
            }
            catch (System.Exception ex)
            {
                return StatusCode(500, $"Internal server error: {ex.Message}");
            }
        }

        // 新增功能：價格預測與趨勢分析 (Price Forecasting)
        // 根據過去 14 天的歷史資料，計算 7 日移動平均線，預測未來價格趨勢
        [HttpGet("forecast/{produceId}")]
        public async Task<IActionResult> GetPriceForecast(string produceId)
        {
            var history = await _dbContext.PriceHistories
                .Where(p => p.ProduceId == produceId)
                .OrderByDescending(p => p.RecordDate)
                .Take(14)
                .ToListAsync();

            if (history.Count < 7)
            {
                return Ok(new { Forecast = "Insufficient Data", Trend = "Unknown" });
            }

            var recent7DaysAvg = history.Take(7).Average(p => p.AveragePrice);
            var previous7DaysAvg = history.Skip(7).Take(7).Average(p => p.AveragePrice);

            string trend = "Stable";
            if (recent7DaysAvg > previous7DaysAvg * 1.05) trend = "Up";
            else if (recent7DaysAvg < previous7DaysAvg * 0.95) trend = "Down";

            return Ok(new { 
                RecentAverage = recent7DaysAvg,
                PreviousAverage = previous7DaysAvg,
                Trend = trend,
                Message = $"Recent 7-day avg is {recent7DaysAvg:F2}, previous 7-day avg was {previous7DaysAvg:F2}."
            });
        }

        // 新增功能：熱門交易農產品 (Top Volume Crops)
        // 取得今日交易量最大的前 10 名農產品，幫助使用者了解目前市場上最熱銷、當季的農產品
        [HttpGet("top-volume")]
        public async Task<IActionResult> GetTopVolumeCrops()
        {
            try
            {
                string rawData = await _produceService.FetchProduceDataAsync("ALL");
                var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<MoaProduceDto>();

                var topItems = moaItems.Select(m => new ProduceDto {
                    CropCode = m.CropCode,
                    CropName = m.CropName,
                    MarketCode = m.MarketCode,
                    MarketName = m.MarketName,
                    AvgPrice = m.AvgPrice,
                    TransQuantity = m.TransQuantity,
                    Date = m.Date
                })
                .OrderByDescending(x => x.TransQuantity)
                .Take(10)
                .ToList();

                return Ok(topItems);
            }
            catch (System.Exception ex)
            {
                return StatusCode(500, $"Internal server error: {ex.Message}");
            }
        }

        [HttpPost("favorites")]
        public async Task<IActionResult> AddFavorite([FromBody] FavoriteRequest request)
        {
            // 邏輯修正：從 Header 中讀取 X-User-Id，而不是依賴前端在 Body 傳遞。
            // 解決問題：這樣可以防止惡意使用者竄改 Body 中的 UserId 去修改別人的收藏。
            var userId = Request.Headers["X-User-Id"].FirstOrDefault();
            
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var existingFavorite = await _dbContext.UserFavorites
                .FirstOrDefaultAsync(f => f.UserId == userId && f.ProduceId == request.ProduceId);

            if (existingFavorite != null)
            {
                existingFavorite.TargetPrice = request.TargetPrice;
                _dbContext.UserFavorites.Update(existingFavorite);
            }
            else
            {
                _dbContext.UserFavorites.Add(new UserFavorite
                {
                    UserId = userId,
                    ProduceId = request.ProduceId,
                    TargetPrice = request.TargetPrice
                });
            }

            await _dbContext.SaveChangesAsync();
            return Ok(new { Message = "Favorite synced successfully" });
        }
    }

    public class FavoriteRequest
    {
        // 邏輯修正：移除 UserId，因為已經改由 Header 傳遞
        public string ProduceId { get; set; }
        public double TargetPrice { get; set; }
    }
}
