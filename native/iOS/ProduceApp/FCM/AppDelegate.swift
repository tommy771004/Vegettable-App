import SwiftUI
import FirebaseCore
import FirebaseMessaging

// 整合 Firebase Cloud Messaging (FCM) 的 AppDelegate
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        // 1. 初始化 Firebase
        FirebaseApp.configure()
        
        // 2. 設定推播通知代理
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { granted, error in
            print("推播權限狀態: \(granted)")
        }
        application.registerForRemoteNotifications()
        
        // 3. 設定 FCM 代理
        Messaging.messaging().delegate = self
        
        return true
    }
    
    // 取得 FCM Token
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token: \(String(describing: fcmToken))")

        guard let token = fcmToken else { return }

        // 讀取 JWT（由 ProduceService 存入 UserDefaults）
        let jwt = UserDefaults.standard.string(forKey: "jwt_token")

        if let jwt = jwt {
            sendFcmTokenToBackend(token, jwt: jwt)
        } else {
            // 尚無 JWT（首次冷啟動）→ 先取得 JWT 再重試
            Task {
                await ProduceService.shared.ensureJwtToken()
                if let jwt = UserDefaults.standard.string(forKey: "jwt_token") {
                    sendFcmTokenToBackend(token, jwt: jwt)
                }
            }
        }
    }

    private func sendFcmTokenToBackend(_ fcmToken: String, jwt: String) {
        // 從 Configuration 讀取 API URL（統一設定，不硬編碼）
        let endpoint = Configuration.apiBaseUrl + "fcm-token"
        guard let url = URL(string: endpoint) else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(jwt)", forHTTPHeaderField: "Authorization")

        let body: [String: String] = ["token": fcmToken]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        URLSession.shared.dataTask(with: request) { _, response, error in
            if let error = error {
                print("Failed to send FCM token to backend: \(error.localizedDescription)")
                return
            }
            if let httpResponse = response as? HTTPURLResponse {
                print("FCM token sent. Response code: \(httpResponse.statusCode)")
            }
        }.resume()
    }
    
    // 在前景收到推播時的處理
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // 允許在 App 開啟時依然顯示橫幅與聲音
        completionHandler([[.banner, .sound]])
    }
}

// 為何修改：原先 @main 進入點被註解掉，App 無法啟動。
// 現在取消註解讓 SwiftUI App 正常執行，並整合 FCM AppDelegate。
@main
struct ProduceApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
