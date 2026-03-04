# 台灣農產品價格追蹤系統 - 原生架構與 API 說明文件

本文件詳細說明本專案從 Web (React) 轉移至 **純原生開發 (Native App)** 與 **C# 後端 (Backend)** 的架構設計、邏輯修正，以及每個檔案的具體職責。

---

## 🏗️ 系統架構總覽 (Architecture Overview)

本系統採用 **BFF (Backend for Frontend)** 與 **Offline-First (離線優先)** 架構：

1. **C# 後端 (BFF)**：作為手機端與政府開放資料 (MOA API) 之間的中繼站。由後端負責每天定時抓取政府資料、清理格式、存入資料庫，並提供分頁與搜尋 API 給手機端。這解決了政府 API 回傳資料過大導致手機 OOM (Out of Memory) 的問題。
2. **手機端 (Android / iOS)**：採用 **Repository Pattern (單一資料來源模式)**。UI 層只與 Repository 溝通，Repository 會自動判斷要從網路 (API) 抓取資料，還是從本地資料庫 (SQLite/CoreData) 讀取離線快取。
3. **背景推播 (Background Jobs)**：雙平台皆實作了背景排程，能在背景比對價格並發送降價推播通知。

---

## 📂 檔案詳細說明 (File Breakdown)

### 🟣 Backend (C# ASP.NET Core)
後端負責資料整合、快取與提供 RESTful API。

*   **`Program.cs`**
    *   **職責**：應用程式的進入點 (Entry Point)。負責依賴注入 (DI)，註冊資料庫連線 (Entity Framework)、HttpClient、背景同步服務 (`ProduceSyncWorker`) 以及設定 CORS。
*   **`ProduceController.cs`**
    *   **職責**：RESTful API 控制器。提供手機端呼叫的端點，包含：
        *   `GET /daily-prices`：取得今日價格（支援 `keyword` 搜尋與 `page` 分頁）。
        *   `GET /history/{produceId}`：取得歷史價格趨勢（供手機端畫圖表）。
        *   `POST /favorites`：同步使用者的收藏清單與目標追蹤價格。**（已修正邏輯：透過 `X-User-Id` Header 識別使用者，而非依賴 Request Body，提升安全性）**。
*   **`ProduceDbContext.cs`**
    *   **職責**：Entity Framework Core 的資料庫上下文。定義了 `UserFavorites` (使用者收藏) 與 `PriceHistories` (歷史價格) 兩個資料表結構。
*   **`ProduceSyncWorker.cs`**
    *   **職責**：背景託管服務 (Hosted Service)。每天定時在背景執行，向台灣農業部 API 抓取最新資料並寫入 `PriceHistories` 資料庫，確保後端擁有完整的歷史數據。
*   **`ProduceDto.cs`**
    *   **職責**：資料傳輸物件 (Data Transfer Object)。確保前後端傳遞的 JSON 欄位名稱與型別完全一致。

---

### 🟢 Android (Java)
Android 端採用標準的 MVC/MVVM 網路架構，結合 OkHttp 與 SQLite。

*   **`ProduceRepository.java`**
    *   **職責**：單一資料來源 (Single Source of Truth)。負責協調 `ProduceService` (網路) 與 `ProduceDatabaseHelper` (本地資料庫)。先嘗試打 API，失敗時自動退回讀取 SQLite 的離線快取。
*   **`ProduceService.java`**
    *   **職責**：網路請求層。使用 `OkHttp` 發送 API 請求，並使用 `Gson` 將後端回傳的 JSON 自動反序列化為 `ProduceDto` Java 物件。
*   **`AuthInterceptor.java` (✨ 新增優化)**
    *   **職責**：OkHttp 攔截器。自動在所有發送給後端的 API 請求中加入 `X-User-Id` Header（例如設備 ID），讓後端能識別是哪個使用者在操作，解決了前後端身分驗證的邏輯斷層。
*   **`ProduceDatabaseHelper.java`**
    *   **職責**：SQLite 資料庫管理員。負責建立本地資料表，用於儲存離線快取與本地收藏清單。
*   **`PriceAlertWorker.java`**
    *   **職責**：背景工作者 (WorkManager)。在背景定期檢查價格，若達到使用者的目標價，則觸發系統推播通知 (NotificationCompat)。
*   **`ProduceDto.java`**
    *   **職責**：對應後端的資料模型，確保 Gson 解析時欄位完全吻合。

---

### 🍎 iOS (Swift)
iOS 端採用原生 URLSession 與 CoreData 架構。

*   **`ProduceRepository.swift`**
    *   **職責**：單一資料來源。邏輯與 Android 相同，負責協調網路 API 與 CoreData，實作離線優先 (Offline-First) 邏輯。
*   **`ProduceService.swift`**
    *   **職責**：網路請求層。使用原生 `URLSession` 發送請求，並透過 `JSONDecoder` 與 `Codable` 協定，將 JSON 安全地轉換為 Swift Struct。**（已修正邏輯：自動在 Request 中注入 `X-User-Id` Header）**。
*   **`ProduceCoreDataStore.swift`**
    *   **職責**：CoreData 管理員。負責將 API 取得的資料持久化到 iPhone 本地儲存空間，供無網路時使用。
*   **`PriceAlertNotifier.swift`**
    *   **職責**：本地推播管理員。使用 `UNUserNotificationCenter`，在背景偵測到降價時發送 iOS 推播通知。
*   **`ProduceDto.swift`**
    *   **職責**：實作 `Codable` 的資料模型，確保與後端 JSON 欄位完美對應。

---

## 🔄 前後端邏輯 Match 重點總結

1. **分頁與搜尋 (Pagination & Search)**：前端不再一次接收 2000 筆資料，而是透過 `page` 與 `keyword` 參數向後端請求，後端透過 LINQ 過濾後只回傳 20 筆，大幅降低手機記憶體消耗。
2. **身分識別 (Authentication)**：移除了原本在 Body 中手動傳遞 `UserId` 的錯誤邏輯。現在透過 Android 的 `Interceptor` 與 iOS 的 `URLRequest` 自動在 Header 注入 `X-User-Id`，後端 Controller 統一從 Header 讀取，邏輯完全 Match 且更安全。
3. **離線容錯 (Offline Tolerance)**：透過 Repository Pattern，前端在呼叫後端 API 失敗時，不會再發生 Crash，而是優雅地降級 (Fallback) 讀取本地資料庫。
4. **JSON 解析邏輯修正 (Backend)**：政府 API 回傳的 JSON 欄位是中文 (如 `"作物代號"`)，如果後端直接用 `ProduceDto` 解析，會導致前端收到的也是中文欄位。新增了專門用來解析政府 API 的 `MoaProduceDto` (加上 `[JsonPropertyName]` 標籤)，並在 Controller 中將其映射回標準的 `ProduceDto`，確保前端收到的永遠是標準的英文欄位 (如 `"cropCode"`)。
5. **背景同步服務邏輯修正 (`ProduceSyncWorker.cs`)**：實作了完整的 JSON 解析邏輯 (使用 `MoaProduceDto`)，並將每天抓取到的最新價格寫入 `PriceHistory` 資料表，讓後端真正成為資料的 Source of Truth。

---

## ✨ 新增功能 (New Features)

1. **市場比價 (Market Comparison)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/compare/{cropName}`，允許使用者查詢特定農產品在全台各市場的今日價格，並由低到高排序。
   - **Android/iOS (`ProduceService.java`, `ProduceService.swift`)**：新增 `comparePrices` 方法，讓前端可以輕鬆呼叫比價 API。

2. **價格預測與趨勢分析 (Price Forecasting)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/forecast/{produceId}`，根據過去 14 天的歷史資料，計算 7 日移動平均線，預測未來價格趨勢 (上漲、下跌或持平)。
   - **Android/iOS (`ProduceService.java`, `ProduceService.swift`)**：新增 `getForecast` 方法，讓前端可以取得價格預測結果。

3. **熱門交易農產品 (Top Volume Crops)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/top-volume`，取得今日交易量最大的前 10 名農產品，幫助使用者了解目前市場上最熱銷、當季的農產品。
   - **Android/iOS (`ProduceService.java`, `ProduceService.swift`)**：新增 `getTopVolumeCrops` 方法，讓前端可以輕鬆呼叫熱門農產品 API。

4. **我的收藏與價格提醒 (My Favorites & Price Alerts)**：
   - **Backend (`ProduceController.cs`)**：
     - 新增 `GET /api/produce/favorites`：取得使用者的收藏清單，並自動比對今日最新價格，判斷是否達到使用者設定的目標價格 (`IsAlertTriggered`)。
     - 新增 `DELETE /api/produce/favorites/{produceId}`：允許使用者移除收藏。
   - **Android/iOS (`ProduceDto.java`, `ProduceDto.swift`)**：新增 `FavoriteAlertDto`，確保前後端資料結構完全一致。
   - **Android/iOS (`ProduceService.java`, `ProduceService.swift`)**：新增 `getFavorites` 與 `removeFavorite` 方法，讓前端可以輕鬆管理收藏並顯示價格提醒。

5. **主動式推播通知 (Push Notifications)**：
   - **Backend (`PriceAlertWorker.cs`)**：新增 `BackgroundService`，每天清晨自動掃描所有使用者的 `TargetPrice`，一旦達標，直接發送手機推播（模擬 FCM/APNs）。

6. **離線快取機制 (Offline Caching)**：
   - **Android (`ProduceEntity.java`, `ProduceDao.java`, `ProduceDatabase.java`)**：導入 `Room Database`，在有網路時自動快取「我的收藏」與「今日熱門農產品」的最新價格。
   - **iOS (`ProduceModel.swift`)**：導入 `SwiftData` 進行本地快取。

7. **社群回報機制 (Community Retail Price)**：
   - **Backend (`ProduceController.cs`)**：新增 `POST /api/produce/community-price` 與 `GET /api/produce/community-price/{cropCode}` 讓使用者回報與查詢零售價。
   - **Android/iOS (`CommunityPriceDto`, `ProduceService`)**：新增對應的 DTO 與 API 呼叫方法。

8. **當季盛產日曆 (Seasonal Crop Calendar)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/seasonal`，根據目前的月份，自動篩選出盛產的農產品清單。
   - **Android/iOS (`SeasonalCropDto`, `ProduceService`)**：新增對應的 DTO 與 API 呼叫方法。

9. **農產品價格異常警告 (Price Anomaly Detection)**：
   - **Backend (`ProduceController.cs`)**：新增 `GET /api/produce/anomalies`，自動比對近兩日的歷史價格，若單日漲幅超過 50%（例如因颱風或豪雨），則自動產生異常警報。
   - **Android/iOS (`PriceAnomalyDto`, `ProduceService`)**：新增對應的 DTO 與 API 呼叫方法，讓前端能在首頁顯示「價格異常警報」，提醒使用者避開購買。

10. **多語系與無障礙設計 (i18n & Accessibility)**：
    - **Android/iOS (`strings.xml`, `Localizable.strings`)**：針對外籍移工或長輩，加入多國語言支援（如印尼語 `id`、越南語 `vi`）。
    - **Android (`TextToSpeechHelper.java`)**：導入 `android.speech.tts.TextToSpeech`，提供語音播報功能，並可調慢語速，方便長輩或視力不佳者聽取價格。
    - **iOS (`TextToSpeechHelper.swift`)**：導入 `AVFoundation` 的 `AVSpeechSynthesizer`，實現跨語系的語音播報輔助。

11. **前端 UI 介面實作 (UI Layer)**：
    - **UI 風格 (iOS 26 Liquid Glass / 毛玻璃)**：參考未來感十足的 iOS 26 質感，全站採用淡綠色漸層底色，並搭配半透明、帶有模糊效果與白色細邊框的卡片設計。底部導覽列採用「懸浮式毛玻璃 (Floating Liquid Glass)」設計，營造清新且極具現代感的視覺體驗。
    - **Android (Jetpack Compose)**：
      - `MainScreen.kt`：實作懸浮式底部導覽列 (Bottom Navigation)，串聯首頁、收藏與設定頁面。
      - `ProduceViewModel.kt`：使用 `StateFlow` 綁定資料，並提供歷史價格、預測數據與收藏清單。
      - `HomeScreen.kt`：實作 `liquidGlass()` Modifier 達成毛玻璃效果。包含異常警報、熱門交易、以及帶有語音播報的菜價列表。
      - `FavoritesScreen.kt`：實作「我的收藏」頁面，延續 Liquid Glass 風格，並根據目標價是否達成顯示不同的鈴鐺狀態 (已達標/追蹤中)。
      - `SettingsScreen.kt`：實作「設定」頁面，包含推播、語言、離線快取等選項。
      - `PriceTrendChart.kt`：使用 `Canvas` 繪製折線圖，實線代表歷史價格，虛線 (橘色) 代表 AI 預測趨勢。
    - **iOS (SwiftUI)**：
      - `MainTabView.swift`：實作懸浮式底部導覽列，完美融合 `.ultraThinMaterial` 打造 iOS 26 質感。
      - `ProduceViewModel.swift`：使用 `@MainActor` 與 `@Published` 進行狀態管理，包含收藏清單資料。
      - `HomeScreen.swift`：使用 `.ultraThinMaterial` 搭配淡綠色背景實作毛玻璃質感 (`LiquidGlassModifier`)。
      - `FavoritesScreen.swift`：實作「我的收藏」頁面，延續毛玻璃質感，並使用 SF Symbols 呈現目標價追蹤狀態。
      - `SettingsScreen.swift`：實作「設定」頁面，提供各項 App 設定選項。
      - `PriceTrendView.swift`：導入 Apple 原生 `Charts` 框架，使用 `LineMark` 繪製歷史與預測價格走勢圖。
