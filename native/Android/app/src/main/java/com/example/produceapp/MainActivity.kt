package com.example.produceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.produceapp.ui.MainScreen
import com.example.produceapp.ui.ProduceViewModel
import com.example.produceapp.util.TextToSpeechHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var ttsHelper: TextToSpeechHelper

    // Compose 可觀察的深度連結 State：當 App 已開啟時收到新通知，
    // 更新此 state 可觸發 Recomposition，讓 HomeScreen 即時高亮目標農產品
    private var deepLinkProduceId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsHelper = TextToSpeechHelper(this)

        // FCM Deep Link: 從推播通知的 PendingIntent 取得 produceId
        deepLinkProduceId = intent.getStringExtra("produceId")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ProduceViewModel = hiltViewModel()
                    // deepLinkProduceId 是 Compose State，onNewIntent 更新後自動重組
                    MainScreen(viewModel, ttsHelper, deepLinkProduceId = deepLinkProduceId)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 更新 Compose State → 觸發 MainScreen 重組 → HomeScreen 高亮新 produceId
        deepLinkProduceId = intent.getStringExtra("produceId")
    }

    override fun onDestroy() {
        if (::ttsHelper.isInitialized) {
            ttsHelper.shutdown()
        }
        super.onDestroy()
    }
}
