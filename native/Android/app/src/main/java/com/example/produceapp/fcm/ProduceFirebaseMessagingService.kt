package com.example.produceapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.produceapp.BuildConfig
import com.example.produceapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ProduceFirebaseMessagingService : FirebaseMessagingService() {

    // 當收到推播訊息時觸發 (例如：高麗菜價格異常、達到目標價)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "菜價警報"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "您追蹤的菜價有新動態！"
        val produceId = remoteMessage.data["produceId"]

        showNotification(title, body, produceId)
    }

    // 當 FCM Token 更新時觸發 (需將 Token 送回後端伺服器)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New FCM Token: $token")

        val prefs = getSharedPreferences("vegettable_prefs", Context.MODE_PRIVATE)
        val jwt = prefs.getString("jwt_token", null) ?: run {
            println("No JWT available, skipping FCM token registration")
            return
        }

        val fcmUrl = BuildConfig.API_BASE_URL + "fcm-token"
        val client = OkHttpClient()
        val body = JSONObject().apply { put("token", token) }
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(fcmUrl)
            .post(body)
            .header("Authorization", "Bearer $jwt")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                println("Failed to send FCM token to backend: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                println("FCM token registered. Response code: ${response.code}")
                response.close()
            }
        })
    }

    private fun showNotification(title: String, message: String, produceId: String? = null) {
        val channelId = "produce_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "菜價異常與達標警報",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link：點擊推播開啟 MainActivity 並帶入 produceId
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            produceId?.let { putExtra("produceId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
