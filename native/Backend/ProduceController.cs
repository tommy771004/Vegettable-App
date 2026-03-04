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
                var allItems = JsonSerializer.Deserialize<List<ProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<ProduceDto>();

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
