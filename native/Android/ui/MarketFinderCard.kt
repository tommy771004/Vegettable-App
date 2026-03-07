package com.example.produceapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.produceapp.data.MarketCompareDto
import kotlinx.coroutines.launch

// =============================================================================
// MarketFinderCard.kt — 市場尋找 + 各市場比價功能卡片
//
// 新功能（本次更新）：
//   在原有「開啟 Google Maps 導航至附近市場」的基礎上，
//   新增「農產品比價」功能：
//   1. 使用者輸入農產品名稱（例：「高麗菜」）
//   2. 呼叫後端 GET /api/produce/compare/{cropName}
//   3. 顯示各市場的當日批發均價，由低到高排序
//   4. 使用者可選擇要前往的市場，點擊「導航」開啟 Google Maps
// =============================================================================

@Composable
fun MarketFinderCard(
    viewModel: ProduceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cropQuery by remember { mutableStateOf("") }
    var comparisonResults by remember { mutableStateOf<List<MarketCompareDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // 標題列
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📍 找市場 & 比價", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("輸入農產品名稱查看各市場批發均價", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // 搜尋欄 + 比價按鈕
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cropQuery,
                    onValueChange = { cropQuery = it },
                    placeholder = { Text("例：高麗菜") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFFFFCC80)
                    )
                )
                Button(
                    onClick = {
                        if (cropQuery.isNotBlank()) {
                            scope.launch {
                                isLoading = true
                                hasSearched = false
                                comparisonResults = viewModel.getMarketComparison(cropQuery)
                                hasSearched = true
                                isLoading = false
                            }
                        }
                    },
                    enabled = cropQuery.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "查詢", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 比價結果列表
            if (hasSearched) {
                if (comparisonResults.isEmpty()) {
                    Text(
                        "查無「$cropQuery」的比價資料",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        "「$cropQuery」各市場批發均價（由低到高）",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF795548),
                        fontWeight = FontWeight.Bold
                    )
                    comparisonResults.forEachIndexed { index, item ->
                        val rankColor = when (index) {
                            0 -> Color(0xFF4CAF50)  // 最便宜 → 綠
                            1 -> Color(0xFF8BC34A)
                            else -> Color(0xFF9E9E9E)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (index == 0) "🥇" else if (index == 1) "🥈" else "  ",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.marketName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "$${String.format("%.1f", item.avgPrice)}/kg",
                                    fontWeight = FontWeight.Bold,
                                    color = rankColor
                                )
                                IconButton(
                                    onClick = {
                                        // 點擊特定市場 → 直接導航到該市場
                                        val query = Uri.encode("${item.marketName} 果菜市場")
                                        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            val webUri = Uri.parse("https://www.google.com/maps/search/${Uri.encode(item.marketName)}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = "導航到${item.marketName}",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (index < comparisonResults.lastIndex) {
                            HorizontalDivider(color = Color(0xFFFFE0B2), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // 通用導航按鈕（不指定特定農產品，找附近所有市場）
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=果菜市場 傳統市場")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/果菜市場"))
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Place, contentDescription = "地圖", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("找附近的果菜市場")
            }
        }
    }
}
