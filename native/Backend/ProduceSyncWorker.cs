using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Text.Json;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;
using ProduceApi.Services;
using ProduceApi.Models;

// =============================================================================
// ProduceSyncWorker.cs — 背景服務：每日定時同步農委會 (MOA) 資料
//
// 功能說明：
//   實作 IHostedService 的 BackgroundService，應用程式啟動時自動在背景執行。
//   每 24 小時向行政院農委會開放資料平台抓取最新農產品批發價格，
//   並將其持久化到本地 SQLite 資料庫的 PriceHistories 表格中。
//
// 架構優勢 (對比「每次 API 請求才打政府 API」的方案)：
//   ✅ 後端成為 Source of Truth，減少對外部 API 的即時依賴
//   ✅ 歷史價格資料可用於「趨勢分析」與「異常偵測」功能
//   ✅ 政府 API 偶發性不穩定不會影響使用者體驗 (Redis 快取 + DB 保底)
//   ✅ 避免政府 API 的 Rate Limit 限制 (只在後端定時呼叫，而非每個使用者請求都呼叫)
//
// Bug Fix (本次修正)：
//   原本每次執行都無條件 Add 新記錄，若同一天執行多次 (例如重啟服務) 會產生重複資料。
//   修正後改為 Upsert 邏輯：
//   - 若當天 (ProduceId + MarketCode + RecordDate) 已有記錄 → 更新均價
//   - 若當天沒有記錄 → 新增
//   使用批次查詢取代迴圈中 N 次 DB 查詢，大幅提升 2000+ 筆資料時的效能
// =============================================================================

namespace ProduceApi.Workers
{
    /// <summary>
    /// 農產品每日價格同步背景服務
    /// 每 24 小時從農委會 API 拉取最新批發價，使用 Upsert 寫入 PriceHistories
    /// </summary>
    public class ProduceSyncWorker : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<ProduceSyncWorker> _logger;

        /// <summary>
        /// 建構子：注入 DI 服務容器與日誌服務
        /// </summary>
        /// <param name="serviceProvider">用於建立 Scoped DbContext 的服務容器</param>
        /// <param name="logger">結構化日誌輸出</param>
        public ProduceSyncWorker(IServiceProvider serviceProvider, ILogger<ProduceSyncWorker> logger)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
        }

        /// <summary>
        /// BackgroundService 核心執行迴圈：每 24 小時同步一次
        /// </summary>
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("ProduceSyncWorker started. Will sync MOA data every 24 hours.");

            // 應用程式關閉時，stoppingToken 被取消，迴圈優雅退出
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await SyncProduceDataAsync();
                }
                catch (Exception ex)
                {
                    // 捕獲所有例外，確保 Worker 不會因單次失敗而停止
                    _logger.LogError(ex, "Error occurred during produce data sync. Will retry in 24 hours.");
                }

                // 等待 24 小時後執行下一次同步
                await Task.Delay(TimeSpan.FromHours(24), stoppingToken);
            }
        }

        /// <summary>
        /// 核心業務邏輯：從 MOA API 拉取資料，執行 Upsert 寫入資料庫
        /// </summary>
        private async Task SyncProduceDataAsync()
        {
            // 使用 CreateScope 建立新的 DI 作用域
            // 原因：BackgroundService 是 Singleton 生命週期，DbContext 是 Scoped 生命週期，
            //       必須手動建立 Scope 才能正確解析 DbContext，避免「Captive Dependency」問題
            using var scope = _serviceProvider.CreateScope();
            var produceService = scope.ServiceProvider.GetRequiredService<ProduceService>();
            var dbContext = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();

            _logger.LogInformation("Starting MOA produce data sync...");

            // 1. 透過 ProduceService 取得資料 (優先從 Redis 快取讀取，Cache Miss 才打政府 API)
            string rawData = await produceService.FetchProduceDataAsync("ALL");

            // 2. 將 JSON 字串解析成 MoaProduceDto 列表
            //    PropertyNameCaseInsensitive = true：允許 JSON key 大小寫不敏感匹配
            var items = JsonSerializer.Deserialize<List<MoaProduceDto>>(
                rawData,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
            ) ?? new List<MoaProduceDto>();

            if (items.Count == 0)
            {
                _logger.LogWarning("MOA API returned 0 items. Sync aborted.");
                return;
            }

            // 3. 取得今日的 UTC 日期 (去除時間部分，只保留日期，方便後續日期比對)
            var today = DateTime.UtcNow.Date;

            // 4. [BUG FIX] Upsert 邏輯：批次查詢今天已存在的記錄，避免重複插入
            //
            //    原本的 Bug：
            //      foreach (var item in items) { dbContext.PriceHistories.Add(new PriceHistory {...}) }
            //      → 若服務重啟，同一天會有 2000+ 筆重複資料，嚴重污染歷史資料
            //
            //    修正後的方案：
            //      先批次查詢今天的所有記錄 → 建立 lookup Dictionary → 逐筆判斷 Insert or Update
            //      使用 Dictionary 查詢複雜度 O(1)，避免 N+1 Query 問題
            var todayKeys = await dbContext.PriceHistories
                .Where(p => p.RecordDate.Date == today)
                .Select(p => new { p.ProduceId, p.MarketCode, p.Id })
                .ToListAsync();

            // Key = "{ProduceId}_{MarketCode}"，確保同一農產品在不同市場各自獨立更新
            var existingDict = todayKeys.ToDictionary(
                k => $"{k.ProduceId}_{k.MarketCode}",
                k => k.Id
            );

            int insertCount = 0;
            int updateCount = 0;

            foreach (var item in items)
            {
                // 跳過沒有代碼的無效資料
                if (string.IsNullOrEmpty(item.CropCode)) continue;

                string key = $"{item.CropCode}_{item.MarketCode}";

                if (existingDict.TryGetValue(key, out int existingId))
                {
                    // 今日已有記錄 → 更新均價 (避免重複插入)
                    // 使用 ExecuteUpdateAsync 直接在 DB 執行 UPDATE，不需要先 SELECT 整個 Entity
                    await dbContext.PriceHistories
                        .Where(p => p.Id == existingId)
                        .ExecuteUpdateAsync(s => s
                            .SetProperty(p => p.AveragePrice, item.AvgPrice)
                            .SetProperty(p => p.ProduceName, item.CropName)
                        );
                    updateCount++;
                }
                else
                {
                    // 今日尚無記錄 → 新增一筆歷史記錄
                    dbContext.PriceHistories.Add(new PriceHistory
                    {
                        ProduceId = item.CropCode,
                        ProduceName = item.CropName,
                        MarketCode = item.MarketCode,
                        AveragePrice = item.AvgPrice,
                        RecordDate = today  // 儲存日期部分 (忽略時間)
                    });
                    insertCount++;
                }
            }

            // 5. 批次提交所有新增的記錄 (UPDATE 已在迴圈中即時執行)
            await dbContext.SaveChangesAsync();

            _logger.LogInformation(
                $"Sync completed. Total={items.Count}, Inserted={insertCount}, Updated={updateCount}, Date={today:yyyy-MM-dd}"
            );
        }
    }
}
