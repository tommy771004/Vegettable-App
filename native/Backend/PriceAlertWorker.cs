using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.DependencyInjection;
using System;
using System.Threading;
using System.Threading.Tasks;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;
using ProduceApi.Services;
using System.Text.Json;
using System.Collections.Generic;
using ProduceApi.Models;

namespace ProduceApi
{
    // 新增功能：主動式推播通知 (Push Notifications)
    // 每天清晨自動掃描所有使用者的 TargetPrice，一旦達標，發送手機推播
    public class PriceAlertWorker : BackgroundService
    {
        private readonly ILogger<PriceAlertWorker> _logger;
        private readonly IServiceProvider _serviceProvider;

        public PriceAlertWorker(ILogger<PriceAlertWorker> logger, IServiceProvider serviceProvider)
        {
            _logger = logger;
            _serviceProvider = serviceProvider;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("PriceAlertWorker started.");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await CheckAndSendAlertsAsync();
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error occurred while checking price alerts.");
                }

                // 模擬每天清晨執行一次 (這裡設定為每 24 小時)
                await Task.Delay(TimeSpan.FromHours(24), stoppingToken);
            }
        }

        private async Task CheckAndSendAlertsAsync()
        {
            using var scope = _serviceProvider.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();
            var produceService = scope.ServiceProvider.GetRequiredService<ProduceService>();

            var favorites = await dbContext.UserFavorites.ToListAsync();
            if (!favorites.Any()) return;

            // 取得最新價格資料
            string rawData = await produceService.FetchProduceDataAsync("ALL");
            var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(rawData, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<MoaProduceDto>();

            foreach (var fav in favorites)
            {
                var latestData = moaItems.FirstOrDefault(m => m.CropCode == fav.ProduceId);
                if (latestData != null && latestData.AvgPrice > 0 && latestData.AvgPrice <= fav.TargetPrice)
                {
                    // 模擬發送 FCM / APNs 推播通知
                    _logger.LogInformation($"[PUSH NOTIFICATION] To User {fav.UserId}: 🔔 您關注的 {latestData.CropName} 今天跌至 {latestData.AvgPrice} 元囉，現在買最划算！");
                    
                    // 實際應用中，這裡會呼叫 FirebaseAdmin SDK 或 APNs API
                    // 例如: FirebaseMessaging.DefaultInstance.SendAsync(message);
                }
            }
        }
    }
}
