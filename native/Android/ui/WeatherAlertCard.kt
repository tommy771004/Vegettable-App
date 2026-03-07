package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.WeatherAlertDto
import com.example.produceapp.util.Resource

@Composable
fun WeatherAlertCard(
    viewModel: ProduceViewModel,
    modifier: Modifier = Modifier
) {
    val weatherState by viewModel.weatherAlerts.collectAsState()

    when (weatherState) {
        is Resource.Loading -> { /* 天氣警報靜默載入，不顯示 Skeleton */ }
        is Resource.Error   -> { /* 載入失敗時不顯示卡片，避免錯誤訊息干擾主畫面 */ }
        is Resource.Success -> {
            val alert = (weatherState as Resource.Success<WeatherAlertDto>).data ?: return
            if (alert.alertType == "None") return // 無警報時不顯示卡片

            Card(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFFFFEBEE)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "天氣警告",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(32.dp).padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = alert.title ?: "⚠️ 天氣警報",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.message ?: "",
                                style = MaterialTheme.typography.body2,
                                color = Color.DarkGray,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    // 受影響農作物清單
                    alert.affectedCrops?.takeIf { it.isNotEmpty() }?.let { crops ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "受影響農作物：${crops.joinToString("、")}",
                            style = MaterialTheme.typography.caption,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
