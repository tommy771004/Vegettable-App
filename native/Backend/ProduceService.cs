using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Extensions.Caching.Memory;

namespace ProduceApi.Services
{
    public class ProduceService
    {
        private readonly HttpClient _httpClient;
        private readonly IMemoryCache _cache;
        private static readonly TimeSpan CacheDuration = TimeSpan.FromMinutes(5);

        public ProduceService(HttpClient httpClient, IMemoryCache cache)
        {
            _httpClient = httpClient;
            _cache = cache;
        }

        // Fetch produce data with in-memory caching
        public async Task<string> FetchProduceDataAsync(string marketCode = "")
        {
            string cacheKey = string.IsNullOrEmpty(marketCode) ? "ALL" : marketCode;

            // Return cached data if valid
            if (_cache.TryGetValue(cacheKey, out string cachedData))
            {
                return cachedData;
            }

            // Fetch from MOA API
            string url = "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?$top=2000";
            var response = await _httpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            string responseData = await response.Content.ReadAsStringAsync();

            // Save to cache
            _cache.Set(cacheKey, responseData, CacheDuration);

            return responseData;
        }

        public void ClearCache()
        {
            // Note: IMemoryCache doesn't have a direct Clear() method. 
            // In a real app, you would use CancellationTokens to evict cache entries.
        }
    }
}
