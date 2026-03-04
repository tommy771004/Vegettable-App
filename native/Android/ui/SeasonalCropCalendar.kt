package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeasonalCrop(val name: String, val reason: String, val icon: String)

@Composable
fun SeasonalCropCalendar(modifier: Modifier = Modifier) {
    // 模擬從後端 GET /api/produce/seasonal 取得的當季資料
    val currentMonthCrops = listOf(
        SeasonalCrop("高麗菜", "盛產期，價格平穩", "🥬"),
        SeasonalCrop("白蘿蔔", "冬季最甜，適合燉湯", "蘿蔔"), // Emoji 替代
        SeasonalCrop("菠菜", "當季蔬菜，營養價值高", "🥬"),
        SeasonalCrop("番茄", "溫室盛產，酸甜適中", "🍅")
    )

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "📅 本月當季盛產推薦",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        // 使用 LazyRow 製作橫向滑動的卡片列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(currentMonthCrops) { crop ->
                Card(
                    modifier = Modifier.width(140.dp).height(130.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = crop.icon, fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = crop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = crop.reason, 
                            style = MaterialTheme.typography.caption, 
                            color = Color.Gray,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
