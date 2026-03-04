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

### 🍎 iOS (URLSession + SwiftData)
*   **程式碼位置**：`/native/iOS/ProduceApp/Data/ProduceRepository.swift`
*   **您需要做的設定**：
    1. 在 `ProduceApp.swift` (主程式入口) 中加入 `.modelContainer(for: ProduceEntity.self)` 來初始化 SwiftData 環境。
    2. 在 UI 視圖中使用 `@Query var produceItems: [ProduceEntity]` 即可自動監聽資料庫變化。

---

## 2. 🔔 推播通知系統 (Firebase Cloud Messaging)

當菜價達標或異常時，即使 App 關閉也能收到通知。

### 🤖 Android
*   **程式碼位置**：`/native/Android/app/src/main/java/com/example/produceapp/fcm/ProduceFirebaseMessagingService.kt`
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

### 🍎 iOS
*   **程式碼位置**：`/native/iOS/ProduceApp/FCM/AppDelegate.swift`
*   **您需要做的設定**：
    1. 前往 Firebase Console 下載 `GoogleService-Info.plist` 並拖入 Xcode 專案。
    2. 使用 Swift Package Manager (SPM) 或 CocoaPods 安裝 `firebase-ios-sdk`。
    3. 在 Apple Developer Portal 建立 **APNs Auth Key**，並上傳至 Firebase Console。
    4. 在 Xcode 的 `Signing & Capabilities` 中新增 **Push Notifications** 與 **Background Modes (Remote notifications)**。

---

## 3. 📱 桌面小工具 (Widgets)

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
