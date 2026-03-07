package com.example.produceapp.network

import android.content.Context
import android.content.SharedPreferences
import com.example.produceapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

// =============================================================================
// RetrofitClient.kt — Android 網路層設定 (Retrofit + OkHttp)
//
// 架構說明：
//   Retrofit 負責定義「API 介面」(ProduceService.kt)，
//   OkHttp 負責實際的 HTTP 連線、Header 注入、逾時等底層設定。
//
// JWT 認證整合 (本次更新)：
//   原本：每個 Retrofit Request 都要手動帶 X-User-Id Header (任何人可偽造)
//   現在：加入 OkHttp Interceptor 自動為所有 Request 注入 JWT Bearer Token
//
//   OkHttp Interceptor 的優勢：
//   - 一次設定，所有 API 自動帶 Token，無需每個 Retrofit Interface 個別處理
//   - 統一管理 Token 刷新邏輯 (收到 401 自動重新申請 Token)
//   - 不侵入業務邏輯，關注點分離
//
// JWT 生命週期：
//   1. App 首次啟動 → Interceptor 發現無 Token → 呼叫 /auth/token 取得 JWT
//   2. 每次請求：Interceptor 從 SharedPreferences 讀取 Token → 注入 Authorization Header
//   3. 收到 401 → 清除舊 Token → 重新取得新 Token → 重試原始請求
// =============================================================================

/**
 * Retrofit Client 單例：建立配置好 JWT 認證的 Retrofit 實例
 * 需要 Context 以存取 SharedPreferences 儲存 JWT Token
 */
object RetrofitClient {

    /** 後端 API Base URL（從 build.gradle BuildConfig 讀取，不硬編碼） */
    private val BASE_URL = BuildConfig.API_BASE_URL

    /** JWT 認證端點：從 BASE_URL 推導主機，替換路徑 */
    private val AUTH_URL = BASE_URL.substringBefore("/api/") + "/auth/token"

    /** SharedPreferences 鍵名常數 */
    private const val PREF_NAME    = "vegettable_prefs"
    private const val KEY_JWT      = "jwt_token"
    private const val KEY_DEVICE_ID = "device_uuid"

    /** 延遲初始化的 Retrofit 實例，首次呼叫時建立 */
    @Volatile
    private var instance: ProduceService? = null

    /**
     * 取得 ProduceService 實例（含 JWT Interceptor）
     * @param context Application Context，用於存取 SharedPreferences
     */
    fun getInstance(context: Context): ProduceService {
        return instance ?: synchronized(this) {
            instance ?: buildInstance(context).also { instance = it }
        }
    }

    /** 建立帶有 JWT OkHttp Interceptor 的 Retrofit 實例 */
    private fun buildInstance(context: Context): ProduceService {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val okHttpClient = OkHttpClient.Builder()
            // 加入 JWT 認證攔截器
            .addInterceptor(JwtInterceptor(prefs, AUTH_URL))
            // 連線逾時設定（秒）
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProduceService::class.java)
    }

    /**
     * 裝置唯一識別碼：首次呼叫時生成並永久儲存在 SharedPreferences
     * 用途：呼叫 /auth/token 時作為使用者身份識別
     */
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, uuid).apply()
            uuid
        }
    }
}

// =============================================================================
// JwtInterceptor — OkHttp 攔截器：自動管理 JWT Token 的取得與注入
//
// 運作原理：
//   OkHttp 在每個 HTTP 請求發出前，先執行所有已加入的 Interceptor。
//   JwtInterceptor 在此時：
//   1. 從 SharedPreferences 讀取已儲存的 JWT Token
//   2. 若無 Token → 先呼叫 /auth/token 取得 JWT (同步阻塞，因為 Interceptor 是同步的)
//   3. 將 JWT 加到原始請求的 Authorization Header
//   4. 執行原始請求，若收到 401 → 清除 Token → 重新取得 → 重試一次
// =============================================================================
private class JwtInterceptor(
    private val prefs: SharedPreferences,
    private val authUrl: String
) : Interceptor {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        // Step 1：確保 JWT Token 存在
        val token = getOrFetchToken()

        // Step 2：將 JWT 注入原始請求的 Authorization Header
        val authenticatedRequest = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        // Step 3：執行帶有 JWT 的請求
        val response = chain.proceed(authenticatedRequest)

        // Step 4：若收到 401 Unauthorized → Token 可能已過期
        if (response.code == 401) {
            response.close()

            // 清除過期 Token，重新申請
            prefs.edit().remove("jwt_token").apply()
            val newToken = fetchNewToken() ?: return response

            // 以新 Token 重試原始請求
            val retryRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
            return chain.proceed(retryRequest)
        }

        return response
    }

    /**
     * 取得現有 Token，若無則申請新的
     * 使用 synchronized 確保多執行緒不會重複申請 Token
     */
    private fun getOrFetchToken(): String {
        return prefs.getString("jwt_token", null) ?: synchronized(this) {
            // Double-Check Locking：進入鎖後再次確認是否有其他執行緒已取得 Token
            prefs.getString("jwt_token", null) ?: (fetchNewToken() ?: "")
        }
    }

    /**
     * 向後端 /auth/token 申請新的 JWT Token（同步 HTTP 呼叫）
     * Interceptor 本身是同步環境，不能使用 suspend/async
     */
    private fun fetchNewToken(): String? {
        return try {
            val deviceId = prefs.getString("device_uuid", null)
                ?: UUID.randomUUID().toString().also {
                    prefs.edit().putString("device_uuid", it).apply()
                }

            // 組裝 POST /auth/token 請求
            val body = JSONObject().apply { put("deviceId", deviceId) }
                .toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(authUrl)
                .post(body)
                .build()

            // 同步執行 HTTP 請求（在 OkHttp 背景執行緒中，不阻塞主執行緒）
            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return null
                val json = JSONObject(responseBody)
                val token = json.getString("token")

                // 存入 SharedPreferences 供後續請求使用
                prefs.edit().putString("jwt_token", token).apply()
                token
            } else {
                null
            }
        } catch (e: Exception) {
            // Token 申請失敗（例：網路中斷），記錄錯誤但不 crash App
            android.util.Log.e("JwtInterceptor", "Failed to fetch JWT token: ${e.message}")
            null
        }
    }
}
