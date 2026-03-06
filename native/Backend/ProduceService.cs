using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Extensions.Caching.Distributed;
using System.Text;

// =============================================================================
// ProduceService.cs — 農委會 (MOA) API 整合服務
//
// 功能說明：
//   封裝所有與行政院農委會開放資料平台的 HTTP 通訊。
//   加入 Redis 分散式快取層 (24 小時 TTL)，大幅減少對政府 API 的呼叫次數。
//
// 快取策略：Cache-Aside Pattern (旁路快取)
//   1. 查詢 Redis：若快取命中 (Cache Hit) → 直接回傳，跳過後續步驟
//   2. 快取未命中 (Cache Miss) → 呼叫政府 API 取得最新資料
//   3. 將結果寫入 Redis (TTL 24 小時) → 回傳給呼叫者
//
// 農委會 API 說明：
//   資料來源：行政院農委會農糧署 農產品交易行情
//   API URL：https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx
//   資料更新頻率：每日上午 (前一日交易資料)
//   回傳格式：JSON 陣列，欄位包含 CropCode, CropName, MarketCode, AvgPrice, TransQuantity, Date
//
// Redis 快取鍵命名規則：
//   "{InstanceName}{marketCode}" 例："ProduceApi_ALL"
//   InstanceName 在 Program.cs 中設定為 "ProduceApi_"
// =============================================================================

namespace ProduceApi.Services
{
    /// <summary>
    /// 農產品資料服務：整合農委會 API + Redis 快取
    /// 透過 AddHttpClient<ProduceService>() 注入，由框架管理 HttpClient 連線池
    /// </summary>
    public class ProduceService
    {
        private readonly HttpClient _httpClient;
        private readonly IDistributedCache _cache;

        /// <summary>
        /// Redis 快取 TTL：24 小時
        /// 選擇 24 小時的原因：政府 API 每日更新一次，所以快取時間不超過 24 小時仍能保持資料新鮮度
        /// </summary>
        private static readonly TimeSpan CacheDuration = TimeSpan.FromHours(24);

        /// <summary>
        /// 建構子：透過 DI 注入 HttpClient 與 Redis 分散式快取
        /// </summary>
        /// <param name="httpClient">由 IHttpClientFactory 管理的 HttpClient 實例</param>
        /// <param name="cache">Redis 分散式快取 (IDistributedCache)</param>
        public ProduceService(HttpClient httpClient, IDistributedCache cache)
        {
            _httpClient = httpClient;
            _cache = cache;
        }

        /// <summary>
        /// 取得農產品交易行情資料 (Cache-Aside Pattern)
        ///
        /// 流程：Redis 命中 → 直接回傳 / Redis 未命中 → 呼叫農委會 API → 寫入 Redis → 回傳
        /// </summary>
        /// <param name="marketCode">
        ///   市場代碼，留空或 "ALL" 表示取得全台所有市場資料
        ///   例："110" = 台北第一果菜市場, "ALL" = 全台市場
        /// </param>
        /// <returns>農委會 API 回傳的原始 JSON 字串</returns>
        public async Task<string> FetchProduceDataAsync(string marketCode = "")
        {
            // 1. 建立 Redis Cache Key (使用市場代碼作為 Key，空值統一用 "ALL")
            string cacheKey = string.IsNullOrEmpty(marketCode) ? "ALL" : marketCode;

            // 2. 嘗試從 Redis 讀取快取資料
            var cachedData = await _cache.GetStringAsync(cacheKey);
            if (!string.IsNullOrEmpty(cachedData))
            {
                // Cache Hit：直接回傳快取資料，避免打外部 API
                return cachedData;
            }

            // 3. Cache Miss：呼叫農委會開放資料 API
            //    $top=2000 表示最多取 2000 筆 (一般每日全台約有 1500-2000 筆交易記錄)
            string url = "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?$top=2000";
            var response = await _httpClient.GetAsync(url);
            response.EnsureSuccessStatusCode(); // 若回應不是 2xx，拋出 HttpRequestException

            string responseData = await response.Content.ReadAsStringAsync();

            // 4. 將 API 回應寫入 Redis，下次請求直接從 Redis 取得
            //    AbsoluteExpirationRelativeToNow：從「現在起算」24 小時後過期
            await _cache.SetStringAsync(cacheKey, responseData, new DistributedCacheEntryOptions
            {
                AbsoluteExpirationRelativeToNow = CacheDuration
            });

            return responseData;
        }

        /// <summary>
        /// 手動清除指定市場的快取 (用於強制刷新資料)
        /// 一般不需要呼叫此方法，由 Redis TTL 自動過期
        /// 可在未來加入「管理員手動刷新」功能時使用
        /// </summary>
        /// <param name="marketCode">要清除的市場代碼，留空表示清除 "ALL" 的快取</param>
        public async Task ClearCacheAsync(string marketCode = "")
        {
            string cacheKey = string.IsNullOrEmpty(marketCode) ? "ALL" : marketCode;
            await _cache.RemoveAsync(cacheKey);
        }
    }
}
