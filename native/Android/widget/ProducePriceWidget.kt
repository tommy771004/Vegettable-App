package com.example.produceapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class ProducePriceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 在實際應用中，這裡會從 Room Database 或 WorkManager 取得最新快取價格
        val cropName = "高麗菜"
        val currentPrice = "25.5"
        val trend = "▼ 2.1%" // 跌價
        val trendColor = Color(0xFF4CAF50) // 綠色代表跌價 (對消費者是好事)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今日最便宜",
                    style = TextStyle(fontSize = 14.sp, color = androidx.glance.unit.ColorProvider(Color.Gray))
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = cropName,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$$currentPrice",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.glance.unit.ColorProvider(Color.Black))
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = trend,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.glance.unit.ColorProvider(trendColor))
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "最後更新: 08:00 AM",
                    style = TextStyle(fontSize = 10.sp, color = androidx.glance.unit.ColorProvider(Color.LightGray))
                )
            }
        }
    }
}
