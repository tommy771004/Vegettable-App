# 🚀 進階功能整合指南 (Open Data, FCM, Offline-First, Widgets)

我已經為您產生了所有進階功能的**核心程式碼**。由於這些功能涉及深度的原生系統設定（如憑證、套件管理、系統權限），請依照以下指南將這些程式碼整合進您的專案中。

---

## 1. 🇹🇼 串接真實政府開放資料 (Open Data) & 本地端快取 (Offline-First)

我們採用了 **Offline-First (離線優先)** 架構：App 啟動時會先秒速載入本地資料庫的舊資料，同時在背景向政府 API 請求新資料。新資料抓到後會寫入資料庫，並自動觸發 UI 更新。

### 🤖 Android (Retrofit + Room)
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/data/ProduceRepository.kt`
*   **您需要做的設定**：
    1. 在 `app/build.gradle` 中加入依賴：
       ```gradle
       implementation "com.squareup.retrofit2:retrofit:2.9.0"
       implementation "com.squareup.retrofit2:converter-gson:2.9.0"
       implementation "androidx.room:room-runtime:2.6.1"
       implementation "androidx.room:room-ktx:2.6.1"
       kapt "androidx.room:room-compiler:2.6.1"
       ```
    2. 在 `AndroidManifest.xml` 加入網路權限：`<uses-permission android:name="android.permission.INTERNET" />`
    3. 在 `app/build.gradle` 的 `android.defaultConfig` 區塊加入 API URL 設定：
       ```gradle
       buildConfigField("String", "API_BASE_URL", "\"https://your-api-server.com/api/produce/\"")
       ```
       正式環境可透過 CI/CD 環境變數注入，避免 URL 硬寫在程式碼中。

### 🍎 iOS (URLSession + SwiftData)
*   **程式碼位置**：`/native/iOS/ProduceApp/Data/ProduceRepository.swift`
*   **您需要做的設定**：
    1. 建立 `Config.plist` 並加入 `API_BASE_URL` 欄位，填入您的後端服務 URL。
    2. 在 `ProduceApp.swift` (主程式入口) 中加入 `.modelContainer(for: ProduceEntity.self)` 來初始化 SwiftData 環境。
    3. 在 UI 視圖中使用 `@Query var produceItems: [ProduceEntity]` 即可自動監聽資料庫變化。
    4. **注意**：`Config.plist` 不應 commit 至版本控制，請加入 `.gitignore`。`Configuration.swift` 在 Debug 模式下若未設定會觸發 `assertionFailure`。

---

## 2. 🔔 推播通知系統 (Firebase Cloud Messaging)

當菜價達標或異常時，即使 App 關閉也能收到通知。

### 🤖 Android
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/fcm/ProduceFirebaseMessagingService.kt`
*   **架構說明**：
    *   FCM Token 取得後以 **JWT Bearer** 向後端 `POST /fcm/token` 註冊，JWT 從 `SharedPreferences("produce_prefs")` 的 key `"jwt_token"` 讀取。
    *   收到推播通知後，若訊息包含 `produceId`，會建立帶有該 Extra 的 `PendingIntent`，點擊通知後開啟 `MainActivity` 並帶入 `produceId` 供 Deep Link 使用。
    *   網路請求直接使用 OkHttp（不依賴 ProduceService 實例化，避免循環依賴）。
    *   API URL 使用 `BuildConfig.API_BASE_URL`（不硬寫）。
*   **您需要做的設定**：
    1. 前往 [Firebase Console](https://console.firebase.google.com/) 建立專案，下載 `google-services.json` 並放入 `app/` 目錄。
    2. 在 `app/build.gradle` 加入 FCM 依賴：`implementation 'com.google.firebase:firebase-messaging-ktx:23.4.0'`
    3. 在 `AndroidManifest.xml` 註冊 Service：
       ```xml
       <service android:name=".fcm.ProduceFirebaseMessagingService" android:exported="true">
           <intent-filter>
               <action android:name="com.google.firebase.MESSAGING_EVENT" />
           </intent-filter>
       </service>
       ```
    4. 在 `MainActivity.kt` 的 `onCreate` 中讀取 `intent.getStringExtra("produceId")` 以處理 Deep Link 導覽。

### 🍎 iOS
*   **程式碼位置**：`/native/iOS/ProduceApp/FCM/AppDelegate.swift`
*   **架構說明**：
    *   FCM Token 取得後，從 `UserDefaults.standard.string(forKey: "jwt_token")` 讀取 JWT。
    *   若 JWT 已存在，直接呼叫 `sendFcmTokenToBackend(_:jwt:)` 以 Bearer Token 向後端 `POST /fcm/token` 註冊。
    *   若 App 冷啟動時 JWT 尚未取得（首次執行），會先執行 `Task { await ProduceService.shared.ensureJwtToken() }` 異步等候 JWT 後再進行 FCM 注冊，確保不遺漏任何冷啟動情境。
    *   API URL 使用 `Configuration.apiBaseUrl`（從 `Config.plist` 讀取）。
*   **您需要做的設定**：
    1. 前往 Firebase Console 下載 `GoogleService-Info.plist` 並拖入 Xcode 專案。
    2. 使用 Swift Package Manager (SPM) 或 CocoaPods 安裝 `firebase-ios-sdk`。
    3. 在 Apple Developer Portal 建立 **APNs Auth Key**，並上傳至 Firebase Console。
    4. 在 Xcode 的 `Signing & Capabilities` 中新增 **Push Notifications** 與 **Background Modes (Remote notifications)**。

---

## 3. 🌤️ 天氣警報整合 (Weather Alerts)

颱風或豪大雨時自動顯示警報卡片，提醒使用者受影響農作物可能漲價。

### 🤖 Android
*   **程式碼位置**：
    *   `WeatherAlertCard.kt`：訂閱 `viewModel.weatherAlerts`（`StateFlow<Resource<WeatherAlertDto>>`），當 `alertType == "None"` 時自動隱藏整個卡片。
    *   `ProduceDto.kt`：`WeatherAlertDto` 包含 `alertType`, `severity`, `title`, `message`, `affectedCrops`。
    *   `ProduceService.kt`：`@GET("weather-alerts") suspend fun getWeatherAlerts(): WeatherAlertDto`
*   **注意**：天氣警報載入失敗時不影響主資料（使用獨立 try-catch），靜默失敗。

### 🍎 iOS
*   後端 API 相同，前端以 `ProduceService.swift` 呼叫，對應的 SwiftUI 視圖以 Optional binding 判斷是否顯示。

---

## 4. 🍳 省錢食譜推薦 (Budget Recipe Generator)

依當季低價食材推薦省錢食譜，數據來自後端真實 API。

### 🤖 Android
*   **程式碼位置**：
    *   `BudgetRecipeCard.kt`：訂閱 `viewModel.budgetRecipes`，顯示橫向滾動食譜卡片列表，支援 Loading/Error/Empty/Success 四態 UI。
    *   `ProduceDto.kt`：`BudgetRecipeDto` 包含 `recipeName`, `mainIngredients`, `reason`, `imageUrl`（Emoji 字元）, `steps`。
    *   `ProduceService.kt`：`@GET("budget-recipes") suspend fun getBudgetRecipes(): List<BudgetRecipeDto>`
*   **注意**：食譜載入失敗時不影響主資料（使用獨立 try-catch），靜默失敗。

---

## 5. 🔑 JWT 身份驗證整合 (JWT Authentication)

所有 API 請求皆使用 JWT Bearer Token，已從舊版 `X-User-Id` Header 全面遷移。

### 🤖 Android
*   **JWT 取得**：`POST /auth/token` 傳入 `{ "deviceId": "<Android Device ID>" }` 取得 JWT。
*   **JWT 儲存**：存入 `SharedPreferences("produce_prefs")`，key 為 `"jwt_token"`。
*   **JWT 自動注入**：`JwtInterceptor.kt` 攔截所有 OkHttp 請求，自動加上 `Authorization: Bearer <token>`，若收到 401 則重新取得 token 並重試一次。
*   **API URL**：`RetrofitClient.kt` 使用 `BuildConfig.API_BASE_URL`，在 `build.gradle` 設定 `buildConfigField`：
    ```gradle
    buildConfigField("String", "API_BASE_URL", "\"https://your-server.com/api/produce/\"")
    ```

### 🍎 iOS
*   **JWT 取得**：呼叫 `ProduceService.shared.ensureJwtToken()` 以 `deviceId` 換取 JWT。
*   **JWT 儲存**：`UserDefaults.standard.set(token, forKey: "jwt_token")`。
*   **JWT 自動注入**：`ProduceService.swift` 在每個請求 `URLRequest` 中設定 `setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")`。
*   **API URL**：`Configuration.apiBaseUrl` 讀取 `Config.plist` 中的 `API_BASE_URL`，Debug 模式未設定則 `assertionFailure`。

### 🟣 Backend (安全設定)
*   **正式環境**：透過 OS 環境變數 `Jwt__SecretKey` 注入 JWT 簽名金鑰（雙底線 = ASP.NET Core 巢狀設定分隔符）。
*   **本地開發**：在 `appsettings.Development.json` 設定開發專用金鑰（不可 commit 至正式分支）。
*   **`appsettings.json`**：`SecretKey` 必須保持空字串，`Program.cs` 啟動時若發現金鑰為空則拋出 `InvalidOperationException` 阻止服務啟動。

---

## 6. 📱 桌面小工具 (Widgets)

讓使用者不用打開 App，在手機首頁就能看到今天高麗菜的價格。

### 🤖 Android (Glance)
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/widget/ProduceWidget.kt`
*   **您需要做的設定**：
    1. 在 `app/build.gradle` 加入 Glance 依賴：`implementation "androidx.glance:glance-appwidget:1.0.0"`
    2. 建立 `res/xml/produce_widget_info.xml` 定義 Widget 尺寸。
    3. 在 `AndroidManifest.xml` 註冊 Receiver：
       ```xml
       <receiver android:name=".widget.ProduceWidgetReceiver" android:exported="true">
           <intent-filter>
               <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
           </intent-filter>
           <meta-data android:name="android.appwidget.provider" android:resource="@xml/produce_widget_info" />
       </receiver>
       ```

### 🍎 iOS (WidgetKit)
*   **程式碼位置**：`/native/iOS/ProduceWidget/ProduceWidget.swift`
*   **您需要做的設定**：
    1. 在 Xcode 中點擊 `File` -> `New` -> `Target...`，選擇 **Widget Extension**，命名為 `ProduceWidget`。
    2. 將我提供的 `ProduceWidget.swift` 程式碼貼入新產生的檔案中。
    3. **重要**：若要讓 Widget 讀取主 App 的資料 (例如 SwiftData)，您必須在 Xcode 的 `Signing & Capabilities` 中為兩個 Target (主 App 與 Widget) 新增同一個 **App Groups** (例如 `group.com.yourname.produceapp`)。

---

## 7. ♿ 無障礙與長輩友善設計 (Accessibility)

### 🤖 Android
*   **`PriceTrendChart.kt`**：Canvas 折線圖加入 Compose 語意標籤：
    ```kotlin
    Canvas(modifier = Modifier.semantics {
        contentDescription = "農產品價格趨勢折線圖，包含歷史價格（綠色實線）與預測價格（橘色虛線）"
    })
    ```
    使螢幕閱讀器 (TalkBack) 能夠讀出圖表內容。
*   **`VoiceSearchButton.kt`**：按鈕說明文字已修正為「點擊說話」（與實際互動行為一致）。
*   **`TextToSpeechHelper.kt`**：菜價列表每一筆都有語音播報圖示按鈕，點擊後朗讀價格。

### 🍎 iOS
*   **`ElderlyModeView.swift`**：透過探索 Tab 導覽入口可直接進入大字體語音查詢模式。
*   **`TextToSpeechHelper.swift`**：以 `AVSpeechSynthesizer` 朗讀農產品價格。

---

## 8. 🎨 UI 動畫與視覺效果 (UI Animations)

### 🍎 iOS
*   **`Components.swift` (SkeletonView)**：骨架屏升級為 shimmer sweep 光澤掃過動畫：
    *   `LinearGradient` 從 `clear → white.opacity(0.5) → clear` 的漸層覆蓋在灰色底色上
    *   `withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false))` 驅動 `shimmerPhase` 從 `-1.0` 到 `1.3`，產生由左至右的流動光澤效果。

### 🤖 Android
*   **`HomeScreen.kt`**：`liquidGlass()` Modifier 以 `Color(0x40A5D6A7)` 半透明淡綠底色 + `1.dp` 白色細邊框 (`Color(0x60FFFFFF)`) + `RoundedCornerShape(16.dp)` 實現毛玻璃質感。
