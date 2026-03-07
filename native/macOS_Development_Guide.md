# 🍏 macOS 環境建置與雙平台開發部署指南

這份指南將帶領您從零開始，在 macOS 環境上建置 Android 與 iOS 的開發環境，並涵蓋從開發、測試到最終打包上架 (App Store & Google Play) 的完整流程。

---

## 壹、基礎環境準備

在 macOS 上，我們強烈建議使用 **Homebrew** 來管理套件。

1. **安裝 Homebrew** (若尚未安裝)：
   打開終端機 (Terminal) 並輸入：
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **安裝 Git**：
   ```bash
   brew install git
   ```

---

## 貳、Android 開發環境建置 (Jetpack Compose)

### 1. 安裝 Java 開發套件 (JDK)
Android 開發需要 JDK。建議安裝 Zulu JDK (支援 Apple Silicon M1/M2/M3)：
```bash
brew install --cask zulu
```

### 2. 安裝 Android Studio
```bash
brew install --cask android-studio
```
*   開啟 Android Studio，按照安裝精靈 (Setup Wizard) 下載預設的 Android SDK、SDK Platform-Tools 與 Android Emulator。

### 3. 設定環境變數
在您的 `~/.zshrc` (或 `~/.bash_profile`) 中加入：
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
```
執行 `source ~/.zshrc` 使其生效。

### 4. 設定 API URL (BuildConfig)
本專案的 API 基底 URL 透過 `buildConfigField` 在編譯期注入，不硬寫在程式碼中。
在 `app/build.gradle` 的 `android.defaultConfig` 區塊設定：
```gradle
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://your-dev-server.com/api/produce/\"")
    }
    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://your-prod-server.com/api/produce/\"")
        }
    }
}
```
CI/CD 部署時可透過 Gradle 參數或環境變數覆蓋此值。

### 5. 開發與測試
*   在 Android Studio 中開啟您的專案 (`/native/Android` 目錄)。
*   點擊 **Device Manager** 建立一個虛擬設備 (Virtual Device，建議選擇 Pixel 7)。
*   點擊綠色三角形 **Run (Shift+F10)** 即可在模擬器上預覽 Liquid Glass UI。

### 6. 打包與上架 (Google Play)
1.  **產生簽名金鑰 (Keystore)**：
    在 Android Studio 中，選擇 `Build` -> `Generate Signed Bundle / APK` -> 選擇 `Android App Bundle` -> 點擊 `Create new...` 建立金鑰。
2.  **打包 AAB 檔案**：
    完成設定後，Android Studio 會產出 `.aab` (Android App Bundle) 檔案。
3.  **上架 Google Play Console**：
    登入 [Google Play Console](https://play.google.com/console/)，建立新應用程式，並將產出的 `.aab` 檔案上傳至「正式版 (Production)」或「內部測試 (Internal Testing)」軌道。

---

## 參、iOS 開發環境建置 (SwiftUI)

### 1. 安裝 Xcode
*   請前往 Mac 上的 **App Store** 搜尋並下載 **Xcode** (檔案較大，需耐心等待)。
*   安裝完成後，開啟 Xcode 並同意授權條款。
*   安裝 Command Line Tools：
    ```bash
    xcode-select --install
    ```

### 2. 安裝 CocoaPods (若專案有使用第三方套件)
雖然我們目前主要使用原生 SwiftUI，但若未來需要整合 Firebase 等套件，會需要 CocoaPods：
```bash
brew install cocoapods
```

### 3. 設定 API URL (Config.plist)
本專案的 API 基底 URL 從 `Config.plist` 讀取（透過 `Configuration.swift`），不硬寫在程式碼中。
1. 在 Xcode 專案目錄建立 `Config.plist`（選 Property List 格式）。
2. 加入一個 String 型別的 key：`API_BASE_URL`，填入您的後端服務 URL。
3. **重要**：將 `Config.plist` 加入 `.gitignore`，避免 URL 被 commit 至版本控制。
4. Debug 模式下若 `Config.plist` 未設定，`Configuration.swift` 會觸發 `assertionFailure` 立即提醒開發者。

### 4. 開發與測試
*   使用 Xcode 開啟您的專案 (`/native/iOS/ProduceApp.xcodeproj` 或 `.xcworkspace`)。
*   在左上角選擇目標模擬器 (例如 iPhone 15 Pro)。
*   點擊左上角的 **Play 按鈕 (Cmd + R)** 即可在 iOS 模擬器中編譯並執行 App，檢視 iOS 26 Liquid Glass 效果。

### 5. 打包與上架 (App Store)
1.  **Apple Developer 帳號**：
    您需要付費註冊 [Apple Developer Program](https://developer.apple.com/programs/) (每年 $99 USD)。
2.  **設定 Signing & Capabilities**：
    在 Xcode 專案設定中，選擇您的 Target，進入 `Signing & Capabilities`，登入您的 Apple ID，並設定唯一的 `Bundle Identifier` (例如 `com.yourname.produceapp`)。
3.  **打包 Archive**：
    *   將左上角的目標設備從模擬器改為 **Any iOS Device (arm64)**。
    *   點擊頂部選單 `Product` -> `Archive`。
4.  **上傳至 App Store Connect**：
    *   Archive 完成後會跳出 Organizer 視窗。
    *   點擊 **Distribute App**，選擇 `App Store Connect`，按照步驟上傳。
5.  **TestFlight 與正式上架**：
    上傳成功後，登入 [App Store Connect](https://appstoreconnect.apple.com/)，您可以在 TestFlight 發送測試邀請給內部人員，確認無誤後即可送審 (Submit for Review) 正式上架。

---

## 肆、後端開發環境建置 (ASP.NET Core 8)

### 1. 安裝 .NET 8 SDK
```bash
brew install --cask dotnet-sdk
```
安裝完成後確認版本：
```bash
dotnet --version
```

### 2. 設定 JWT Secret 金鑰（重要安全事項）

本專案採用 JWT Bearer Token 進行身份驗證，金鑰**絕對不可**硬寫在 `appsettings.json` 中。

**本地開發設定**：
1. 在後端目錄建立 `appsettings.Development.json`（已加入 `.gitignore`）。
2. 填入開發專用金鑰（長度至少 32 字元）：
   ```json
   {
     "Jwt": {
       "SecretKey": "YourDevelopment-Only-Key-NotForProduction-Min32Chars!"
     }
   }
   ```
3. 確認 `appsettings.json` 的 `Jwt.SecretKey` 為空字串：
   ```json
   {
     "Jwt": {
       "SecretKey": ""
     }
   }
   ```

**正式環境設定**：
透過 OS 環境變數注入（ASP.NET Core 雙底線 `__` 為巢狀設定分隔符）：
```bash
# Linux / macOS
export Jwt__SecretKey="YourSuperSecretProductionKey_AtLeast32Chars!"

# Windows PowerShell
$env:Jwt__SecretKey = "YourSuperSecretProductionKey_AtLeast32Chars!"

# Docker / Kubernetes
# 在 docker-compose.yml 或 k8s Secret 中設定
```

**`Program.cs` 啟動驗證**：若 `Jwt__SecretKey` 為空，服務啟動時會立即拋出 `InvalidOperationException` 並顯示設定指引，防止以不安全的金鑰運行。

### 3. 本地執行後端
```bash
cd native/Backend
dotnet run
```
後端預設監聽 `http://localhost:5000`，API 文件可在 `http://localhost:5000/swagger` 查看。

### 4. Redis 快取（選用）
天氣警報等 API 支援 Redis 快取。本地開發可使用 Docker：
```bash
docker run -d -p 6379:6379 redis:alpine
```
正式環境請透過環境變數設定 Redis 連線字串，不要硬寫在 `appsettings.json` 中：
```bash
export ConnectionStrings__Redis="your-redis-server:6379,password=yourpassword"
```

---

## 伍、總結與最佳實踐

*   **版本控制**：強烈建議使用 Git 進行版本控制，並將程式碼推播至 GitHub 或 GitLab。
*   **UI 一致性**：目前雙平台皆已實作 Liquid Glass (毛玻璃) 風格。在修改 UI 時，請確保 Android (`liquidGlass` Modifier) 與 iOS (`.ultraThinMaterial`) 的視覺效果保持一致。
*   **機密資訊管理**（API 金鑰、JWT Secret）：
    *   **Android**：使用 `build.gradle` 的 `buildConfigField` 搭配 `local.properties` 或 CI/CD 環境變數。
    *   **iOS**：使用 `Config.plist`（不 commit）或 `.xcconfig` 管理 API URL；敏感金鑰使用 Keychain 儲存。
    *   **Backend**：所有金鑰透過 OS 環境變數或 Kubernetes Secret 注入，**絕對不要**將任何金鑰或密碼 commit 至 `appsettings.json`。
*   **無障礙設計**：Android Canvas 圖表記得加入 `semantics { contentDescription }` 語意標籤；iOS 確保重要元件有適當的 `.accessibilityLabel`。
*   **FCM Deep Link**：Android 收到推播點擊後，在 `MainActivity.kt` 的 `onCreate` 讀取 `intent.getStringExtra("produceId")` 以跳轉至對應農產品詳情頁。
