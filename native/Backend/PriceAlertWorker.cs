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

// =============================================================================
// PriceAlertWorker.cs — 背景服務：主動式價格提醒推播
//
// 功能說明：
//   實作 IHostedService 的 BackgroundService，應用程式啟動時自動在背景執行。
//   每 24 小時掃描一次資料庫中所有「我的收藏」(UserFavorites)，
//   若農產品當日均價 ≤ 使用者設定的目標價，透過 Firebase Cloud Messaging (FCM)
//   向對應的手機送出推播通知。
//
// Bug Fix (本次修正)：
//   原本錯誤地使用 fav.UserId 作為 FCM Token，但 UserId 是應用程式內部識別碼
//   (UUID 格式，例："550e8400-e29b-41d4-a716-446655440000")，
//   FCM Token 是 Firebase 為每一台裝置核發的完全不同格式的字串。
//   修正後改從 UserStats 表格中查詢 FcmToken 欄位。
//
// FCM Token 生命週期：
//   行動端 App 啟動時取得 FCM Token → 呼叫 POST /api/produce/fcm-token 上傳到後端
//   → 後端存入 UserStats.FcmToken → 本 Worker 讀取 UserStats.FcmToken 發送推播
//
// 設定需求：
//   - FIREBASE_CREDENTIALS_PATH 環境變數 或 firebase-service-account.json 放在執行目錄
//   - firebase-service-account.json 可從 Firebase Console → 專案設定 → 服務帳戶 下載
// =============================================================================

namespace ProduceApi
{
    /// <summary>
    /// 主動式推播通知背景服務
    /// 每天清晨自動掃描所有使用者收藏，一旦達到目標價格，透過 FCM 發送手機推播
    /// </summary>
    public class PriceAlertWorker : BackgroundService
    {
        private readonly ILogger<PriceAlertWorker> _logger;
        private readonly IServiceProvider _serviceProvider;

        /// <summary>
        /// 建構子：在應用程式啟動時初始化 Firebase Admin SDK
        /// </summary>
        /// <param name="logger">DI 注入的日誌服務</param>
        /// <param name="serviceProvider">DI 服務容器 (用於建立 Scoped 的 DbContext)</param>
        public PriceAlertWorker(ILogger<PriceAlertWorker> logger, IServiceProvider serviceProvider)
        {
            _logger = logger;
            _serviceProvider = serviceProvider;

            // Firebase Admin SDK 只能初始化一次，透過 DefaultInstance 防止重複初始化
            if (FirebaseApp.DefaultInstance == null)
            {
                try
                {
                    // 優先從環境變數讀取憑證路徑，否則預設使用執行目錄下的 firebase-service-account.json
                    // 建議：生產環境使用環境變數，避免 JSON 金鑰檔案被意外提交到版本控制
                    var serviceAccountPath = Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH")
                        ?? "firebase-service-account.json";

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
                        // 檔案不存在時記錄警告，但不中止應用程式啟動
                        // Push notification 功能將停用，其他 API 仍正常運作
                        _logger.LogWarning($"Firebase service account file not found at '{serviceAccountPath}'. Push notifications will be disabled.");
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Failed to initialize Firebase App. Push notifications will be disabled.");
                }
            }
        }

        /// <summary>
        /// BackgroundService 核心執行迴圈
        /// 每 24 小時呼叫一次 CheckAndSendAlertsAsync
        /// </summary>
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("PriceAlertWorker started. Will check price alerts every 24 hours.");

            // stoppingToken 在應用程式關閉時被取消，此時迴圈優雅退出
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await CheckAndSendAlertsAsync();
                }
                catch (Exception ex)
                {
                    // 捕獲所有例外，避免 Worker 因單次執行失敗而停止背景服務
                    _logger.LogError(ex, "Error occurred while checking price alerts. Will retry in 24 hours.");
                }

                // 等待 24 小時後再次執行
                // Task.Delay 支援 CancellationToken，應用程式關閉時可立即中止等待
                await Task.Delay(TimeSpan.FromHours(24), stoppingToken);
            }
        }

        /// <summary>
        /// 核心業務邏輯：掃描所有收藏，比對最新價格，發送達標推播
        /// </summary>
        private async Task CheckAndSendAlertsAsync()
        {
            // 使用 CreateScope 建立新的 DI 作用域
            // 原因：BackgroundService 是 Singleton，但 DbContext 是 Scoped，
            //       必須手動建立 Scope 才能正確使用 DbContext，避免物件生命週期衝突
            using var scope = _serviceProvider.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();
            var produceService = scope.ServiceProvider.GetRequiredService<ProduceService>();

            // 1. 取得所有使用者的收藏清單
            var favorites = await dbContext.UserFavorites.ToListAsync();
            if (!favorites.Any())
            {
                _logger.LogInformation("No favorites found. Skipping price alert check.");
                return;
            }

            _logger.LogInformation($"Checking price alerts for {favorites.Count} favorite(s)...");

            // 2. 從政府 API 或 Redis 快取取得最新農產品價格
            string rawData = await produceService.FetchProduceDataAsync("ALL");
            var moaItems = JsonSerializer.Deserialize<List<MoaProduceDto>>(
                rawData,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
            ) ?? new List<MoaProduceDto>();

            // 3. 預先載入所有相關使用者的 FCM Token (避免在迴圈中重複查詢 DB)
            var userIds = favorites.Select(f => f.UserId).Distinct().ToList();
            var userStats = await dbContext.UserStats
                .Where(u => userIds.Contains(u.UserId))
                .ToDictionaryAsync(u => u.UserId);

            // 4. 逐一比對收藏項目與最新市場價格
            foreach (var fav in favorites)
            {
                // 搜尋此農產品的最新均價
                var latestData = moaItems.FirstOrDefault(m => m.CropCode == fav.ProduceId);

                // 條件：有找到價格資料，且當日均價 ≤ 使用者設定的目標價 → 觸發通知
                if (latestData != null && latestData.AvgPrice > 0 && latestData.AvgPrice <= fav.TargetPrice)
                {
                    string title = "🛒 蔬果特價通知";
                    string body = $"您關注的「{latestData.CropName}」今天跌至 {latestData.AvgPrice:F0} 元/公斤，現在買最划算！";

                    _logger.LogInformation($"[ALERT TRIGGERED] User={fav.UserId}, Crop={latestData.CropName}, Price={latestData.AvgPrice}, Target={fav.TargetPrice}");

                    // 5. [BUG FIX] 從 UserStats 表格查詢使用者的真實 FCM Token
                    //    修正前的錯誤：Token = fav.UserId (UserId 是 UUID，不是 FCM Token)
                    //    修正後：從 UserStats.FcmToken 欄位讀取
                    //    此欄位由行動端 App 登入後呼叫 POST /api/produce/fcm-token 更新
                    if (!userStats.TryGetValue(fav.UserId, out var userStat)
                        || string.IsNullOrEmpty(userStat.FcmToken))
                    {
                        _logger.LogWarning($"No FCM token found for user {fav.UserId}. Skipping push notification.");
                        continue; // 跳過沒有 FCM Token 的使用者 (例：未授權推播的裝置)
                    }

                    // 6. 透過 Firebase Admin SDK 發送 FCM 推播
                    if (FirebaseApp.DefaultInstance != null)
                    {
                        try
                        {
                            var message = new Message()
                            {
                                // 使用正確的 FCM Token (從 UserStats.FcmToken 讀取)
                                Token = userStat.FcmToken,

                                // 推播通知的標題和內文
                                Notification = new Notification()
                                {
                                    Title = title,
                                    Body = body
                                },

                                // 自訂資料 Payload，讓 App 點擊通知後可導航到對應農產品頁面
                                Data = new Dictionary<string, string>()
                                {
                                    { "produceId", fav.ProduceId },
                                    { "price", latestData.AvgPrice.ToString("F2") },
                                    { "targetPrice", fav.TargetPrice.ToString("F2") }
                                }
                            };

                            string response = await FirebaseMessaging.DefaultInstance.SendAsync(message);
                            _logger.LogInformation($"FCM message sent successfully. MessageId={response}");
                        }
                        catch (Exception ex)
                        {
                            // 記錄失敗原因，但不中斷其他使用者的通知處理
                            _logger.LogError(ex, $"Failed to send FCM message to user {fav.UserId} (token ending: ...{userStat.FcmToken[^4..]})");
                        }
                    }
                    else
                    {
                        // Firebase 未初始化時，僅記錄日誌 (開發環境 Fallback)
                        _logger.LogInformation($"[SIMULATED PUSH] To User {fav.UserId}: {title} - {body}");
                    }
                }
            }

            _logger.LogInformation("Price alert check completed.");
        }
    }
}
