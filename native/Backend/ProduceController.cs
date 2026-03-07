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
            // Cap pageSize to prevent OOM from malicious large values
            pageSize = System.Math.Clamp(pageSize, 1, 100);
            page = System.Math.Max(1, page);

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
                // [Bug Fix] 原本 x.CropName.Contains(keyword) 在 CropName 或 MarketName 為 null 時
                // 會拋出 NullReferenceException。修正：使用 null 條件運算子 ?. 避免 NPE。
                if (!string.IsNullOrEmpty(keyword))
                {
                    allItems = allItems.Where(x =>
                        (x.CropName?.Contains(keyword, StringComparison.OrdinalIgnoreCase) ?? false) ||
                        (x.MarketName?.Contains(keyword, StringComparison.OrdinalIgnoreCase) ?? false)
                    ).ToList();
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
                _logger.LogError(ex, "Unhandled exception in {Action}", ControllerContext.ActionDescriptor.ActionName);
                return StatusCode(500, "Internal server error. Please try again later.");
            }
        }

        /// <summary>
        /// 取得指定農產品的歷史 30 天均價記錄 (供折線圖使用)
        /// GET /api/produce/history/{produceId}
        /// </summary>
        [HttpGet("history/{produceId}")]
        public async Task<IActionResult> GetPriceHistory(string produceId)
        {
            // [Bug Fix 1] 原本回傳原始 PriceHistory 實體，欄位名稱為 AveragePrice / RecordDate，
            // 與 iOS HistoricalPriceDto { date: String, avgPrice: Double } 和
            // Android HistoricalPriceDto { date: String, avgPrice: Double } 不符，導致解析失敗。
            // 修正：投影為符合客戶端 DTO 格式的匿名物件，並格式化日期字串。
            //
            // [Bug Fix 2] ProduceDto.Id 現在是複合鍵 "{CropCode}-{MarketCode}-{Date}"
            // 但 PriceHistory.ProduceId 只存 CropCode (由 ProduceSyncWorker 寫入)。
            // 修正：若 produceId 含有 '-'，取第一個 '-' 前的部分作為 CropCode 查詢。
            var cropCode = produceId.Contains('-') ? produceId[..produceId.IndexOf('-')] : produceId;

            var history = await _dbContext.PriceHistories
                .Where(p => p.ProduceId == cropCode)
                .OrderByDescending(p => p.RecordDate)
                .Take(30)
                .Select(p => new { date = p.RecordDate.ToString("yyyy-MM-dd"), avgPrice = p.AveragePrice })
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

                // [Bug Fix] 原本回傳完整 ProduceDto（含 cropCode、cropName 等多餘欄位）。
                // iOS MarketComparisonDto / Android MarketCompareDto 只需要
                // { marketName, avgPrice, transQuantity, date }。
                // 修正：投影為 MarketCompareDto，與客戶端 DTO 精確對應。
                var comparisonData = allItems
                    .Where(x => x.CropName.Equals(cropName, System.StringComparison.OrdinalIgnoreCase))
                    .OrderBy(x => x.AvgPrice)
                    .Select(x => new MarketCompareDto
                    {
                        MarketName = x.MarketName,
                        AvgPrice = x.AvgPrice,
                        TransQuantity = x.TransQuantity,
                        Date = x.Date
                    })
                    .ToList();

                return Ok(comparisonData);
            }
            catch (System.Exception ex)
            {
                _logger.LogError(ex, "Unhandled exception in {Action}", ControllerContext.ActionDescriptor.ActionName);
                return StatusCode(500, "Internal server error. Please try again later.");
            }
        }

        // 新增功能：價格預測與趨勢分析 (Price Forecasting)
        // 根據過去 14 天的歷史資料，計算 7 日移動平均線，預測未來價格趨勢
        [HttpGet("forecast/{produceId}")]
        public async Task<IActionResult> GetPriceForecast(string produceId)
        {
            // [Bug Fix] 同 GetPriceHistory：ProduceDto.Id 是複合鍵，PriceHistory.ProduceId 只存 CropCode。
            var cropCode = produceId.Contains('-') ? produceId[..produceId.IndexOf('-')] : produceId;

            var history = await _dbContext.PriceHistories
                .Where(p => p.ProduceId == cropCode)
                .OrderByDescending(p => p.RecordDate)
                .Take(14)
                .ToListAsync();

            if (history.Count < 7)
            {
                // [Bug Fix] 原本回傳 { Forecast, Trend }，與客戶端 PricePredictionResponse
                // { recentAverage, previousAverage, trend, message } 格式不符，導致解析失敗。
                // 修正：使用資料不足時的合理預設值，保持回應格式一致。
                var singleAvg = history.Count > 0 ? history.Average(p => p.AveragePrice) : 0;
                return Ok(new { recentAverage = singleAvg, previousAverage = singleAvg, trend = "Stable", message = "歷史資料不足，無法預測趨勢。" });
            }

            var recent7DaysAvg = history.Take(7).Average(p => p.AveragePrice);
            var previous7DaysAvg = history.Skip(7).Take(7).Average(p => p.AveragePrice);

            string trend = "Stable";
            if (recent7DaysAvg > previous7DaysAvg * 1.05) trend = "Up";
            else if (recent7DaysAvg < previous7DaysAvg * 0.95) trend = "Down";

            string trendText = trend == "Up" ? "上漲" : trend == "Down" ? "下跌" : "穩定";
            return Ok(new {
                recentAverage = recent7DaysAvg,
                previousAverage = previous7DaysAvg,
                trend = trend,
                message = $"近 7 日均價 {recent7DaysAvg:F1} 元，前 7 日均價 {previous7DaysAvg:F1} 元，價格趨勢{trendText}。"
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

                // [Improvement] 原本直接對市場-作物組合排序取前 10，
                // 同一作物（如高麗菜）在 20 個市場都有記錄，全部佔滿前 10 名。
                // 修正：先 GroupBy CropCode 合計全台交易量，再取前 10 名，
                // 使排名代表全台各「作物」的交易量，而非各「市場-作物」組合。
                var topItems = moaItems
                    .GroupBy(m => m.CropCode)
                    .Select(g => new ProduceDto {
                        CropCode = g.Key,
                        CropName = g.First().CropName,
                        MarketCode = "",
                        MarketName = "全台合計",
                        AvgPrice = g.Average(m => m.AvgPrice),
                        TransQuantity = g.Sum(m => m.TransQuantity),
                        Date = g.First().Date
                    })
                    .OrderByDescending(x => x.TransQuantity)
                    .Take(10)
                    .ToList();

                return Ok(topItems);
            }
            catch (System.Exception ex)
            {
                _logger.LogError(ex, "Unhandled exception in {Action}", ControllerContext.ActionDescriptor.ActionName);
                return StatusCode(500, "Internal server error. Please try again later.");
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
            }

            if (string.IsNullOrWhiteSpace(request.ProduceId) || request.TargetPrice <= 0)
            {
                return BadRequest(new { Message = "ProduceId is required and TargetPrice must be greater than 0." });
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
                    // [Bug Fix] 儲存作物名稱，供 GetFavorites 在 MOA API 無資料時作為 fallback。
                    ProduceName = request.ProduceName,
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
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

            // [Improvement] 原本在迴圈內 FirstOrDefault(m => m.CropCode == fav.ProduceId) 是 O(N) 搜尋，
            // 每筆收藏都要掃描 ~2000 筆 MOA 資料 → 整體 O(M×N)。
            // 修正：先建立 CropCode → 最新均價 Dictionary，查詢降為 O(1)。
            var priceDict = moaItems
                .GroupBy(m => m.CropCode)
                .ToDictionary(g => g.Key, g => g.First());

            var result = new List<FavoriteAlertDto>();
            foreach (var fav in favorites)
            {
                priceDict.TryGetValue(fav.ProduceId, out var latestData);
                double currentPrice = latestData?.AvgPrice ?? 0;
                string produceName = latestData?.CropName ?? fav.ProduceName ?? "Unknown";

                // Only trigger alert when we have actual market data (latestData != null)
                bool alertTriggered = latestData != null && currentPrice > 0 && currentPrice <= fav.TargetPrice;
                result.Add(new FavoriteAlertDto
                {
                    ProduceId = fav.ProduceId,
                    ProduceName = produceName,
                    TargetPrice = fav.TargetPrice,
                    CurrentPrice = currentPrice,
                    IsAlertTriggered = alertTriggered
                });
            }

            return Ok(result);
        }

        /// <summary>
        /// 修改收藏的目標到價提醒
        /// PUT /api/produce/favorites/{produceId}
        /// Body: { "targetPrice": 25.0 }
        /// [Bug Fix] iOS updateFavoriteTargetPrice 與 Android UpdateFavoriteDto 均呼叫此端點，
        ///           但原本後端只有 POST (upsert) 沒有 PUT，導致 405 Method Not Allowed。
        /// </summary>
        [Authorize]
        [HttpPut("favorites/{produceId}")]
        public async Task<IActionResult> UpdateFavorite(string produceId, [FromBody] UpdateFavoriteRequest request)
        {
            var userId = GetCurrentUserId();
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
            }

            if (request.TargetPrice <= 0)
            {
                return BadRequest(new { Message = "TargetPrice must be greater than 0." });
            }

            var favorite = await _dbContext.UserFavorites
                .FirstOrDefaultAsync(f => f.UserId == userId && f.ProduceId == produceId);

            if (favorite == null)
            {
                return NotFound(new { Message = $"Favorite '{produceId}' not found." });
            }

            favorite.TargetPrice = request.TargetPrice;
            _dbContext.UserFavorites.Update(favorite);
            await _dbContext.SaveChangesAsync();

            return Ok(new { Message = "Target price updated successfully" });
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
            }

            if (string.IsNullOrWhiteSpace(request.CropName) || request.RetailPrice <= 0)
            {
                return BadRequest(new { Message = "CropName is required and RetailPrice must be greater than 0." });
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
            }

            var stats = await _dbContext.UserStats.FirstOrDefaultAsync(u => u.UserId == userId);

            // [Bug Fix] 原本：
            //   1. stats == null 時回傳 { ContributionPoints, Level }（缺少 reportCount）
            //   2. stats != null 時回傳原始實體（暴露 FcmToken、UserId 敏感欄位，且無 reportCount）
            // 修正：一律回傳 UserStatsDto，reportCount 從 CommunityPrices 動態計算。
            var reportCount = await _dbContext.CommunityPrices.CountAsync(p => p.UserId == userId);

            return Ok(new UserStatsDto
            {
                ContributionPoints = stats?.ContributionPoints ?? 0,
                Level = stats?.Level ?? "新手菜鳥",
                ReportCount = reportCount
            });
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

            // [Bug Fix] 原本使用 ToDictionaryAsync(p => p.ProduceId)，但同一作物在多個市場都有記錄，
            // ProduceId (CropCode) 重複 → 拋出 ArgumentException: An item with the same key has already been added。
            // 修正：先 GroupBy ProduceId，計算各作物的全台平均價格，再建立唯一 Dictionary。
            var todayPrices = await _dbContext.PriceHistories
                .Where(p => p.RecordDate.Date == today)
                .GroupBy(p => p.ProduceId)
                .Select(g => new { ProduceId = g.Key, ProduceName = g.First().ProduceName, AveragePrice = g.Average(p => p.AveragePrice) })
                .ToListAsync();

            var yesterdayPrices = await _dbContext.PriceHistories
                .Where(p => p.RecordDate.Date == yesterday)
                .GroupBy(p => p.ProduceId)
                .Select(g => new { ProduceId = g.Key, AveragePrice = g.Average(p => p.AveragePrice) })
                .ToDictionaryAsync(g => g.ProduceId);

            var anomalies = new List<PriceAnomalyDto>();
            foreach (var tp in todayPrices)
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
            // [Improvement] 加入 Redis 快取（30 分鐘 TTL）：
            // 原本每次請求都打一次 CWA RSS，多人同時使用時會產生大量外部 HTTP 請求。
            // 天氣警報 30 分鐘更新一次已足夠精確，快取可大幅降低外部依賴。
            const string cacheKey = "WeatherAlert_CWA";
            var cached = await _produceService.GetCachedStringAsync(cacheKey);
            if (cached != null)
            {
                return Content(cached, "application/json");
            }

            object alertResult;
            try
            {
                using var client = new System.Net.Http.HttpClient();
                client.Timeout = System.TimeSpan.FromSeconds(5); // 避免外部 API 緩慢時阻塞請求
                var response = await client.GetAsync("https://www.cwa.gov.tw/rss/Data/cwa_warning.xml");
                if (response.IsSuccessStatusCode)
                {
                    var xmlContent = await response.Content.ReadAsStringAsync();
                    bool hasTyphoonWarning = xmlContent.Contains("颱風");
                    bool hasHeavyRainWarning = xmlContent.Contains("豪雨") || xmlContent.Contains("大雨");

                    if (hasTyphoonWarning)
                    {
                        alertResult = new {
                            AlertType = "Typhoon",
                            Severity = "High",
                            Title = "⚠️ 颱風警報發布！",
                            Message = "根據中央氣象署資料，目前有颱風警報。預期葉菜類將有顯著漲幅，建議提早採買耐放蔬菜（如高麗菜、根莖類）！",
                            AffectedCrops = new[] { "高麗菜", "青江菜", "小白菜", "空心菜" }
                        };
                    }
                    else if (hasHeavyRainWarning)
                    {
                        alertResult = new {
                            AlertType = "HeavyRain",
                            Severity = "Medium",
                            Title = "🌧️ 豪大雨特報！",
                            Message = "根據中央氣象署資料，目前有豪大雨特報。瓜果類與葉菜類易受損，價格可能波動，請留意！",
                            AffectedCrops = new[] { "西瓜", "木瓜", "青江菜", "地瓜葉" }
                        };
                    }
                    else
                    {
                        alertResult = new { AlertType = "None" };
                    }
                }
                else
                {
                    alertResult = new { AlertType = "None" };
                }
            }
            catch (System.Exception ex)
            {
                _logger.LogWarning(ex, "Failed to fetch weather alerts from CWA RSS feed. Returning 'None'.");
                alertResult = new { AlertType = "None" };
            }

            // 序列化後存入 Redis 30 分鐘
            var json = JsonSerializer.Serialize(alertResult, new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase });
            await _produceService.SetCachedStringAsync(cacheKey, json, System.TimeSpan.FromMinutes(30));
            return Content(json, "application/json");
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

            // [Bug Fix] 同 GetPriceAnomalies：ToDictionaryAsync 在多市場同一作物時拋出 ArgumentException。
            // 修正：GroupBy ProduceId，計算全台平均價格後建立唯一 Dictionary。
            var todayPrices = await _dbContext.PriceHistories
                .Where(p => p.RecordDate.Date == today)
                .GroupBy(p => p.ProduceId)
                .Select(g => new { ProduceId = g.Key, ProduceName = g.First().ProduceName, AveragePrice = g.Average(p => p.AveragePrice) })
                .ToListAsync();

            var yesterdayPrices = await _dbContext.PriceHistories
                .Where(p => p.RecordDate.Date == yesterday)
                .GroupBy(p => p.ProduceId)
                .Select(g => new { ProduceId = g.Key, AveragePrice = g.Average(p => p.AveragePrice) })
                .ToDictionaryAsync(g => g.ProduceId);

            var priceDrops = new List<PriceAnomalyDto>();
            foreach (var tp in todayPrices)
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
                // 尋找匹配的食譜 (雙向模糊搜尋)
                // [Bug Fix] 原本只做 CropName.Contains(MainIngredient)：
                //   "甘藍".Contains("高麗菜") → false（即使是同一種蔬菜，名稱不同即無法匹配）。
                // 修正：增加反向判斷 MainIngredient.Contains(CropName)，提高匹配率。
                var matchedRecipe = allRecipes.FirstOrDefault(r =>
                    drop.CropName.Contains(r.MainIngredient, StringComparison.OrdinalIgnoreCase) ||
                    r.MainIngredient.Contains(drop.CropName, StringComparison.OrdinalIgnoreCase));
                
                if (matchedRecipe != null)
                {
                    string[]? ingredients = null;
                    string[]? steps = null;
                    try { ingredients = JsonSerializer.Deserialize<string[]>(matchedRecipe.IngredientsJson ?? "[]"); } catch { }
                    try { steps = JsonSerializer.Deserialize<string[]>(matchedRecipe.StepsJson ?? "[]"); } catch { }

                    recipes.Add(new {
                        RecipeName = matchedRecipe.RecipeName,
                        MainIngredients = ingredients ?? System.Array.Empty<string>(),
                        Reason = $"今日 {drop.CropName} 價格大跌 {drop.IncreasePercentage}%，每公斤只要 {drop.CurrentPrice} 元！",
                        ImageUrl = matchedRecipe.ImageUrl,
                        Steps = steps ?? System.Array.Empty<string>()
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
                return Unauthorized(new { Message = "Invalid or missing JWT token" });
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
        // [Bug Fix] 新增 ProduceName 欄位：原本 AddFavorite 不儲存作物名稱，
        // 導致 GetFavorites 回傳時 fav.ProduceName 永遠是 null → 顯示 "Unknown"。
        public string ProduceName { get; set; } = string.Empty;
    }

    // PUT /api/produce/favorites/{produceId} 請求 Body
    public class UpdateFavoriteRequest
    {
        public double TargetPrice { get; set; }
    }
}
