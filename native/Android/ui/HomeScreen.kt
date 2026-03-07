package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.util.TextToSpeechHelper
import com.example.produceapp.util.Resource
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// liquidGlass() 已移至 Components.kt 集中管理（同 package 直接可用）

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProduceViewModel,
    ttsHelper: TextToSpeechHelper,
    onNavigateToGroceryList: () -> Unit = {},
    onNavigateToElderlyMode: () -> Unit = {},
    highlightedProduceId: String? = null
) {
    val anomaliesState by viewModel.anomalies.collectAsState()
    val topVolumeState by viewModel.topVolume.collectAsState()
    val dailyPricesState by viewModel.dailyPrices.collectAsState()
    val historicalDataState by viewModel.historicalData.collectAsState()
    val predictedDataState by viewModel.predictedData.collectAsState()

    // 搜尋關鍵字狀態：同時支援語音輸入與文字輸入
    var searchKeyword by remember { mutableStateOf("") }

    // 收藏對話框狀態
    var favoriteTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // produceId to cropName
    var targetPriceInput by remember { mutableStateOf("") }
    var favoriteSuccessMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("農產品批發價查詢", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = {
            // 收藏成功 Snackbar
            favoriteSuccessMsg?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { favoriteSuccessMsg = null }) { Text("關閉") }
                    },
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ) { Text(msg) }
            }
        }
    ) { padding ->

        // 加入收藏對話框
        favoriteTarget?.let { (produceId, cropName) ->
            AlertDialog(
                onDismissRequest = { favoriteTarget = null },
                title = { Text("❤️ 加入收藏：$cropName") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("設定到價提醒：當價格低於此目標價時，系統將發送通知。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        OutlinedTextField(
                            value = targetPriceInput,
                            onValueChange = { targetPriceInput = it },
                            label = { Text("目標提醒價格 (元/公斤)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = targetPriceInput.toDoubleOrNull()
                            if (price != null) {
                                scope.launch {
                                    val ok = viewModel.addToFavorites(produceId, price)
                                    favoriteSuccessMsg = if (ok) "✅ $cropName 已加入收藏，目標價 $$price/kg" else "⚠️ 加入收藏失敗，請稍後再試"
                                }
                                favoriteTarget = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("確認", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { favoriteTarget = null }) { Text("取消") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 0. 🌟 Hero 摘要橫幅
                item {
                    val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
                    } else ""
                    val totalCount = (dailyPricesState as? Resource.Success)?.data?.size ?: 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🌿 今日農產批發", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(today, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                if (totalCount > 0) {
                                    Text("共 $totalCount 筆今日行情", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                                    Text("🇹🇼 臺灣農業", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF4CAF50).copy(alpha = 0.4f)) {
                                    Text("即時更新", color = Color.White, fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                // 1. 🔴 價格異常警報（優先顯示，最緊急）
                item {
                    when (anomaliesState) {
                        is Resource.Loading -> SkeletonLoader(modifier = Modifier.fillMaxWidth().height(50.dp))
                        is Resource.Error -> { /* 不阻擋其他內容 */ }
                        is Resource.Success -> {
                            val anomalies = (anomaliesState as Resource.Success).data ?: emptyList()
                            if (anomalies.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚠️ 價格異常警報", style = MaterialTheme.typography.titleMedium, color = Color.Red)
                                    anomalies.forEach { anomaly ->
                                        Box(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = "異常警報", tint = Color.Red)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(anomaly.alertMessage, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 🔥 今日熱門交易（Top 3 精簡橫排）
                item {
                    when (topVolumeState) {
                        is Resource.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(3) { SkeletonLoader(modifier = Modifier.width(120.dp).height(80.dp)) }
                        }
                        is Resource.Error -> { }
                        is Resource.Success -> {
                            val topVolume = ((topVolumeState as Resource.Success).data ?: emptyList()).take(5)
                            if (topVolume.isNotEmpty()) {
                                Text("🔥 今日熱門交易", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(topVolume) { crop ->
                                        Box(modifier = Modifier.width(120.dp).liquidGlass().padding(12.dp)) {
                                            Column {
                                                Text(crop.cropName, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                                Text("$${String.format("%.1f", crop.avgPrice)}/kg", color = Color(0xFF388E3C), style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 🎤 長輩語音搜尋按鈕（結果直接填入搜尋欄）
                item {
                    VoiceSearchButton(
                        onResult = { keyword ->
                            searchKeyword = keyword
                        },
                        modifier = Modifier
                    )
                }

                // 3.5. 🔍 文字搜尋欄（與語音搜尋共享同一關鍵字狀態）
                item {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜尋農產品名稱...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "搜尋", tint = Color(0xFF4CAF50))
                        },
                        trailingIcon = {
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除搜尋")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFA5D6A7)
                        )
                    )
                }

                // 4. 📋 今日菜價列表
                item {
                    if (searchKeyword.isBlank()) {
                        Text("📋 今日菜價", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                    } else {
                        Text("🔍 搜尋結果：「$searchKeyword」", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                    }
                }

                when (dailyPricesState) {
                    is Resource.Loading -> {
                        items(5) {
                            SkeletonLoader(modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 4.dp))
                        }
                    }
                    is Resource.Error -> {
                        item {
                            ErrorView(
                                message = (dailyPricesState as Resource.Error).message ?: "載入失敗",
                                onRetry = { viewModel.fetchDashboardData() }
                            )
                        }
                    }
                    is Resource.Success -> {
                        val allPrices = (dailyPricesState as Resource.Success).data ?: emptyList()
                        // 依搜尋關鍵字過濾，空字串則顯示全部
                        val dailyPrices = if (searchKeyword.isBlank()) allPrices
                            else allPrices.filter { it.cropName.contains(searchKeyword, ignoreCase = true) }
                        if (dailyPrices.isEmpty()) {
                            item {
                                if (searchKeyword.isNotBlank())
                                    EmptyStateView(message = "找不到「$searchKeyword」的菜價資料", icon = Icons.Default.Search)
                                else
                                    EmptyStateView(message = "今日暫無菜價資料", icon = Icons.Default.ShoppingCart)
                            }
                        } else {
                            items(dailyPrices) { produce ->
                                val isHighlighted = produce.produceId == highlightedProduceId
                                Box(modifier = Modifier.fillMaxWidth()
                                    .then(if (isHighlighted) Modifier.border(2.dp, Color(0xFFFF6F00), RoundedCornerShape(16.dp)) else Modifier)
                                    .liquidGlass().padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(produce.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                            Text(produce.marketName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("$${String.format("%.1f", produce.avgPrice)}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            // 語音播報按鈕
                                            IconButton(onClick = { ttsHelper.speak("今日 ${produce.cropName} 價格是 ${produce.avgPrice} 元") }) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = "語音播報", tint = Color(0xFF388E3C))
                                            }
                                            // ❤️ 加入收藏按鈕
                                            IconButton(onClick = {
                                                favoriteTarget = Pair(produce.cropCode, produce.cropName)
                                                targetPriceInput = String.format("%.1f", produce.avgPrice)
                                            }) {
                                                Icon(Icons.Default.FavoriteBorder, contentDescription = "加入收藏", tint = Color(0xFFE91E63))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. ⛈️ 天氣預警（真實 API，無警報時自動隱藏）
                item {
                    WeatherAlertCard(viewModel = viewModel)
                }

                // 6. 🍳 省錢食譜推薦（真實 API）
                item {
                    BudgetRecipeGenerator(viewModel = viewModel)
                }

                // 7. 📈 高麗菜歷史價格趨勢圖
                item {
                    if (historicalDataState is Resource.Success && predictedDataState is Resource.Success) {
                        val historical = (historicalDataState as Resource.Success).data ?: emptyList()
                        val predicted = (predictedDataState as Resource.Success).data ?: emptyList()
                        if (historical.isNotEmpty()) {
                            Text("📈 高麗菜 價格趨勢與預測", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).liquidGlass().padding(16.dp)) {
                                PriceTrendChart(historical = historical, predicted = predicted)
                            }
                        }
                    } else if (historicalDataState is Resource.Loading) {
                        SkeletonLoader(modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }

                // 8. 🌱 當季盛產日曆
                item {
                    Text("🌱 當季盛產推薦", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(8.dp))
                    SeasonalCropCalendar()
                }

                // 9. 📍 尋找最近的果菜市場 + 各市場比價
                item {
                    MarketFinderCard(viewModel = viewModel)
                }

                // 10. 🛒 智慧買菜清單入口
                item {
                    Button(
                        onClick = onNavigateToGroceryList,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text("🛒 打開智慧買菜清單", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
