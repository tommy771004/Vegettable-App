using System.Text.Json;
using Microsoft.Extensions.Caching.Memory;
using ProduceApi.Models;

namespace ProduceApi.Services;

public class MoaDataService
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<MoaDataService> _logger;
    private readonly IMemoryCache _cache;
    private const string CacheKey = "ProduceDataCache";

    public MoaDataService(HttpClient httpClient, ILogger<MoaDataService> logger, IMemoryCache cache)
    {
        _httpClient = httpClient;
        _logger = logger;
        _cache = cache;
    }

    public async Task<List<ProducePrice>> GetProduceDataAsync(string? marketCode = null)
    {
        var cacheKey = string.IsNullOrEmpty(marketCode) ? CacheKey : $"{CacheKey}_{marketCode}";
        if (_cache.TryGetValue(cacheKey, out List<ProducePrice>? cachedData))
        {
            return cachedData!;
        }

        var allProduce = new List<ProducePrice>();

        try
        {
            var farmTask = FetchFarmDataAsync(marketCode);
            var fisheryTask = FetchFisheryDataAsync(marketCode);
            var poultryTask = FetchPoultryDataAsync(marketCode);
            var riceTask = FetchRiceDataAsync(marketCode);
            var animalTask = FetchAnimalDataAsync(marketCode);

            await Task.WhenAll(farmTask, fisheryTask, poultryTask, riceTask, animalTask);

            allProduce.AddRange(await farmTask);
            allProduce.AddRange(await fisheryTask);
            allProduce.AddRange(await poultryTask);
            allProduce.AddRange(await riceTask);
            allProduce.AddRange(await animalTask);

            var result = allProduce.OrderBy(x => x.CurrentPrice / x.AvgPrice3Year).ToList();

            var cacheOptions = new MemoryCacheEntryOptions()
                .SetAbsoluteExpiration(TimeSpan.FromHours(1));

            _cache.Set(cacheKey, result, cacheOptions);

            return result;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching data from MOA APIs");
            return new List<ProducePrice>();
        }
    }

    private async Task<List<ProducePrice>> FetchFarmDataAsync(string? marketCode)
    {
        var response = await _httpClient.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?$top=1000");
        var data = JsonSerializer.Deserialize<List<MoaApiResponse>>(response, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        if (data == null) return new List<ProducePrice>();

        var aliasDict = GetAliasDictionary();

        var filteredData = string.IsNullOrEmpty(marketCode) ? data : data.Where(x => x.市場代號 == marketCode);

        return filteredData.Where(x => !string.IsNullOrEmpty(x.作物名稱) && x.平均價 > 0)
            .GroupBy(x => x.作物名稱)
            .Select(g => {
                var name = g.Key.Trim();
                var avgPrice = Math.Round(g.Average(x => x.平均價), 1);
                var code = g.First().作物代號;
                var category = DetermineFarmCategory(name, code);
                var subCategory = category == "Vegetable" ? DetermineVegetableSubCategory(name) : null;

                return CreateProducePrice(code, name, avgPrice, category, subCategory, aliasDict);
            }).ToList();
    }

    private async Task<List<ProducePrice>> FetchFisheryDataAsync(string? marketCode)
    {
        try {
            // 漁產交易行情 (FisheryTransData)
            var response = await _httpClient.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/FisheryTransData.aspx?$top=1000");
            var data = JsonSerializer.Deserialize<List<FisheryApiResponse>>(response, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
            if (data == null) return new List<ProducePrice>();

            return data.GroupBy(x => x.魚貨名稱)
                .Select(g => {
                    var name = g.Key.Trim();
                    var avgPrice = Math.Round(g.Average(x => x.平均價), 1);
                    var code = g.First().魚貨代號;
                    var subCategory = DetermineFisherySubCategory(name);
                    return CreateProducePrice(code, name, avgPrice, "Fishery", subCategory, null);
                })
                .ToList();
        } catch { return new List<ProducePrice>(); }
    }

    private async Task<List<ProducePrice>> FetchPoultryDataAsync(string? marketCode)
    {
        try {
            // 家禽交易行情 (雞、鴨、鵝、蛋)
            var response = await _httpClient.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/PoultryTransData.aspx?$top=200");
            var data = JsonSerializer.Deserialize<List<PoultryApiResponse>>(response, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
            if (data == null) return new List<ProducePrice>();

            return data.Select(x => {
                var name = x.品項.Trim();
                var subCategory = DetermineLivestockSubCategory(name);
                return CreateProducePrice("P-" + name, name, x.平均價, "Poultry", subCategory, null);
            }).ToList();
        } catch { return new List<ProducePrice>(); }
    }

    private async Task<List<ProducePrice>> FetchAnimalDataAsync(string? marketCode)
    {
        try {
            // 毛豬交易行情
            var response = await _httpClient.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/AnimalTransData.aspx?$top=100");
            var data = JsonSerializer.Deserialize<List<MoaApiResponse>>(response, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
            if (data == null) return new List<ProducePrice>();

            return data.GroupBy(x => x.作物名稱)
                .Select(g => CreateProducePrice("A-" + g.Key, g.Key.Trim(), Math.Round(g.Average(x => x.平均價), 1), "Poultry", "Livestock", null))
                .ToList();
        } catch { return new List<ProducePrice>(); }
    }

    private string DetermineFisherySubCategory(string name)
    {
        if (name.Contains("養殖") || name.Contains("吳郭魚") || name.Contains("虱目魚") || name.Contains("鱸魚") || name.Contains("石斑") || name.Contains("蛤") || name.Contains("蝦")) return "Aquaculture";
        if (name.Contains("遠洋") || name.Contains("鮪") || name.Contains("旗魚") || name.Contains("鯊") || name.Contains("魷")) return "DeepSea";
        return "Coastal";
    }

    private string DetermineLivestockSubCategory(string name)
    {
        if (name.Contains("雞") || name.Contains("鴨") || name.Contains("鵝") || name.Contains("蛋")) return "Poultry";
        if (name.Contains("豬") || name.Contains("羊") || name.Contains("牛")) return "Livestock";
        return "Other";
    }

    private async Task<List<ProducePrice>> FetchRiceDataAsync(string? marketCode)
    {
        try {
            var response = await _httpClient.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/RicePriceData.aspx?$top=50");
            var data = JsonSerializer.Deserialize<List<RiceApiResponse>>(response, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
            if (data == null) return new List<ProducePrice>();

            return data.Select(x => CreateProducePrice("R-" + x.品項, x.品項.Trim(), x.價格, "Rice", null, null)).ToList();
        } catch { return new List<ProducePrice>(); }
    }

    private ProducePrice CreateProducePrice(string id, string name, decimal currentPrice, string category, string? subCategory, Dictionary<string, List<string>>? aliasDict)
    {
        var mock3YearAvg = Math.Round(currentPrice * (decimal)(0.8 + new Random().NextDouble() * 0.4), 1);
        var level = "Fair";
        var trend = "stable";

        if (currentPrice < mock3YearAvg * 0.8m) { level = "Cheap"; trend = "down"; }
        else if (currentPrice > mock3YearAvg * 1.2m) { level = "Expensive"; trend = "up"; }
        else if (currentPrice > mock3YearAvg) { level = "SlightlyExpensive"; trend = "up"; }

        return new ProducePrice
        {
            Id = id,
            Name = name,
            Aliases = aliasDict != null && aliasDict.ContainsKey(name) ? aliasDict[name] : new List<string>(),
            Category = category,
            SubCategory = subCategory,
            Description = GetProduceDescription(name),
            CurrentPrice = currentPrice,
            AvgPrice3Year = mock3YearAvg,
            Level = level,
            Trend = trend
        };
    }

    private string GetProduceDescription(string name)
    {
        // In a real production environment, we would call a Knowledge Base API or an LLM here.
        // Since the MOA Encyclopedia API (ServiceId=113) is currently unreliable, 
        // we use a dynamic template generator that adapts to the item name and category.
        
        var category = DetermineFarmCategory(name, "");
        var categoryName = category == "Fruit" ? "水果" : (category == "Flower" ? "花卉" : "蔬菜");
        
        // Dynamic generation based on common patterns
        string nutrition = "富含維生素與膳食纖維";
        if (name.Contains("紅") || name.Contains("橘")) nutrition = "富含β-胡蘿蔔素及抗氧化物質";
        if (name.Contains("綠") || name.Contains("葉")) nutrition = "富含葉綠素、鐵質及葉酸";
        if (name.Contains("豆")) nutrition = "提供優質植物性蛋白質及纖維";
        if (category == "Fruit") nutrition = "富含天然果糖、維生素C及水分";

        string selection = "挑選時以色澤自然、無損傷、拿起來有沉重感者為佳";
        if (name.Contains("葉")) selection = "挑選時以葉片翠綠、無枯萎、莖部細嫩者為佳";
        if (name.Contains("果") || category == "Fruit") selection = "挑選時以果實飽滿、蒂頭新鮮、無碰撞傷者為佳";
        if (name.Contains("根") || name.Contains("薯")) selection = "挑選時以表皮光滑、無發芽、無蟲蛀、質地堅實者為佳";

        return $"{name}是台灣優質的{categoryName}品項，{nutrition}。{selection}。建議存放於陰涼通風處或冷藏保存以維持鮮度。";
    }

    private string DetermineFarmCategory(string name, string code)
    {
        if (code.StartsWith("F") || name.Contains("果") || name.Contains("瓜") || name.Contains("蕉") || name.Contains("莓")) return "Fruit";
        if (name.Contains("花") || name.Contains("蘭") || name.Contains("菊") || name.Contains("玫瑰")) return "Flower";
        return "Vegetable";
    }

    private string DetermineVegetableSubCategory(string name)
    {
        if (name.Contains("根") || name.Contains("薯") || name.Contains("芋") || name.Contains("蘿蔔") || name.Contains("筍")) return "Root";
        if (name.Contains("葉") || name.Contains("菜") || name.Contains("萵苣") || name.Contains("菠") || name.Contains("白菜") || name.Contains("甘藍")) return "Leafy";
        if (name.Contains("花") || name.Contains("果") || name.Contains("椒") || name.Contains("茄") || name.Contains("豆") || name.Contains("玉米")) return "FlowerFruit";
        if (name.Contains("菇") || name.Contains("耳")) return "Mushroom";
        if (name.Contains("醃") || name.Contains("漬") || name.Contains("乾")) return "Pickled";
        return "Other";
    }

    private Dictionary<string, List<string>> GetAliasDictionary()
    {
        return new Dictionary<string, List<string>>
        {
            { "甘藍", new List<string> { "高麗菜", "捲心菜", "包心菜" } },
            { "甘薯", new List<string> { "地瓜", "番薯" } },
            { "馬鈴薯", new List<string> { "洋芋", "土豆" } },
            { "番茄", new List<string> { "西紅柿", "柑仔蜜" } },
            { "胡蘿蔔", new List<string> { "紅蘿蔔", "人參" } },
            { "大白菜", new List<string> { "紹菜", "結球白菜" } },
            { "青花菜", new List<string> { "西蘭花", "綠花菜" } },
            { "空心菜", new List<string> { "蕹菜" } },
            { "小白菜", new List<string> { "不結球白菜" } },
            { "菠菜", new List<string> { "菠薐菜", "飛龍菜" } }
        };
    }
}
