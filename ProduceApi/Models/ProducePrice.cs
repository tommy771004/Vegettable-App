namespace ProduceApi.Models;

public class ProducePrice
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public List<string> Aliases { get; set; } = new();
    public string Category { get; set; } = string.Empty;
    public string? SubCategory { get; set; }
    public string Description { get; set; } = string.Empty;
    public decimal CurrentPrice { get; set; }
    public decimal AvgPrice3Year { get; set; }
    public string Trend { get; set; } = "stable";
    public string Level { get; set; } = "Fair";
    public string Unit { get; set; } = "kg";
}

public class MoaApiResponse
{
    public string 作物代號 { get; set; } = string.Empty;
    public string 作物名稱 { get; set; } = string.Empty;
    public string 市場代號 { get; set; } = string.Empty;
    public string 市場名稱 { get; set; } = string.Empty;
    public decimal 平均價 { get; set; }
}

public class FisheryApiResponse
{
    public string 魚貨代號 { get; set; } = string.Empty;
    public string 魚貨名稱 { get; set; } = string.Empty;
    public decimal 平均價 { get; set; }
}

public class PoultryApiResponse
{
    public string 品項 { get; set; } = string.Empty;
    public decimal 平均價 { get; set; }
}

public class RiceApiResponse
{
    public string 品項 { get; set; } = string.Empty;
    public decimal 價格 { get; set; }
}
