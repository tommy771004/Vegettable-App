using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;
using ProduceApi.Services;
using ProduceApi.Workers;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Text;

// =============================================================================
// Program.cs — ASP.NET Core 8 應用程式進入點與服務設定
//
// 架構說明：
//   此檔案使用 .NET 6+ 的「Minimal Hosting Model」，將應用程式設定集中在一個地方。
//   採用 Builder Pattern：
//     1. builder.Services.Add*() → 向 DI 容器「登記」各種服務
//     2. var app = builder.Build() → 建立實際的應用程式實例
//     3. app.Use*() → 設定 HTTP 請求處理 Middleware 管線
//     4. app.Run() → 啟動 Web Server 開始監聽請求
//
// 服務架構圖：
//   Request → CORS → Authentication (JWT) → Authorization → Controller
//                                                              ↓
//                                                        DbContext (SQLite)
//                                                        ProduceService (Redis Cache → MOA API)
//                                                        BackgroundServices (Workers)
// =============================================================================

var builder = WebApplication.CreateBuilder(args);

// ─────────────────────────────────────────────────────────────────────────────
// 1. 資料庫 (SQLite + Entity Framework Core)
// ─────────────────────────────────────────────────────────────────────────────
// 使用 SQLite 輕量級資料庫，適合開發與中小規模部署
// 生產環境可換成：options.UseNpgsql(connStr) 或 options.UseSqlServer(connStr)
builder.Services.AddDbContext<ProduceDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")));

// ─────────────────────────────────────────────────────────────────────────────
// 2. Redis 分散式快取
// ─────────────────────────────────────────────────────────────────────────────
// 快取農委會 API 回應 (TTL 24 小時)，避免每次請求都打外部 API，大幅提升效能
// InstanceName 為 Redis Key 前綴，確保多個應用程式共用 Redis 時不會 Key 衝突
builder.Services.AddStackExchangeRedisCache(options =>
{
    options.Configuration = builder.Configuration.GetConnectionString("Redis");
    options.InstanceName = "ProduceApi_";
});

// ─────────────────────────────────────────────────────────────────────────────
// 3. HttpClient (for ProduceService → MOA API)
// ─────────────────────────────────────────────────────────────────────────────
// AddHttpClient<T> 讓框架管理 HttpClient 生命週期，避免 Socket 耗盡問題
// (直接 new HttpClient() 是常見的反模式，會在高並發時導致 Socket 耗盡)
builder.Services.AddHttpClient<ProduceService>();

// ─────────────────────────────────────────────────────────────────────────────
// 4. 背景服務 (Background Workers)
// ─────────────────────────────────────────────────────────────────────────────
// 每 24 小時從農委會 API 同步最新價格資料到本地 SQLite 資料庫
builder.Services.AddHostedService<ProduceSyncWorker>();
// 每 24 小時掃描所有使用者收藏，達到目標價時透過 FCM 發送手機推播
builder.Services.AddHostedService<ProduceApi.PriceAlertWorker>();

// ─────────────────────────────────────────────────────────────────────────────
// 5. JWT 認證 (本次新增)
// ─────────────────────────────────────────────────────────────────────────────
// 為何加入 JWT：
//   原本 API 以 X-User-Id Header 傳遞使用者識別碼，任何人都可以偽造此 Header。
//   JWT 使用 HMAC-SHA256 簽章，後端可驗證 Token 是否由自己發行，防止身份偽造。
//
// JWT 驗證流程：
//   1. Client: POST /auth/token { deviceId } → 取得 JWT
//   2. Client: 每次 API 請求帶上 Authorization: Bearer {token}
//   3. Middleware: 自動驗證簽章、有效期、Issuer/Audience
//   4. Controller: User.FindFirst(ClaimTypes.NameIdentifier)?.Value 取得 UserId
var jwtSection = builder.Configuration.GetSection("Jwt");
var secretKey = jwtSection["SecretKey"];
if (string.IsNullOrWhiteSpace(secretKey))
    throw new InvalidOperationException(
        "JWT SecretKey is not configured. " +
        "Set the 'Jwt__SecretKey' environment variable in production, " +
        "or add it to appsettings.Development.json for local development.");

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,                   // 驗證 Token 是由我們的後端發行
        ValidIssuer = jwtSection["Issuer"],
        ValidateAudience = true,                 // 驗證 Token 是給我們的 App 使用
        ValidAudience = jwtSection["Audience"],
        ValidateLifetime = true,                 // 驗證 Token 是否已過期
        ValidateIssuerSigningKey = true,         // 驗證 Token 簽章，防止篡改
        IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey)),
        ClockSkew = TimeSpan.FromMinutes(5)      // 容忍客戶端與伺服器時鐘偏差 5 分鐘
    };
});

builder.Services.AddAuthorization();

// ─────────────────────────────────────────────────────────────────────────────
// 6. Controllers
// ─────────────────────────────────────────────────────────────────────────────
builder.Services.AddControllers();

// ─────────────────────────────────────────────────────────────────────────────
// 7. CORS (跨域資源共享)
// ─────────────────────────────────────────────────────────────────────────────
// 允許 Web 前端 (React 開發伺服器) 跨域存取 API
// ⚠️ 生產環境應改為：policy.WithOrigins("https://your-production-domain.com")
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader();
    });
});

// ─────────────────────────────────────────────────────────────────────────────
// 建立應用程式實例
// ─────────────────────────────────────────────────────────────────────────────
var app = builder.Build();

// 資料庫初始化：建立資料表並植入種子資料 (如果尚未初始化)
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();
    DbInitializer.Initialize(db);
}

// ─────────────────────────────────────────────────────────────────────────────
// HTTP Middleware Pipeline (順序非常重要，不可隨意調換)
// ─────────────────────────────────────────────────────────────────────────────
app.UseCors("AllowAll");         // CORS 必須在 Authentication 之前，讓 OPTIONS preflight 通過
app.UseAuthentication();         // 解析 JWT Token，設定 HttpContext.User
app.UseAuthorization();          // 根據 [Authorize] Attribute 決定是否允許存取
app.MapControllers();            // 掃描所有 Controller，依 Route Attribute 建立路由

app.Run();
