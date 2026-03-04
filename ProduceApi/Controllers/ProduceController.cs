using Microsoft.AspNetCore.Mvc;
using ProduceApi.Models;
using ProduceApi.Services;

namespace ProduceApi.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ProduceController : ControllerBase
{
    private readonly MoaDataService _moaService;

    public ProduceController(MoaDataService moaService)
    {
        _moaService = moaService;
    }

    [HttpGet("daily-prices")]
    public async Task<ActionResult<List<ProducePrice>>> GetDailyPrices([FromQuery] string? marketCode = null)
    {
        var data = await _moaService.GetProduceDataAsync(marketCode);
        return Ok(data);
    }
}
