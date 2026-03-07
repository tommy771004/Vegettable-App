# 台灣農產品價格追蹤系統 - 原生架構與 API 說明文件

本文件詳細說明本專案從 Web (React) 轉移至 **純原生開發 (Native App)** 與 **C# 後端 (Backend)** 的架構設計、邏輯修正，以及每個檔案的具體職責。

---

## 🏗️ 系統架構總覽 (Architecture Overview)

本系統採用 **BFF (Backend for Frontend)** 與 **Offline-First (離線優先)** 架構：

1. **C# 後端 (BFF)**：作為手機端與政府開放資料 (MOA API) 之間的中繼站。由後端負責每天定時抓取政府資料、清理格式、存入資料庫，並提供分頁與搜尋 API 給手機端。這解決了政府 API 回傳資料過大導致手機 OOM (Out of Memory) 的問題。
2. **手機端 (Android / iOS)**：採用 **Repository Pattern (單一資料來源模式)**。UI 層只與 Repository 溝通，Repository 會自動判斷要從網路 (API) 抓取資料，還是從本地資料庫 (SQLite/CoreData) 讀取離線快取。
3. **背景推播 (Background Jobs)**：雙平台皆實作了背景排程，能在背景比對價格並發送降價推播通知。

---

## 🔐 身分驗證架構 (Authentication Architecture)

本系統採用 **JWT (JSON Web Token)** 進行前後端身份驗證，已從舊版 `X-User-Id` Header 全面升級：

### JWT 認證流程
```
手機端 (冷啟動)
  → POST /auth/token  { "deviceId": "<UUID>" }
  ← { "token": "<JWT Bearer>" }
  → 儲存至 UserDefaults (iOS) / SharedPreferences (Android)
  → 所有後續 API 請求自動附加  Authorization: Bearer <token>
```

### Android JWT 自動注入
*   **`JwtInterceptor.kt`**：OkHttp 攔截器，從 `SharedPreferences` 讀取 JWT token，自動注入所有 API 請求的 `Authorization: Bearer` Header。
*   **`RetrofitClient.kt`**：使用 `BuildConfig.API_BASE_URL` 取得 API 基底 URL（由 `build.gradle` 的 `buildConfigField` 在編譯期注入，而非硬寫在程式碼中）。

### iOS JWT 自動注入
*   **`ProduceService.swift`**：所有 API 請求從 `UserDefaults` 讀取 JWT token，自動注入 `Authorization: Bearer` Header。
*   **`Configuration.swift`**：API 基底 URL 從 `Config.plist` 讀取（Debug 模式若未設定則觸發 `assertionFailure`）。

### 後端 JWT 設定安全性
*   **`appsettings.json`**：`SecretKey` 欄位必須保持空字串，不得包含真實金鑰。
*   **`appsettings.Development.json`**（本地開發專用）：含開發用金鑰，**不得** commit 至正式環境分支。
*   **正式環境**：透過 OS 環境變數 `Jwt__SecretKey` 注入（ASP.NET Core 雙底線 = 巢狀設定）。
*   **`Program.cs`**：啟動時驗證 `SecretKey` 是否已設定，若為空則拋出 `InvalidOperationException` 阻止服務啟動。

---

## 📂 檔案詳細說明 (File Breakdown)

### 🟣 Backend (C# ASP.NET Core 8)
後端負責資料整合、快取與提供 RESTful API。

*   **`Program.cs`**
    *   **職責**：應用程式的進入點 (Entry Point)。負責依賴注入 (DI)，註冊資料庫連線 (Entity Framework)、HttpClient、背景同步服務 (`ProduceSyncWorker`) 以及設定 CORS。
    *   **安全強化**：啟動時驗證 `Jwt__SecretKey` 環境變數，若未設定則明確提示錯誤訊息並阻止服務啟動。
*   **`ProduceController.cs`**
    *   **職責**：RESTful API 控制器。提供手機端呼叫的端點，包含：
        *   `GET /daily-prices`：取得今日價格（支援 `keyword` 搜尋與 `page` 分頁）。
        *   `GET /history/{produceId}`：取得歷史價格趨勢（供手機端畫圖表）。
        *   `GET /top-volume`：今日交易量前 10 名農產品。
        *   `GET /anomalies`：單日漲幅超過 50% 的價格異常警告。
        *   `GET /forecast/{produceId}`：根據 7 日移動平均計算明日價格趨勢。
        *   `GET /seasonal`：當季盛產農作物清單（依月份篩選）。
        *   `GET /weather-alerts`：中央氣象署颱風/豪大雨警報（RSS 解析），無警報時回傳 `alertType: "None"`。
        *   `GET /budget-recipes`：依當季/低價食材推薦的省錢食譜清單。
        *   `GET /favorites`、`DELETE /favorites/{produceId}`：使用者收藏管理。
        *   `POST /community-price`、`GET /community-price/{cropCode}`：社群零售價回報。
        *   `POST /auth/token`：以 `deviceId` 換取 JWT Bearer Token。
    *   **已改善**：注入 `ILogger<ProduceController>`，天氣警報 API 失敗時使用 `LogWarning` 記錄而非 Crash，不影響主畫面資料。
    *   **已移除**：`X-User-Id` Header 識別方式，已全面改為 JWT Bearer 認證。
*   **`ProduceDbContext.cs`**
    *   **職責**：Entity Framework Core 的資料庫上下文。定義了 `UserFavorites`、`PriceHistories`、`CommunityPrices` 資料表結構。
*   **`ProduceSyncWorker.cs`**
    *   **職責**：背景託管服務 (Hosted Service)。每天定時在背景執行，向台灣農業部 API 抓取最新資料並寫入 `PriceHistories` 資料庫，確保後端擁有完整的歷史數據。
*   **`ProduceDto.cs`**
    *   **職責**：資料傳輸物件 (Data Transfer Object)。確保前後端傳遞的 JSON 欄位名稱與型別完全一致。包含 `WeatherAlertDto`（天氣警報）與 `BudgetRecipeDto`（省錢食譜）。

---

### 🟢 Android (Kotlin / Jetpack Compose / Hilt)
Android 端採用標準的 **MVVM** 架構，結合 Jetpack Compose UI、Hilt 依賴注入、Retrofit 網路層與 Room 本地快取。

*   **`ProduceRepository.kt`**
    *   **職責**：單一資料來源 (Single Source of Truth)。負責協調 `ProduceService` (網路) 與 `ProduceDao` (Room 本地資料庫)。先嘗試打 API，失敗時自動退回讀取 SQLite 的離線快取。
*   **`ProduceService.kt`** (Retrofit Interface)
    *   **職責**：網路請求層。以 Retrofit 介面定義所有 API 端點，Retrofit 自動序列化/反序列化 JSON。
    *   **新增端點**：`getWeatherAlerts(): WeatherAlertDto`、`getBudgetRecipes(): List<BudgetRecipeDto>`。
*   **`RetrofitClient.kt`**
    *   **職責**：Retrofit 與 OkHttp 初始化。
    *   **API URL**：使用 `BuildConfig.API_BASE_URL`（由 `build.gradle` `buildConfigField` 編譯期注入），避免 URL 硬寫在程式碼中。
    *   **JWT 注入**：透過 `JwtInterceptor` 攔截所有請求，自動附加 `Authorization: Bearer` Header。
*   **`ProduceDto.kt`**
    *   **職責**：對應後端的 Kotlin data class。
    *   **變更**：`HistoricalPriceDto.avgPrice` 與 `PricePredictionDto.predictedPrice` 型別從 `Float` 改為 `Double`（與後端 double 精度對齊）。
    *   **新增**：`WeatherAlertDto`（天氣警報）、`BudgetRecipeDto`（省錢食譜）。
*   **`ProduceViewModel.kt`**
    *   **職責**：MVVM ViewModel 層，使用 `@HiltViewModel` + `StateFlow` 管理 UI 狀態。
    *   **新增 StateFlow**：`weatherAlerts`、`budgetRecipes`（獨立 try-catch，不影響主資料載入）。
*   **`ProduceFirebaseMessagingService.kt`**
    *   **職責**：Firebase Cloud Messaging 推播處理。
    *   **已修復**：移除錯誤的 `com.example.produce.data.ProduceService()` 實例化（ClassNotFoundException），改用 OkHttp 直接發送請求。
    *   **JWT 整合**：FCM Token 向後端註冊時使用 JWT Bearer（從 `SharedPreferences` 讀取）。
    *   **Deep Link**：通知點擊後透過 `PendingIntent` 傳遞 `produceId` Extra，啟動 `MainActivity` 並跳轉到對應農產品詳情頁。
*   **`HomeScreen.kt`**
    *   **職責**：首頁 UI，10 個資訊區塊依緊急程度排序：① 價格異常警報 → ② 今日熱門交易 → ③ 語音搜尋 → ④ 今日菜價 → ⑤ 天氣預警 → ⑥ 省錢食譜 → ⑦ 趨勢圖表 → ⑧ 當季盛產 → ⑨ 最近市場 → ⑩ 購物清單入口。
    *   **毛玻璃效果**：`liquidGlass()` Modifier 以半透明淡綠色 + 白色細邊框實現。
*   **`WeatherAlertCard.kt`**：訂閱 `viewModel.weatherAlerts`，當 `alertType == "None"` 時自動隱藏（不顯示卡片）。
*   **`BudgetRecipeCard.kt`**：訂閱 `viewModel.budgetRecipes`，顯示真實 API 返回的省錢食譜（含 Loading/Error/Empty 三態）。
*   **`PriceTrendChart.kt`**：Canvas 折線圖，加入無障礙語意 (`semantics { contentDescription }`)。
*   **`VoiceSearchButton.kt`**：按鈕文字已修正為「點擊說話」（與實際點擊行為一致）。

---

### 🍎 iOS (SwiftUI / Combine / async-await)
iOS 端採用 SwiftUI + `@MainActor` MVVM，以 URLSession + async/await 處理網路請求。

*   **`ProduceRepository.swift`**
    *   **職責**：單一資料來源。邏輯與 Android 相同，負責協調網路 API 與 SwiftData，實作離線優先 (Offline-First) 邏輯。
*   **`ProduceService.swift`**
    *   **職責**：網路請求層。使用原生 `URLSession` 發送請求，JWT Bearer Token 從 `UserDefaults` 讀取後自動注入每個請求的 `Authorization` Header。
*   **`Configuration.swift`**
    *   **職責**：API 基底 URL 從 `Config.plist` 讀取。
    *   **安全強化**：移除硬寫 fallback URL，Debug 模式若 `Config.plist` 未設定則觸發 `assertionFailure`（開發期間立即發現設定錯誤）。
*   **`AppDelegate.swift`**
    *   **職責**：Firebase Cloud Messaging 初始化與 FCM Token 管理。
    *   **已修正**：FCM Token 向後端註冊改用 JWT Bearer（移除舊 `X-User-Id` Header）。
    *   **冷啟動處理**：若 JWT 尚未取得，以 `Task { await ProduceService.shared.ensureJwtToken() }` 異步等候後再注冊。
*   **`MainTabView.swift`**
    *   **職責**：底部導覽列，包含四個 Tab：首頁、收藏、探索、設定。
    *   **新增「探索」Tab**：`ExploreMenuView` 以 `NavigationLink` 串聯所有進階功能頁面，解決孤立畫面無導覽入口問題：
        *   長輩友善模式 (`ElderlyModeView`)
        *   當季蔬果日曆 (`SeasonalCalendarView`)
        *   社群物價回報 (`CommunityReportView`)
        *   目標提醒設定 (`PriceAlertSetupView`)
*   **`PriceChartView.swift`**
    *   **職責**：農產品歷史價格趨勢圖（SwiftUI Charts）。
    *   **已重寫**：接受 `produceId` + `produceName` 參數，透過 `ProduceService.shared.fetchPriceHistory(produceId:)` 取得真實 API 資料，支援 Loading/Error/Empty/Chart 四態 UI。
*   **`Components.swift` (SkeletonView)**
    *   **已升級**：從簡單閃爍動畫改為 `LinearGradient` 光澤掃過效果（shimmer sweep），以 `withAnimation(.linear.repeatForever(autoreverses: false))` 驅動。
*   **`FavoritesScreen.swift`**
    *   **已修正**：`item.isReached` → `item.isAlertTriggered`（對應後端 DTO 欄位名稱）。

---

## 🔄 前後端邏輯 Match 重點總結

1. **分頁與搜尋 (Pagination & Search)**：前端不再一次接收 2000 筆資料，而是透過 `page` 與 `keyword` 參數向後端請求，後端透過 LINQ 過濾後只回傳 20 筆，大幅降低手機記憶體消耗。
2. **身分識別 (Authentication)**：已從 `X-User-Id` Header 全面升級為 **JWT Bearer Token**。Android 透過 `JwtInterceptor`，iOS 透過 `ProduceService` 自動注入。後端以 `Jwt__SecretKey` 環境變數管理金鑰，嚴禁硬寫於程式碼中。
3. **離線容錯 (Offline Tolerance)**：透過 Repository Pattern，前端在呼叫後端 API 失敗時，不會再發生 Crash，而是優雅地降級 (Fallback) 讀取本地資料庫。
4. **JSON 解析邏輯修正 (Backend)**：政府 API 回傳的 JSON 欄位是中文 (如 `"作物代號"`)，如果後端直接用 `ProduceDto` 解析，會導致前端收到的也是中文欄位。新增了專門用來解析政府 API 的 `MoaProduceDto` (加上 `[JsonPropertyName]` 標籤)，並在 Controller 中將其映射回標準的 `ProduceDto`，確保前端收到的永遠是標準的英文欄位 (如 `"cropCode"`)。
5. **背景同步服務邏輯修正 (`ProduceSyncWorker.cs`)**：實作了完整的 JSON 解析邏輯 (使用 `MoaProduceDto`)，並將每天抓取到的最新價格寫入 `PriceHistory` 資料表，讓後端真正成為資料的 Source of Truth。
6. **天氣預警與食譜獨立容錯**：`weatherAlerts` 與 `budgetRecipes` API 各自有獨立的 try-catch，失敗時不影響主資料（菜價、異常、熱門交易等）的正常顯示。

---

## ✨ 新增功能 (New Features)

1. **市場比價 (Market Comparison)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/compare/{cropName}`，允許使用者查詢特定農產品在全台各市場的今日價格，並由低到高排序。

2. **價格預測與趨勢分析 (Price Forecasting)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/forecast/{produceId}`，根據過去 14 天的歷史資料，計算 7 日移動平均線，預測未來價格趨勢 (上漲、下跌或持平)。

3. **熱門交易農產品 (Top Volume Crops)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/top-volume`，取得今日交易量最大的前 10 名農產品。

4. **我的收藏與價格提醒 (My Favorites & Price Alerts)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/favorites` 自動比對今日最新價格，判斷是否達到目標價格 (`IsAlertTriggered`)。
   - **Android/iOS**：新增 `FavoriteAlertDto`，確保前後端資料結構完全一致。

5. **天氣警報整合 (Weather Alerts)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/weather-alerts` 解析中央氣象署 RSS 取得颱風/豪大雨警報，無警報時回傳 `alertType: "None"`，失敗時記錄 `LogWarning` 而非 Crash。
   - **Android (`WeatherAlertDto`, `WeatherAlertCard.kt`)**：新增 DTO 與對應的 Compose UI，`alertType == "None"` 時自動隱藏。
   - **iOS**：對應的天氣警報顯示邏輯。

6. **省錢食譜推薦 (Budget Recipe Generator)**：
   - **Backend (`ProduceController.cs`)**：`GET /api/produce/budget-recipes` 依當季/低價食材生成省錢食譜清單。
   - **Android (`BudgetRecipeDto`, `BudgetRecipeCard.kt`)**：新增 DTO 與對應的 Compose UI，支援 Loading/Error/Empty/Success 四態。

7. **主動式推播通知 (Push Notifications)**：
   - **Backend (`PriceAlertWorker.cs`)**：新增 `BackgroundService`，每天清晨自動掃描所有使用者的 `TargetPrice`，透過 Firebase Admin SDK 發送 FCM 推播通知。
   - **Android**：FCM 通知點擊後透過 `PendingIntent` Deep Link 傳遞 `produceId`，開啟農產品詳情頁。
   - **iOS**：FCM Token 以 JWT Bearer 向後端註冊。

8. **離線快取機制 (Offline Caching)**：
   - **Android (`ProduceEntity.kt`, `ProduceDao.kt`, `ProduceDatabase.kt`)**：導入 `Room Database`，在有網路時自動快取「我的收藏」與「今日熱門農產品」的最新價格。
   - **iOS (`ProduceModel.swift`)**：導入 `SwiftData` 進行本地快取。

9. **社群回報機制 (Community Retail Price)**：
   - **Backend (`ProduceController.cs`)**：新增 `POST /api/produce/community-price` 與 `GET /api/produce/community-price/{cropCode}` 讓使用者回報與查詢零售價。

10. **當季盛產日曆 (Seasonal Crop Calendar)**：
    - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/seasonal`，根據目前的月份，自動篩選出盛產的農產品清單。

11. **農產品價格異常警告 (Price Anomaly Detection)**：
    - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/anomalies`，自動比對近兩日的歷史價格，若單日漲幅超過 50%，則自動產生異常警報。

12. **多語系與無障礙設計 (i18n & Accessibility)**：
    - **Android/iOS**：針對外籍移工或長輩，加入多國語言支援（如印尼語 `id`、越南語 `vi`）。
    - **Android (`TextToSpeechHelper.kt`)**：導入 `android.speech.tts.TextToSpeech`，提供語音播報功能，並可調慢語速。
    - **iOS (`TextToSpeechHelper.swift`)**：導入 `AVFoundation` 的 `AVSpeechSynthesizer`，實現跨語系的語音播報輔助。
    - **Android (`PriceTrendChart.kt`)**：Canvas 圖表加入 `semantics { contentDescription }` 無障礙語意，方便螢幕閱讀器使用者。

13. **探索 Tab 導覽整合 (iOS)**：
    - **iOS (`MainTabView.swift`)**：新增第四個「探索」Tab，以 `ExploreMenuView` 列出所有進階功能頁面的 `NavigationLink`，解決 `ElderlyModeView`、`SeasonalCalendarView`、`CommunityReportView`、`PriceAlertSetupView` 等畫面無導覽入口的問題。

14. **前端 UI 介面實作 (UI Layer)**：
    - **UI 風格 (iOS 26 Liquid Glass / 毛玻璃)**：全站採用淡綠色漸層底色，搭配半透明、帶有模糊效果與白色細邊框的卡片設計。底部導覽列採用「懸浮式毛玻璃 (Floating Liquid Glass)」設計。
    - **Android (Jetpack Compose)**：
      - `HomeScreen.kt`：10 個資訊區塊依緊急程度排序的首頁 UI。
      - `FavoritesScreen.kt`：我的收藏頁面，根據目標價是否達成顯示不同鈴鐺狀態。
      - `SettingsScreen.kt`：設定頁面，包含推播、語言、離線快取等選項。
      - `PriceTrendChart.kt`：Canvas 折線圖，含無障礙語意標籤。
    - **iOS (SwiftUI)**：
      - `MainTabView.swift`：含「探索」Tab 的四分頁底部導覽列。
      - `HomeScreen.swift`：使用 `.ultraThinMaterial` 的毛玻璃質感首頁。
      - `FavoritesScreen.swift`：收藏頁面，已修正 `isAlertTriggered` 欄位對應。
      - `PriceChartView.swift`：接真實 API 的歷史趨勢圖，含四態 UI。
      - `Components.swift`：`SkeletonView` 升級為 shimmer sweep 光澤動畫。
