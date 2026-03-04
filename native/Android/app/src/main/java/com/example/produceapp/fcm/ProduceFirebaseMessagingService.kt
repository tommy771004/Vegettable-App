package com.example.produceapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.produceapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ProduceFirebaseMessagingService : FirebaseMessagingService() {

    // 當收到推播訊息時觸發 (例如：高麗菜價格異常、達到目標價)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 處理 FCM Data Payload 或 Notification Payload
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "菜價警報"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "您追蹤的菜價有新動態！"

        showNotification(title, body)
    }

    // 當 FCM Token 更新時觸發 (需將 Token 送回您的後端伺服器)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New FCM Token: $token")
        
        val produceService = com.example.produce.data.ProduceService()
        produceService.sendFcmToken(token, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                println("Failed to send FCM token to backend: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                println("Successfully sent FCM token to backend. Response code: ${response.code}")
            }
        })
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "produce_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 以上需要建立 Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "菜價異常與達標警報",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // 替換為您的 App Icon
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
