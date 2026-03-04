package com.example.produceapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.produceapp.MainActivity

// 1. 定義 Widget 的 UI (使用 Glance - Jetpack Compose for Widgets)
class ProduceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        // 模擬從本地資料庫讀取今日高麗菜價格
        val cropName = "高麗菜"
        val price = "$ 25.0 / kg"
        val trend = "▲ 15%"

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE8F5E9)) // 淺綠色背景
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()), // 點擊開啟 App
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日 $cropName",
                style = TextStyle(fontWeight = FontWeight.Bold, color = androidx.glance.unit.ColorProvider(Color(0xFF1B5E20)))
            )
            Text(
                text = price,
                style = TextStyle(fontWeight = FontWeight.Bold, color = androidx.glance.unit.ColorProvider(Color.Black)),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
            Text(
                text = trend,
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Red)),
                modifier = GlanceModifier.padding(top = 4.dp)
            )
        }
    }
}

// 2. 接收 Widget 更新事件的 Receiver
class ProduceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProduceWidget()
}
