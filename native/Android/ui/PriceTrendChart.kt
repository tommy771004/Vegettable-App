package com.example.produceapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.produceapp.data.HistoricalPriceDto
import com.example.produceapp.data.PricePredictionDto

@Composable
fun PriceTrendChart(
    historical: List<HistoricalPriceDto>,
    predicted: List<PricePredictionDto>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Idea C: 一鍵分享到 LINE / 社群
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = "價格趨勢與預測",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val latestPrice = historical.lastOrNull()?.avgPrice ?: 0f
                val shareText = "【菜價快報 🥬】\n目前最新均價為 $latestPrice 元！\n快用「台灣農產品價格追蹤 App」查看更多買菜秘訣！"
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "分享菜價"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "分享到 LINE", tint = Color(0xFF4CAF50))
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val maxPrice = (historical.maxOfOrNull { it.avgPrice } ?: 0f).coerceAtLeast(
                predicted.maxOfOrNull { it.predictedPrice } ?: 0f
            ) * 1.2f // 留點頂部空間
            
            val totalPoints = historical.size + predicted.size
            if (totalPoints == 0 || maxPrice == 0f) return@Canvas

            val stepX = size.width / (totalPoints - 1).coerceAtLeast(1)
            val scaleY = size.height / maxPrice

            // 繪製歷史價格 (實線，亮綠色)
            val histPath = Path()
            historical.forEachIndexed { index, data ->
                val x = index * stepX
                val y = size.height - (data.avgPrice * scaleY)
                if (index == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
                drawCircle(color = Color(0xFF4CAF50), radius = 6.dp.toPx(), center = Offset(x, y)) // 加大資料點並改為亮綠色
            }
            drawPath(path = histPath, color = Color(0xFF4CAF50), style = Stroke(width = 3.dp.toPx())) // 加粗線條

            // 繪製預測價格 (虛線，亮橘色)
            if (predicted.isNotEmpty() && historical.isNotEmpty()) {
                val predPath = Path()
                val lastHist = historical.last()
                val startX = (historical.size - 1) * stepX
                val startY = size.height - (lastHist.avgPrice * scaleY)
                predPath.moveTo(startX, startY)

                predicted.forEachIndexed { index, data ->
                    val x = (historical.size + index) * stepX
                    val y = size.height - (data.predictedPrice * scaleY)
                    predPath.lineTo(x, y)
                    drawCircle(color = Color(0xFFFF5722), radius = 6.dp.toPx(), center = Offset(x, y)) // 加大資料點並改為亮橘色
                }
                drawPath(
                    path = predPath,
                    color = Color(0xFFFF5722),
                    style = Stroke(
                        width = 3.dp.toPx(), // 加粗線條
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) // 調整虛線間距
                    )
                )
            }
        }
    }
}
