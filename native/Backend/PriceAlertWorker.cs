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
using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;

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
            
            // Initialize Firebase App if not already initialized
            if (FirebaseApp.DefaultInstance == null)
            {
                try 
                {
                    // In a real scenario, point to your service account json file
                    // defined in appsettings.json or environment variable
                    var serviceAccountPath = Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH") ?? "firebase-service-account.json";
                    
                    if (System.IO.File.Exists(serviceAccountPath))
                    {
                        FirebaseApp.Create(new AppOptions()
                        {
                            Credential = GoogleCredential.FromFile(serviceAccountPath)
                        });
                        _logger.LogInformation("Firebase App initialized successfully.");
                    }
                    else
                    {
                        _logger.LogWarning($"Firebase service account file not found at {serviceAccountPath}. Push notifications will be simulated.");
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Failed to initialize Firebase App.");
                }
            }
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

                // 每天清晨執行一次 (這裡設定為每 24 小時)
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
                    string title = "價格下跌通知";
                    string body = $"您關注的 {latestData.CropName} 今天跌至 {latestData.AvgPrice} 元囉，現在買最划算！";

                    _logger.LogInformation($"[PUSH NOTIFICATION] To User {fav.UserId}: 🔔 {body}");
                    
                    // Send real FCM message if Firebase is initialized
                    if (FirebaseApp.DefaultInstance != null)
                    {
                        try
                        {
                            // Assuming UserId is the FCM token or we have a mapping. 
                            // For this demo, we assume the UserId stored IS the FCM token (simplified).
                            // In a real app, you'd have a UserTokens table mapping UserId -> FcmToken.
                            
                            var message = new Message()
                            {
                                Token = fav.UserId, // Simplified: Using UserId as FCM Token
                                Notification = new Notification()
                                {
                                    Title = title,
                                    Body = body
                                },
                                Data = new Dictionary<string, string>()
                                {
                                    { "produceId", fav.ProduceId },
                                    { "price", latestData.AvgPrice.ToString() }
                                }
                            };

                            string response = await FirebaseMessaging.DefaultInstance.SendAsync(message);
                            _logger.LogInformation($"Successfully sent message: {response}");
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError(ex, $"Failed to send FCM message to {fav.UserId}");
                        }
                    }
                }
            }
        }
    }
}
