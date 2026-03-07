using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using Microsoft.EntityFrameworkCore;
using System.Threading.Tasks;
using System.Linq;
using System.Collections.Generic;
using System.Security.Claims;
using ProduceApi.Services;
using ProduceApi.Data;
using ProduceApi.Models;
using System.Text.Json;

// =============================================================================
// ProduceController.cs — 農產品 API 控制器 (13 個端點)
//
// API 端點清單：
//   GET  /api/produce/daily-prices      - 今日批發價格 (分頁 + 搜尋)
//   GET  /api/produce/history/{id}      - 歷史 30 天價格紀錄
//   GET  /api/produce/compare/{name}    - 各市場比價排行
//   GET  /api/produce/forecast/{id}     - 7 日移動平均線趨勢預測
//   GET  /api/produce/top-volume        - 今日交易量前 10 名
//   POST /api/produce/favorites         - 新增/更新我的收藏
//   GET  /api/produce/favorites         - 取得我的收藏清單
//   DELETE /api/produce/favorites/{id}  - 移除收藏
//   POST /api/produce/community-price   - 社群零售價回報
//   GET  /api/produce/community-price/{code} - 查詢社群回報記錄
//   GET  /api/produce/user-stats        - 取得使用者積分與等級
//   GET  /api/produce/seasonal          - 當季盛產農作物
//   GET  /api/produce/anomalies         - 價格異常警告
//   GET  /api/produce/weather-alerts    - 颱風/豪大雨預警
//   GET  /api/produce/budget-recipes    - 今日省錢食譜推薦
//   POST /api/produce/fcm-token         - 更新裝置 FCM 推播 Token
//
// 認證方式 (本次更新)：
//   原本：X-User-Id Header (任何人可偽造)
//   現在：JWT Bearer Token (由 /auth/token 發行，HMAC-SHA256 簽章防止偽造)
//
//   取得 UserId 方式：
//     舊：Request.Headers["X-User-Id"].FirstOrDefault()
//     新：User.FindFirst(ClaimTypes.NameIdentifier)?.Value
// =============================================================================

namespace ProduceApi.Controllers
{
    /// <summary>
    /// 農產品 API 控制器
    /// 需要 JWT 認證 ([Authorize])，透過 Authorization: Bearer {token} Header 驗證身份
    /// </summary>
    [ApiController]
    [Route("api/[controller]")]
    public class ProduceController : ControllerBase
    {
        private readonly ProduceService _produceService;
        private readonly ProduceDbContext _dbContext;
        private readonly ILogger<ProduceController> _logger;

        /// <summary>
        /// 建構子：DI 注入農產品服務與資料庫 Context
        /// </summary>
        public ProduceController(ProduceService produceService, ProduceDbContext dbContext, ILogger<ProduceController> logger)
        {
            _produceService = produceService;
            _dbContext = dbContext;
            _logger = logger;
        }

        /// <summary>
        /// 從已驗證的 JWT Token 中取得使用者 ID
        /// 若 Token 無效或未帶 Token，此方法返回 null
        /// </summary>
        private string? GetCurrentUserId()
            => User.FindFirst(ClaimTypes.NameIdentifier)?.Value;

        // ─────────────────────────────────────────────────────────────────────
        // 公開端點 (不需要 JWT，任何人可存取)
        // ─────────────────────────────────────────────────────────────────────

        /// <summary>
        /// 取得今日農產品批發價格 (支援分頁與關鍵字搜尋)
        /// GET /api/produce/daily-prices?keyword=番茄&amp;page=1&amp;pageSize=20
        ///
        /// 為何加入分頁：原本一次回傳 2000+ 筆資料，手機 App 會因 OOM 崩潰且載入極慢。
        /// 分頁後每次只傳 20 筆，記憶體使用量減少 99%。
        /// </summary>
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

        /// <summary>
        /// 取得指定農產品的歷史 30 天均價記錄 (供折線圖使用)
        /// GET /api/produce/history/{produceId}
        /// </summary>
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

        /// <summary>
        /// 市場比價：查詢指定農產品在各市場的今日價格，由低到高排序
        /// GET /api/produce/compare/{cropName}
        /// 幫助使用者找到最便宜的購買地點
        /// </summary>
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

        // ─────────────────────────────────────────────────────────────────────
        // 需要登入的端點 ([Authorize] → 必須帶 JWT Token)
        // ─────────────────────────────────────────────────────────────────────

        /// <summary>
        /// 新增或更新我的收藏 (若已收藏則更新目標價格)
        /// POST /api/produce/favorites
        /// Body: { "produceId": "LA1", "targetPrice": 30.0 }
        /// 需要 JWT 認證：UserId 從 Token Claims 讀取，防止偽造
        /// </summary>
        [Authorize]
        [HttpPost("favorites")]
        public async Task<IActionResult> AddFavorite([FromBody] FavoriteRequest request)
        {
            // [JWT 更新] 改從 JWT Claims 取得 UserId，比 X-User-Id Header 更安全
            // Claims 已由 JwtBearerMiddleware 驗證，無法被偽造
            var userId = GetCurrentUserId();
            
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

        /// <summary>
        /// 取得我的收藏清單，並即時比對今日均價判斷是否達到目標提醒價
        /// GET /api/produce/favorites
        /// 回傳 FavoriteAlertDto 列表，包含 isAlertTriggered 欄位
        /// </summary>
        [Authorize]
        [HttpGet("favorites")]
        public async Task<IActionResult> GetFavorites()
        {
            var userId = GetCurrentUserId();
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

        /// <summary>
        /// 移除收藏
        /// DELETE /api/produce/favorites/{produceId}
        /// </summary>
        [Authorize]
        [HttpDelete("favorites/{produceId}")]
        public async Task<IActionResult> RemoveFavorite(string produceId)
        {
            var userId = GetCurrentUserId();
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

        /// <summary>
        /// 社群零售價回報：使用者回報在超市/傳統市場看到的實際零售價
        /// POST /api/produce/community-price
        /// 回報成功後自動增加 5 點積分，積分達標可升級 (遊戲化機制)
        /// </summary>
        [Authorize]
        [HttpPost("community-price")]
        public async Task<IActionResult> ReportCommunityPrice([FromBody] CommunityPriceDto request)
        {
            var userId = GetCurrentUserId();
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

        /// <summary>
        /// 取得使用者積分與等級 (遊戲化機制)
        /// GET /api/produce/user-stats
        /// 等級系統：0-49分=新手菜鳥, 50-99分=精打細算, 100+分=市場達人
        /// </summary>
        [Authorize]
        [HttpGet("user-stats")]
        public async Task<IActionResult> GetUserStats()
        {
            var userId = GetCurrentUserId();
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
        public async Task<IActionResult> GetSeasonalCrops()
        {
            int currentMonth = System.DateTime.Now.Month;
            
            // 從資料庫查詢當季農產品
            var seasonalCrops = await _dbContext.SeasonalCrops
                .Where(c => (c.StartMonth <= currentMonth && c.EndMonth >= currentMonth) || 
                           (c.StartMonth > c.EndMonth && (currentMonth >= c.StartMonth || currentMonth <= c.EndMonth))) // Handle winter crossing year (e.g., Dec to Feb)
                .Select(c => new SeasonalCropDto 
                { 
                    CropCode = c.CropCode, 
                    CropName = c.CropName, 
                    Season = c.Season, 
                    Description = c.Description 
                })
                .ToListAsync();

            return Ok(seasonalCrops);
        }

        // 新增功能：價格異常警告 (Price Anomaly Detection)
        [HttpGet("anomalies")]
        public async Task<ActionResult<IEnumerable<PriceAnomalyDto>>> GetPriceAnomalies()
        {
            // 找出最近的兩天有交易紀錄的日期
            var latestDates = await _dbContext.PriceHistories
                .Select(p => p.RecordDate.Date)
                .Distinct()
                .OrderByDescending(d => d)
                .Take(2)
                .ToListAsync();

            if (latestDates.Count < 2) return Ok(new List<PriceAnomalyDto>());

            var today = latestDates[0];
            var yesterday = latestDates[1];

            var todayPrices = await _dbContext.PriceHistories.Where(p => p.RecordDate.Date == today).ToDictionaryAsync(p => p.ProduceId);
            var yesterdayPrices = await _dbContext.PriceHistories.Where(p => p.RecordDate.Date == yesterday).ToDictionaryAsync(p => p.ProduceId);

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
                                CropName = tp.ProduceName ?? "未知作物",
                                CurrentPrice = tp.AveragePrice,
                                PreviousPrice = yp.AveragePrice,
                                IncreasePercentage = System.Math.Round(increase * 100, 2),
                                AlertMessage = $"⚠️ {tp.ProduceName ?? "未知作物"} 價格單日暴漲 {System.Math.Round(increase * 100, 0)}%！建議暫緩購買或尋找替代品。"
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
        public async Task<IActionResult> GetWeatherAlerts()
        {
            try
            {
                using var client = new System.Net.Http.HttpClient();
                var response = await client.GetAsync("https://www.cwa.gov.tw/rss/Data/cwa_warning.xml");
                if (response.IsSuccessStatusCode)
                {
                    var xmlContent = await response.Content.ReadAsStringAsync();
                    bool hasTyphoonWarning = xmlContent.Contains("颱風");
                    bool hasHeavyRainWarning = xmlContent.Contains("豪雨") || xmlContent.Contains("大雨");

                    if (hasTyphoonWarning)
                    {
                        return Ok(new {
                            AlertType = "Typhoon",
                            Severity = "High",
                            Title = "⚠️ 颱風警報發布！",
                            Message = "根據中央氣象署資料，目前有颱風警報。預期葉菜類將有顯著漲幅，建議提早採買耐放蔬菜（如高麗菜、根莖類）！",
                            AffectedCrops = new[] { "高麗菜", "青江菜", "小白菜", "空心菜" }
                        });
                    }
                    else if (hasHeavyRainWarning)
                    {
                        return Ok(new {
                            AlertType = "HeavyRain",
                            Severity = "Medium",
                            Title = "🌧️ 豪大雨特報！",
                            Message = "根據中央氣象署資料，目前有豪大雨特報。瓜果類與葉菜類易受損，價格可能波動，請留意！",
                            AffectedCrops = new[] { "西瓜", "木瓜", "青江菜", "地瓜葉" }
                        });
                    }
                }
            }
            catch (System.Exception ex)
            {
                _logger.LogWarning(ex, "Failed to fetch weather alerts from CWA RSS feed. Returning 'None'.");
            }
            
            return Ok(new { AlertType = "None" });
        }

        // 新增功能：「今天吃什麼？」省錢食譜推薦 (Budget Recipe Generator)
        [HttpGet("budget-recipes")]
        public async Task<IActionResult> GetBudgetRecipes()
        {
            // 找出最近的兩天有交易紀錄的日期
            var latestDates = await _dbContext.PriceHistories
                .Select(p => p.RecordDate.Date)
                .Distinct()
                .OrderByDescending(d => d)
                .Take(2)
                .ToListAsync();

            if (latestDates.Count < 2) 
            {
                return Ok(new List<object>());
            }

            var today = latestDates[0];
            var yesterday = latestDates[1];

            var todayPrices = await _dbContext.PriceHistories.Where(p => p.RecordDate.Date == today).ToDictionaryAsync(p => p.ProduceId);
            var yesterdayPrices = await _dbContext.PriceHistories.Where(p => p.RecordDate.Date == yesterday).ToDictionaryAsync(p => p.ProduceId);

            var priceDrops = new List<PriceAnomalyDto>();
            foreach (var tp in todayPrices.Values)
            {
                if (yesterdayPrices.TryGetValue(tp.ProduceId, out var yp))
                {
                    if (yp.AveragePrice > 0)
                    {
                        var decrease = (yp.AveragePrice - tp.AveragePrice) / yp.AveragePrice;
                        if (decrease > 0) 
                        {
                            priceDrops.Add(new PriceAnomalyDto
                            {
                                CropCode = tp.ProduceId,
                                CropName = tp.ProduceName ?? "未知作物",
                                CurrentPrice = tp.AveragePrice,
                                PreviousPrice = yp.AveragePrice,
                                IncreasePercentage = System.Math.Round(decrease * 100, 2)
                            });
                        }
                    }
                }
            }

            var topDrops = priceDrops.OrderByDescending(p => p.IncreasePercentage).Take(3).ToList();
            var recipes = new List<object>();

            // 從資料庫查詢食譜
            var allRecipes = await _dbContext.Recipes.ToListAsync();

            foreach (var drop in topDrops)
            {
                // 尋找匹配的食譜 (模糊搜尋)
                var matchedRecipe = allRecipes.FirstOrDefault(r => drop.CropName.Contains(r.MainIngredient));
                
                if (matchedRecipe != null)
                {
                    recipes.Add(new {
                        RecipeName = matchedRecipe.RecipeName,
                        MainIngredients = JsonSerializer.Deserialize<string[]>(matchedRecipe.IngredientsJson),
                        Reason = $"今日 {drop.CropName} 價格大跌 {drop.IncreasePercentage}%，每公斤只要 {drop.CurrentPrice} 元！",
                        ImageUrl = matchedRecipe.ImageUrl,
                        Steps = JsonSerializer.Deserialize<string[]>(matchedRecipe.StepsJson)
                    });
                }
            }

            // 如果沒有匹配到，提供預設食譜
            if (recipes.Count == 0)
            {
                recipes.Add(new {
                    RecipeName = "清炒時蔬",
                    MainIngredients = new[] { "當季蔬菜", "蒜頭" },
                    Reason = "多吃蔬菜有益健康！",
                    ImageUrl = "🥗",
                    Steps = new[] { "1. 蔬菜洗淨切段", "2. 蒜頭爆香", "3. 放入蔬菜大火快炒", "4. 加鹽調味即可" }
                });
            }

            return Ok(recipes);
        }

        /// <summary>
        /// 更新裝置 FCM 推播 Token
        /// POST /api/produce/fcm-token
        /// App 每次啟動時應呼叫此端點，確保 Token 最新有效
        /// FCM Token 可能因 App 重新安裝或 Firebase 重置而變更
        /// </summary>
        [Authorize]
        [HttpPost("fcm-token")]
        public async Task<IActionResult> UpdateFcmToken([FromBody] FcmTokenRequest request)
        {
            var userId = GetCurrentUserId();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Missing X-User-Id header" });
            }

            var userStat = await _dbContext.UserStats.FirstOrDefaultAsync(u => u.UserId == userId);
            if (userStat == null)
            {
                userStat = new UserStat { UserId = userId, ContributionPoints = 0, Level = "新手菜鳥", FcmToken = request.Token };
                _dbContext.UserStats.Add(userStat);
            }
            else
            {
                userStat.FcmToken = request.Token;
                _dbContext.UserStats.Update(userStat);
            }
            await _dbContext.SaveChangesAsync();

            return Ok(new { Message = "FCM Token updated successfully" });
        }
    }

    public class FcmTokenRequest
    {
        public string Token { get; set; }
    }

    public class FavoriteRequest
    {
        // 邏輯修正：移除 UserId，因為已經改由 Header 傳遞
        public string ProduceId { get; set; }
        public double TargetPrice { get; set; }
    }
}
