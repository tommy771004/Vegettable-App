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

### 4. 開發與測試
*   在 Android Studio 中開啟您的專案 (`/native/Android` 目錄)。
*   點擊 **Device Manager** 建立一個虛擬設備 (Virtual Device，建議選擇 Pixel 7)。
*   點擊綠色三角形 **Run (Shift+F10)** 即可在模擬器上預覽 Liquid Glass UI。

### 5. 打包與上架 (Google Play)
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

### 3. 開發與測試
*   使用 Xcode 開啟您的專案 (`/native/iOS/ProduceApp.xcodeproj` 或 `.xcworkspace`)。
*   在左上角選擇目標模擬器 (例如 iPhone 15 Pro)。
*   點擊左上角的 **Play 按鈕 (Cmd + R)** 即可在 iOS 模擬器中編譯並執行 App，檢視 iOS 26 Liquid Glass 效果。

### 4. 打包與上架 (App Store)
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

## 肆、總結與最佳實踐

*   **版本控制**：強烈建議使用 Git 進行版本控制，並將程式碼推播至 GitHub 或 GitLab。
*   **UI 一致性**：目前雙平台皆已實作 Liquid Glass (毛玻璃) 風格。在修改 UI 時，請確保 Android (`liquidGlass` Modifier) 與 iOS (`.ultraThinMaterial`) 的視覺效果保持一致。
*   **API 金鑰安全**：若未來整合 Gemini API 或政府 Open Data，**絕對不要**將 API Key 直接寫死在程式碼中。Android 請使用 `local.properties`，iOS 請使用 `.xcconfig` 或環境變數來管理機密資訊。
