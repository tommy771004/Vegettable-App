import UserNotifications
import Foundation

class PriceAlertNotifier {
    static let shared = PriceAlertNotifier()

    private init() {}

    // Request permission from the user to send notifications
    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("Notification permission granted.")
            } else if let error = error {
                print("Notification permission error: \(error.localizedDescription)")
            }
        }
    }

    // Schedule a local notification when a price drop is detected
    func schedulePriceAlert(produceName: String, currentPrice: Double) {
        let content = UNMutableNotificationContent()
        content.title = "📉 價格降價通知！"
        content.body = "\(produceName) 目前批發價已降至 $\(currentPrice)，快去選購！"
        content.sound = .default

        // Trigger immediately for demonstration purposes
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to schedule notification: \(error.localizedDescription)")
            }
        }
    }
}
