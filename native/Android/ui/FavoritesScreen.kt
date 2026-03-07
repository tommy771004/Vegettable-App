package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.produceapp.data.FavoriteAlertDto
import com.example.produceapp.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: ProduceViewModel) {
    val favoritesState by viewModel.favorites.collectAsState()
    val scope = rememberCoroutineScope()

    // 修改目標提醒價格的狀態
    var editTarget by remember { mutableStateOf<FavoriteAlertDto?>(null) }
    var editPriceInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar 顯示
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarMessage = null
        }
    }

    // 漸層背景，與 HomeScreen 保持一致的 Liquid Glass 風格
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
    )

    // 修改目標提醒價格的 AlertDialog
    editTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            icon = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
            },
            title = {
                Text(
                    "修改目標提醒價格",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "農產品：${item.produceName}",
                        color = Color(0xFF388E3C),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "目前市場均價：${"%.1f".format(item.currentPrice)}/kg",
                        color = Color(0xFF558B2F),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editPriceInput,
                        onValueChange = { editPriceInput = it },
                        label = { Text("目標提醒價格（元/公斤）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color(0xFF1976D2)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val price = editPriceInput.toDoubleOrNull()
                        if (price != null && price > 0) {
                            val produceId = item.produceId
                            scope.launch {
                                val ok = viewModel.updateFavoriteTargetPrice(produceId, price)
                                snackbarMessage = if (ok) "✅ ${item.produceName} 目標提醒已更新" else "⚠️ 更新失敗，請稍後再試"
                            }
                            editTarget = null
                        }
                    },
                    enabled = editPriceInput.toDoubleOrNull() != null && (editPriceInput.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("確認更新", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("取消", color = Color.Gray)
                }
            },
            containerColor = Color(0xFFF1F8E9)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏與追蹤", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            when (favoritesState) {
                is Resource.Loading -> {
                    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                        repeat(5) {
                            SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp))
                        }
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        ErrorView(
                            message = (favoritesState as Resource.Error).message ?: "Failed to load favorites",
                            onRetry = { viewModel.fetchDashboardData() }
                        )
                    }
                }
                is Resource.Success -> {
                    val favorites = (favoritesState as Resource.Success).data ?: emptyList()

                    if (favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            EmptyStateView(
                                message = "還沒有收藏嗎？試試看搜尋高麗菜",
                                icon = Icons.Default.FavoriteBorder
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(favorites, key = { it.produceId }) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            scope.launch { viewModel.removeFavorite(item.produceId) }
                                            true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        // 紅色刪除背景（向左滑時顯示）
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .liquidGlass()
                                                .background(Color(0xFFD32F2F).copy(alpha = 0.85f))
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.White, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.produceName,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(text = "目前價格: ${"%.1f".format(item.currentPrice)}/kg", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF388E3C))
                                                Text(text = "目標提醒: ${"%.1f".format(item.targetPrice)}/kg", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF558B2F))
                                            }
                                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // 修改目標提醒按鈕
                                                IconButton(
                                                    onClick = {
                                                        editTarget = item
                                                        editPriceInput = "%.1f".format(item.targetPrice)
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "修改提醒",
                                                        tint = Color(0xFF1976D2),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                // 通知狀態圖示
                                                if (item.isAlertTriggered) {
                                                    Icon(Icons.Default.NotificationsActive, contentDescription = "已達標", tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                                                    Text("已達標！", color = Color(0xFFE65100), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Icon(Icons.Default.NotificationsNone, contentDescription = "追蹤中", tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                                                    Text("追蹤中", color = Color(0xFF81C784), style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
