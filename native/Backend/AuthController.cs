using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using System;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Configuration;

// =============================================================================
// AuthController.cs — JWT 身份驗證控制器
//
// 功能說明：
//   提供行動端 App 取得 JWT (JSON Web Token) 的入口點。
//   行動端以裝置唯一識別碼 (DeviceId) 換取 JWT，之後所有 API 請求
//   都以 "Authorization: Bearer {token}" Header 帶上 Token。
//
// JWT 工作流程：
//   1. App 啟動 → 取得 DeviceId (iOS: identifierForVendor / Android: UUID)
//   2. POST /auth/token { deviceId: "..." } → 後端回傳 JWT
//   3. 之後所有 API 請求：Authorization: Bearer eyJhbGc...
//   4. 後端 JWT Middleware 自動驗證 Token，從 Claims 中取出 UserId
//
// 安全性說明：
//   - 此處使用「Device ID 換 Token」的匿名認證模式，不需要帳號密碼
//   - 適合「免登入」型應用，以裝置為單位識別使用者
//   - Token 有效期 720 小時 (30 天)，App 應在 Token 過期前自動刷新
//   - ⚠️ 生產環境必須將 appsettings.json 中的 SecretKey 換成真正的隨機強密碼
//     或使用環境變數注入，絕對不可以 commit 真實密鑰到版本控制
//
// 整合方式：
//   iOS:  URLSession 在每個 URLRequest 加上 "Authorization: Bearer {token}" Header
//   Android: Retrofit OkHttp Interceptor 自動在所有 Request 加上 Header
// =============================================================================

namespace ProduceApi.Controllers
{
    [ApiController]
    [Route("auth")]
    public class AuthController : ControllerBase
    {
        private readonly IConfiguration _configuration;

        /// <summary>
        /// 建構子：注入應用程式設定 (appsettings.json)
        /// </summary>
        public AuthController(IConfiguration configuration)
        {
            _configuration = configuration;
        }

        /// <summary>
        /// 以裝置唯一識別碼換取 JWT Token
        /// POST /auth/token
        /// Body: { "deviceId": "550e8400-e29b-41d4-a716-446655440000" }
        /// Response: { "token": "eyJhbGc...", "expiresAt": "2024-07-15T10:00:00Z" }
        /// </summary>
        [HttpPost("token")]
        public IActionResult GetToken([FromBody] TokenRequest request)
        {
            // 驗證 DeviceId 不為空
            if (string.IsNullOrWhiteSpace(request.DeviceId))
            {
                return BadRequest(new { Message = "DeviceId is required." });
            }

            // 從 appsettings.json 讀取 JWT 設定
            var jwtSection = _configuration.GetSection("Jwt");
            var secretKey = jwtSection["SecretKey"]
                ?? throw new InvalidOperationException("JWT SecretKey is not configured.");
            var issuer = jwtSection["Issuer"] ?? "VegettableAppApi";
            var audience = jwtSection["Audience"] ?? "VegettableAppMobile";
            var expirationHours = int.Parse(jwtSection["ExpirationHours"] ?? "720");

            // 建立 JWT Signing Key (HMAC-SHA256 對稱加密)
            var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));
            var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

            // Claims：Token 中內嵌的使用者資訊
            //   NameIdentifier：用於後端取得 UserId (替代原本的 X-User-Id Header)
            //   JwtRegisteredClaimNames.Jti：每個 Token 的唯一識別碼，防止 Token 重放攻擊
            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, request.DeviceId),
                new Claim(JwtRegisteredClaimNames.Sub, request.DeviceId),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
                new Claim(JwtRegisteredClaimNames.Iat,
                    DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString(),
                    ClaimValueTypes.Integer64)
            };

            var expiration = DateTime.UtcNow.AddHours(expirationHours);

            // 組裝 JWT Token
            var token = new JwtSecurityToken(
                issuer: issuer,
                audience: audience,
                claims: claims,
                expires: expiration,
                signingCredentials: credentials
            );

            var tokenString = new JwtSecurityTokenHandler().WriteToken(token);

            return Ok(new
            {
                Token = tokenString,
                ExpiresAt = expiration,
                DeviceId = request.DeviceId
            });
        }
    }

    /// <summary>
    /// Token 換取請求的 Request Body Model
    /// </summary>
    public class TokenRequest
    {
        /// <summary>
        /// 裝置唯一識別碼
        /// iOS: UIDevice.current.identifierForVendor.uuidString
        /// Android: Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        /// </summary>
        public string DeviceId { get; set; } = string.Empty;
    }
}
