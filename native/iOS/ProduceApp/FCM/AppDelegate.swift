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
        // TODO: 將 Token 送回您的後端伺服器
    }
    
    // 在前景收到推播時的處理
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // 允許在 App 開啟時依然顯示橫幅與聲音
        completionHandler([[.banner, .sound]])
    }
}

// 在主程式中套用 AppDelegate
/*
@main
struct ProduceApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
*/
