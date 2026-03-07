package com.example.produceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.produceapp.ui.MainScreen
import com.example.produceapp.ui.ProduceViewModel
import com.example.produceapp.util.TextToSpeechHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var ttsHelper: TextToSpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsHelper = TextToSpeechHelper(this)

        // FCM Deep Link: 從推播通知的 PendingIntent 取得 produceId
        val deepLinkProduceId: String? = intent.getStringExtra("produceId")

        setContent {
            // Use default MaterialTheme for now as ProduceAppTheme might not be defined
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ProduceViewModel = hiltViewModel()
                    MainScreen(viewModel, ttsHelper, deepLinkProduceId = deepLinkProduceId)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // App 已在背景時收到新的 FCM Deep Link 通知
        setIntent(intent)
    }

    override fun onDestroy() {
        if (::ttsHelper.isInitialized) {
            ttsHelper.shutdown()
        }
        super.onDestroy()
    }
}
