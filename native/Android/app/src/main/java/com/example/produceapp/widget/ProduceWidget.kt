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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.produceapp.MainActivity
import com.example.produceapp.data.ProduceDatabase
import kotlinx.coroutines.flow.first

// 1. 定義 Widget 的 UI (使用 Glance - Jetpack Compose for Widgets)
class ProduceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = ProduceDatabase.getDatabase(context)
        val dao = db.produceDao()
        
        var cropName = "載入中..."
        var price = "--"
        var trend = ""

        try {
            // 嘗試從資料庫取得第一筆資料或特定作物 (例如高麗菜 LA1)
            val produceList = dao.getAllProduce().first()
            val targetCrop = produceList.find { it.cropCode == "LA1" } ?: produceList.firstOrNull()
            
            if (targetCrop != null) {
                cropName = targetCrop.cropName
                price = "$ ${targetCrop.averagePrice} / kg"
                // 趨勢需比較歷史資料，這裡暫時留空或顯示簡單狀態
                trend = "" 
            } else {
                cropName = "無資料"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cropName = "錯誤"
        }

        provideContent {
            WidgetContent(cropName, price, trend)
        }
    }

    @Composable
    private fun WidgetContent(cropName: String, price: String, trend: String) {
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
            if (trend.isNotEmpty()) {
                Text(
                    text = trend,
                    style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Red)),
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// 2. 接收 Widget 更新事件的 Receiver
class ProduceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProduceWidget()
}
