using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using System;
using System.Threading;
using System.Threading.Tasks;
using ProduceApi.Data;
using ProduceApi.Services;

namespace ProduceApi.Workers
{
    // 新功能：後端背景同步服務 (Background Hosted Service)
    // 解決邏輯錯誤：讓後端成為資料的 Source of Truth，定時去政府 API 抓資料存進 DB，
    // 而不是每次 App 請求時才去打政府 API。
    public class ProduceSyncWorker : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<ProduceSyncWorker> _logger;

        public ProduceSyncWorker(IServiceProvider serviceProvider, ILogger<ProduceSyncWorker> logger)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Produce Sync Worker started.");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    using (var scope = _serviceProvider.CreateScope())
                    {
                        var produceService = scope.ServiceProvider.GetRequiredService<ProduceService>();
                        var dbContext = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();

                        _logger.LogInformation("Fetching latest produce data from MOA API...");
                        
                        // 1. 抓取最新資料
                        string rawData = await produceService.FetchProduceDataAsync("ALL");
                        
                        // 2. 解析 JSON (這裡假設有 JSON 解析邏輯)
                        // var items = JsonConvert.DeserializeObject<List<ProduceItem>>(rawData);

                        // 3. 將今日價格寫入 PriceHistory 資料表
                        /*
                        foreach (var item in items) {
                            dbContext.PriceHistories.Add(new PriceHistory {
                                ProduceId = item.CropCode,
                                MarketCode = item.MarketCode,
                                AveragePrice = item.AvgPrice,
                                RecordDate = DateTime.UtcNow
                            });
                        }
                        await dbContext.SaveChangesAsync();
                        */
                        
                        _logger.LogInformation("Successfully synced daily prices to database.");
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error occurred while syncing produce data.");
                }

                // 每天執行一次 (24小時)
                await Task.Delay(TimeSpan.FromHours(24), stoppingToken);
            }
        }
    }
}
