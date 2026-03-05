using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;
using ProduceApi.Services;
using ProduceApi.Workers;

var builder = WebApplication.CreateBuilder(args);

// 邏輯修正與優化 1：後端依賴注入 (Dependency Injection) 與啟動配置
// 解決問題：之前的 Controller、DbContext 和 Worker 雖然寫好了，但沒有註冊到應用程式中，後端根本無法啟動。
// 現在透過 Program.cs 將所有服務串聯起來，讓 C# 後端成為一個真正可執行的 Web API 專案。

// 1. 註冊 Entity Framework Core 資料庫 (這裡以 SQLite 為例，實務上可換成 SQL Server 或 PostgreSQL)
builder.Services.AddDbContext<ProduceDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")));

// 1.1 註冊 Redis 快取
builder.Services.AddStackExchangeRedisCache(options =>
{
    options.Configuration = builder.Configuration.GetConnectionString("Redis");
    options.InstanceName = "ProduceApi_";
});

// 2. 註冊 HttpClient，讓 ProduceService 可以向政府 API 發送請求
builder.Services.AddHttpClient<ProduceService>();

// 3. 註冊背景同步服務 (每天定時抓取政府資料)
builder.Services.AddHostedService<ProduceSyncWorker>();

// 新增功能：主動式推播通知 (Push Notifications)
builder.Services.AddHostedService<ProduceApi.PriceAlertWorker>();

// 4. 註冊 Controllers
builder.Services.AddControllers();

// 5. 加入 CORS 允許跨域請求 (如果未來有 Web 管理後台需要接 API)
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader();
    });
});

var app = builder.Build();

// 確保資料庫建立
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();
    DbInitializer.Initialize(db);
}

app.UseCors("AllowAll");
app.UseAuthorization();
app.MapControllers();

app.Run();
