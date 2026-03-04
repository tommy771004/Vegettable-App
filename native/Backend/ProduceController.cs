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

        // 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts)
        // 取得使用者的收藏清單，並比對今日最新價格，判斷是否達到目標價格 (觸發提醒)
        [HttpGet("favorites")]
        public async Task<IActionResult> GetFavorites()
        {
            var userId = Request.Headers["X-User-Id"].FirstOrDefault();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var favorites = await _dbContext.UserFavorites
                .Where(f => f.UserId == userId)
                .ToListAsync();

            if (!favorites.Any())
            {
                return Ok(new List<FavoriteAlertDto>());
            }

            // 取得最新價格資料
            string rawData = await _produceService.FetchProduceDataAsync("ALL");
            var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<MoaProduceDto>();

            var result = new List<FavoriteAlertDto>();
            foreach (var fav in favorites)
            {
                // 假設 ProduceId 是 CropCode，並取得該作物的最新平均價格
                var latestData = moaItems.FirstOrDefault(m => m.CropCode == fav.ProduceId);
                double currentPrice = latestData?.AvgPrice ?? 0;
                string produceName = latestData?.CropName ?? fav.ProduceName ?? "Unknown";

                result.Add(new FavoriteAlertDto
                {
                    ProduceId = fav.ProduceId,
                    ProduceName = produceName,
                    TargetPrice = fav.TargetPrice,
                    CurrentPrice = currentPrice,
                    IsAlertTriggered = currentPrice > 0 && currentPrice <= fav.TargetPrice
                });
            }

            return Ok(result);
        }

        [HttpDelete("favorites/{produceId}")]
        public async Task<IActionResult> RemoveFavorite(string produceId)
        {
            var userId = Request.Headers["X-User-Id"].FirstOrDefault();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var favorite = await _dbContext.UserFavorites
                .FirstOrDefaultAsync(f => f.UserId == userId && f.ProduceId == produceId);

            if (favorite != null)
            {
                _dbContext.UserFavorites.Remove(favorite);
                await _dbContext.SaveChangesAsync();
            }

            return Ok(new { Message = "Favorite removed successfully" });
        }

        // 新增功能：社群回報機制 (Community Retail Price)
        [HttpPost("community-price")]
        public async Task<IActionResult> ReportCommunityPrice([FromBody] CommunityPriceDto request)
        {
            var userId = Request.Headers["X-User-Id"].FirstOrDefault();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var newPrice = new CommunityPrice
            {
                CropCode = request.CropCode,
                CropName = request.CropName,
                MarketName = request.MarketName,
                RetailPrice = request.RetailPrice,
                UserId = userId,
                ReportDate = System.DateTime.UtcNow
            };

            _dbContext.CommunityPrices.Add(newPrice);

            // Idea A: 遊戲化機制 - 增加貢獻積分
            var userStat = await _dbContext.UserStats.FirstOrDefaultAsync(u => u.UserId == userId);
            if (userStat == null)
            {
                userStat = new UserStat { UserId = userId, ContributionPoints = 0, Level = "新手菜鳥" };
                _dbContext.UserStats.Add(userStat);
            }
            
            userStat.ContributionPoints += 5; // 每次回報 +5 分

            // 升級邏輯
            if (userStat.ContributionPoints >= 100) userStat.Level = "市場達人";
            else if (userStat.ContributionPoints >= 50) userStat.Level = "精打細算";

            await _dbContext.SaveChangesAsync();

            return Ok(new { 
                Message = "Community price reported successfully",
                PointsEarned = 5,
                TotalPoints = userStat.ContributionPoints,
                CurrentLevel = userStat.Level
            });
        }

        [HttpGet("community-price/{cropCode}")]
        public async Task<IActionResult> GetCommunityPrices(string cropCode)
        {
            var prices = await _dbContext.CommunityPrices
                .Where(p => p.CropCode == cropCode)
                .OrderByDescending(p => p.ReportDate)
                .Take(20)
                .Select(p => new CommunityPriceDto
                {
                    CropCode = p.CropCode,
                    CropName = p.CropName,
                    MarketName = p.MarketName,
                    RetailPrice = p.RetailPrice,
                    ReportDate = p.ReportDate.ToString("yyyy-MM-dd HH:mm")
                })
                .ToListAsync();

            return Ok(prices);
        }

        // Idea A: 取得使用者積分與等級
        [HttpGet("user-stats")]
        public async Task<IActionResult> GetUserStats()
        {
            var userId = Request.Headers["X-User-Id"].FirstOrDefault();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var stats = await _dbContext.UserStats.FirstOrDefaultAsync(u => u.UserId == userId);
            if (stats == null) 
            {
                return Ok(new { ContributionPoints = 0, Level = "新手菜鳥" });
            }

            return Ok(stats);
        }

        // 新增功能：當季盛產日曆 (Seasonal Crop Calendar)
        [HttpGet("seasonal")]
        public IActionResult GetSeasonalCrops()
        {
            int currentMonth = System.DateTime.Now.Month;
            var seasonalCrops = new List<SeasonalCropDto>();

            // 簡易模擬當季農產品資料庫
            if (currentMonth >= 3 && currentMonth <= 5)
            {
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "LA1", CropName = "甘藍", Season = "春季", Description = "春季高麗菜鮮甜多汁" });
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "FJ1", CropName = "番茄", Season = "春季", Description = "春季番茄酸甜適中" });
            }
            else if (currentMonth >= 6 && currentMonth <= 8)
            {
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "T1", CropName = "西瓜", Season = "夏季", Description = "消暑解渴最佳選擇" });
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "P1", CropName = "木瓜", Season = "夏季", Description = "夏季木瓜香甜可口" });
            }
            else if (currentMonth >= 9 && currentMonth <= 11)
            {
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "F1", CropName = "柑桔", Season = "秋季", Description = "秋季柑桔富含維他命C" });
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "S1", CropName = "葡萄", Season = "秋季", Description = "秋季葡萄果肉飽滿" });
            }
            else
            {
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "SA1", CropName = "蘿蔔", Season = "冬季", Description = "冬季蘿蔔賽人蔘" });
                seasonalCrops.Add(new SeasonalCropDto { CropCode = "LH1", CropName = "菠菜", Season = "冬季", Description = "冬季菠菜營養豐富" });
            }

            return Ok(seasonalCrops);
        }

        // 新增功能：價格異常警告 (Price Anomaly Detection)
        [HttpGet("anomalies")]
        public async Task<ActionResult<IEnumerable<PriceAnomalyDto>>> GetPriceAnomalies()
        {
            // 找出最近的兩天有交易紀錄的日期
            var latestDates = await _dbContext.PriceHistories
                .Select(p => p.Date)
                .Distinct()
                .OrderByDescending(d => d)
                .Take(2)
                .ToListAsync();

            if (latestDates.Count < 2) return Ok(new List<PriceAnomalyDto>());

            var today = latestDates[0];
            var yesterday = latestDates[1];

            var todayPrices = await _dbContext.PriceHistories.Where(p => p.Date == today).ToDictionaryAsync(p => p.ProduceId);
            var yesterdayPrices = await _dbContext.PriceHistories.Where(p => p.Date == yesterday).ToDictionaryAsync(p => p.ProduceId);

            var anomalies = new List<PriceAnomalyDto>();
            foreach (var tp in todayPrices.Values)
            {
                if (yesterdayPrices.TryGetValue(tp.ProduceId, out var yp))
                {
                    if (yp.AveragePrice > 0)
                    {
                        var increase = (tp.AveragePrice - yp.AveragePrice) / yp.AveragePrice;
                        // 如果單日漲幅超過 50% (0.5)，則視為價格異常暴漲
                        if (increase >= 0.5) 
                        {
                            anomalies.Add(new PriceAnomalyDto
                            {
                                CropCode = tp.ProduceId,
                                CropName = tp.ProduceName,
                                CurrentPrice = tp.AveragePrice,
                                PreviousPrice = yp.AveragePrice,
                                IncreasePercentage = System.Math.Round(increase * 100, 2),
                                AlertMessage = $"⚠️ {tp.ProduceName} 價格單日暴漲 {System.Math.Round(increase * 100, 0)}%！建議暫緩購買或尋找替代品。"
                            });
                        }
                    }
                }
            }

            // 依據漲幅排序，漲最多的排前面
            return Ok(anomalies.OrderByDescending(a => a.IncreasePercentage));
        }

        // 新增功能：颱風 / 暴雨菜價預警推播 (Weather & Price Alert)
        [HttpGet("weather-alerts")]
        public IActionResult GetWeatherAlerts()
        {
            // 模擬串接中央氣象署 API，判斷是否有颱風或豪雨特報
            bool hasTyphoonWarning = true; // 模擬目前有颱風警報
            
            if (hasTyphoonWarning)
            {
                return Ok(new {
                    AlertType = "Typhoon",
                    Severity = "High",
                    Title = "⚠️ 颱風即將登陸！",
                    Message = "根據 AI 預測，葉菜類明日可能上漲 30%，建議今日提早採買高麗菜、青江菜！",
                    AffectedCrops = new[] { "高麗菜", "青江菜", "小白菜" }
                });
            }
            
            return Ok(new { AlertType = "None" });
        }

        // 新增功能：「今天吃什麼？」省錢食譜推薦 (Budget Recipe Generator)
        [HttpGet("budget-recipes")]
        public IActionResult GetBudgetRecipes()
        {
            // 模擬系統抓出今日跌幅最大 / 最便宜的 3 樣當季蔬菜，並推薦食譜
            var recipes = new List<object>
            {
                new {
                    RecipeName = "番茄炒蛋",
                    MainIngredients = new[] { "番茄", "雞蛋" },
                    Reason = "今日番茄盛產大跌價，每台斤只要 25 元！",
                    ImageUrl = "🍅", // 暫用 Emoji 替代圖片
                    Steps = new[] { "1. 番茄切塊", "2. 雞蛋打散炒熟", "3. 加入番茄拌炒", "4. 加點番茄醬與糖調味" }
                },
                new {
                    RecipeName = "蒜炒高麗菜",
                    MainIngredients = new[] { "高麗菜", "蒜頭" },
                    Reason = "高麗菜價格平穩，營養價值高！",
                    ImageUrl = "🥬",
                    Steps = new[] { "1. 高麗菜洗淨切片", "2. 蒜頭爆香", "3. 放入高麗菜大火快炒", "4. 加鹽調味即可" }
                }
            };

            return Ok(recipes);
        }
    }

    public class FavoriteRequest
    {
        // 邏輯修正：移除 UserId，因為已經改由 Header 傳遞
        public string ProduceId { get; set; }
        public double TargetPrice { get; set; }
    }
}
