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
import com.example.produceapp.data.HistoricalPriceDto
import com.example.produceapp.data.PricePredictionDto

@Composable
fun PriceTrendChart(
    historical: List<HistoricalPriceDto>,
    predicted: List<PricePredictionDto>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val maxPrice = (historical.maxOfOrNull { it.avgPrice } ?: 0f).coerceAtLeast(
            predicted.maxOfOrNull { it.predictedPrice } ?: 0f
        ) * 1.2f // 留點頂部空間
        
        val totalPoints = historical.size + predicted.size
        if (totalPoints == 0 || maxPrice == 0f) return@Canvas

        val stepX = size.width / (totalPoints - 1).coerceAtLeast(1)
        val scaleY = size.height / maxPrice

        // 繪製歷史價格 (實線，深綠色)
        val histPath = Path()
        historical.forEachIndexed { index, data ->
            val x = index * stepX
            val y = size.height - (data.avgPrice * scaleY)
            if (index == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
            drawCircle(color = Color(0xFF2E7D32), radius = 4.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path = histPath, color = Color(0xFF2E7D32), style = Stroke(width = 2.dp.toPx()))

        // 繪製預測價格 (虛線，橘色)
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
                drawCircle(color = Color(0xFFFF9800), radius = 4.dp.toPx(), center = Offset(x, y))
            }
            drawPath(
                path = predPath,
                color = Color(0xFFFF9800),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }
    }
}
