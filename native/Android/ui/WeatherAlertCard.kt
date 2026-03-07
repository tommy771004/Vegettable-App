package com.example.produceapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.WeatherAlertDto
import com.example.produceapp.util.Resource

// =============================================================================
// WeatherAlertCard.kt — 天氣警報卡片（Material 3 沉浸式版本）
//
// 更新：
//   - 颱風警報 → 深紅漸層 + 閃爍動畫 (InfiniteTransition alpha)
//   - 豪大雨   → 藍紫漸層（沉穩提示）
//   - 嚴重程度徽章：High/Medium/Low → 紅/橙/黃色圓角標籤
// =============================================================================

@Composable
fun WeatherAlertCard(
    viewModel: ProduceViewModel,
    modifier: Modifier = Modifier
) {
    val weatherState by viewModel.weatherAlerts.collectAsState()

    when (weatherState) {
        is Resource.Loading -> { }
        is Resource.Error   -> { }
        is Resource.Success -> {
            val alert = (weatherState as Resource.Success<WeatherAlertDto>).data ?: return
            if (alert.alertType == "None") return

            val isTyphoon = alert.alertType == "Typhoon"
            val gradientColors = if (isTyphoon)
                listOf(Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFE53935))
            else
                listOf(Color(0xFF1565C0), Color(0xFF1976D2), Color(0xFF42A5F5))

            val iconVector: ImageVector = if (isTyphoon) Icons.Default.Thunderstorm else Icons.Default.WaterDrop

            val infiniteTransition = rememberInfiniteTransition(label = "weatherPulse")
            val pulseAlpha by if (isTyphoon) infiniteTransition.animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "pulseAlpha"
            ) else infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000)), label = "noPulse"
            )

            val severityColor = when (alert.severity) {
                "High"   -> Color(0xFFFF1744)
                "Medium" -> Color(0xFFFF9100)
                else     -> Color(0xFFFFD600)
            }
            val severityLabel = when (alert.severity) {
                "High"   -> "高度警戒"
                "Medium" -> "中度警戒"
                else     -> "低度警戒"
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = pulseAlpha) }))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(iconVector, contentDescription = "天氣警報", tint = Color.White, modifier = Modifier.size(32.dp))
                        Text(
                            text = alert.title ?: "⚠️ 天氣警報",
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = Color.White, modifier = Modifier.weight(1f)
                        )
                        alert.severity?.let {
                            Surface(shape = RoundedCornerShape(50), color = severityColor.copy(alpha = 0.3f)) {
                                Text(
                                    text = severityLabel, color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    alert.message?.takeIf { it.isNotBlank() }?.let { msg ->
                        Text(text = msg, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
                    }

                    alert.affectedCrops?.takeIf { it.isNotEmpty() }?.let { crops ->
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Text(
                                text = "受影響：${crops.joinToString("、")}",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
