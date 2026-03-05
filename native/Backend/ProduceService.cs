using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Extensions.Caching.Distributed;
using System.Text;

namespace ProduceApi.Services
{
    public class ProduceService
    {
        private readonly HttpClient _httpClient;
        private readonly IDistributedCache _cache;
        private static readonly TimeSpan CacheDuration = TimeSpan.FromHours(24);

        public ProduceService(HttpClient httpClient, IDistributedCache cache)
        {
            _httpClient = httpClient;
            _cache = cache;
        }

        // Fetch produce data with Redis caching
        public async Task<string> FetchProduceDataAsync(string marketCode = "")
        {
            string cacheKey = string.IsNullOrEmpty(marketCode) ? "ALL" : marketCode;

            // Return cached data if valid
            var cachedData = await _cache.GetStringAsync(cacheKey);
            if (!string.IsNullOrEmpty(cachedData))
            {
                return cachedData;
            }

            // Fetch from MOA API
            string url = "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?$top=2000";
            var response = await _httpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            string responseData = await response.Content.ReadAsStringAsync();

            // Save to cache
            await _cache.SetStringAsync(cacheKey, responseData, new DistributedCacheEntryOptions
            {
                AbsoluteExpirationRelativeToNow = CacheDuration
            });

            return responseData;
        }

        public async Task ClearCacheAsync(string marketCode = "")
        {
            string cacheKey = string.IsNullOrEmpty(marketCode) ? "ALL" : marketCode;
            await _cache.RemoveAsync(cacheKey);
        }
    }
}
