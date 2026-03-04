package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.produceapp.utils.TextToSpeechHelper

// 毛玻璃效果 Modifier (淡綠色底 + 半透明 + 細邊框)
fun Modifier.liquidGlass() = this
    .clip(RoundedCornerShape(16.dp))
    .background(Color(0x40A5D6A7)) // 淡綠色半透明 (Light Green, 25% opacity)
    .border(1.dp, Color(0x60FFFFFF), RoundedCornerShape(16.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ProduceViewModel, ttsHelper: TextToSpeechHelper) {
    val anomalies by viewModel.anomalies.collectAsState()
    val topVolume by viewModel.topVolume.collectAsState()
    val dailyPrices by viewModel.dailyPrices.collectAsState()
    val historicalData by viewModel.historicalData.collectAsState()
    val predictedData by viewModel.predictedData.collectAsState()

    // 漸層背景，讓毛玻璃效果更明顯
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)) // 極淡綠色漸層
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("農產品批發價查詢", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 價格異常警報
                if (anomalies.isNotEmpty()) {
                    item {
                        Text("⚠️ 價格異常警報", style = MaterialTheme.typography.titleMedium, color = Color.Red)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            anomalies.forEach { anomaly ->
                                Box(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.Red)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(anomaly.alertMessage, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 歷史價格與 7 日趨勢預測 (Line Chart)
                if (historicalData.isNotEmpty() || predictedData.isNotEmpty()) {
                    item {
                        Text("📈 高麗菜 價格趨勢與預測", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).liquidGlass().padding(16.dp)) {
                            PriceTrendChart(historical = historicalData, predicted = predictedData)
                        }
                    }
                }

                // 3. 熱門交易農產品
                if (topVolume.isNotEmpty()) {
                    item {
                        Text("🔥 今日熱門交易", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            items(topVolume) { crop ->
                                Box(modifier = Modifier.width(120.dp).liquidGlass().padding(12.dp)) {
                                    Column {
                                        Text(crop.cropName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1B5E20))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$${crop.avgPrice}/kg", color = Color(0xFF388E3C), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 今日菜價列表
                item { Text("📋 今日菜價", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32)) }
                items(dailyPrices) { produce ->
                    Box(modifier = Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(produce.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                Text(produce.marketName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$${produce.avgPrice}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { ttsHelper.speak("今日 ${produce.cropName} 價格是 ${produce.avgPrice} 元") }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "語音播報", tint = Color(0xFF388E3C))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
