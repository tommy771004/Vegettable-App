package com.example.produceapp.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// =============================================================================
// SmartGroceryListScreen.kt — 智慧買菜清單
//
// 功能：
//   - 輸入蔬菜名稱後自動查詢今日批發均價（供預估費用）
//   - 每筆品項支援 ±數量、勾選已買、左滑刪除
//   - 即時計算「待購金額」（排除已勾選）
//   - 清單持久化：存入 SharedPreferences（JSON），重開 App 不遺失
//   - 「清除已買」一鍵清除所有已勾選品項
// =============================================================================

data class GroceryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var quantity: Int = 1,
    val estimatedPricePerUnit: Double = 0.0,
    var isChecked: Boolean = false
)

// MARK: - SharedPreferences 持久化輔助函式
private const val PREF_NAME = "grocery_list"
private const val PREF_KEY  = "items_json"

private fun saveItems(context: Context, items: List<GroceryItem>) {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("quantity", item.quantity)
            put("estimatedPricePerUnit", item.estimatedPricePerUnit)
            put("isChecked", item.isChecked)
        })
    }
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREF_KEY, arr.toString()).apply()
}

private fun loadItems(context: Context): List<GroceryItem> {
    val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(PREF_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GroceryItem(
                id = obj.getString("id"),
                name = obj.getString("name"),
                quantity = obj.getInt("quantity"),
                estimatedPricePerUnit = obj.getDouble("estimatedPricePerUnit"),
                isChecked = obj.getBoolean("isChecked")
            )
        }
    } catch (_: Exception) { emptyList() }
}

// MARK: - 主畫面

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGroceryListScreen(viewModel: ProduceViewModel? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 持久化：從 SharedPreferences 初始化
    var items by remember { mutableStateOf(loadItems(context)) }
    var newItemName by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }

    // 每次 items 變動時自動儲存
    LaunchedEffect(items) { saveItems(context, items) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarMsg = null
        }
    }

    val pendingItems = items.filter { !it.isChecked }
    val checkedItems = items.filter { it.isChecked }
    val totalCost = pendingItems.sumOf { it.quantity * it.estimatedPricePerUnit }
    val animatedTotal by animateFloatAsState(
        targetValue = totalCost.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "totalCost"
    )
    val bgBrush = Brush.linearGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))

    fun addItem(name: String) {
        if (name.isBlank()) return
        isSearching = true
        scope.launch {
            val price = viewModel?.searchCropPrice(name) ?: 0.0
            if (price == null) snackbarMsg = "找不到「$name」的價格，已設為 0"
            items = items + GroceryItem(name = name.trim(), estimatedPricePerUnit = price ?: 0.0)
            newItemName = ""
            isSearching = false
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智慧買菜清單", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                actions = {
                    if (checkedItems.isNotEmpty()) {
                        TextButton(
                            onClick = { items = items.filter { !it.isChecked } }
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清除已買 (${checkedItems.size})", color = Color(0xFF1976D2), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Hero Banner：預估總花費
                GroceryHeroBanner(
                    totalCost = animatedTotal.toDouble(),
                    pendingCount = pendingItems.size,
                    checkedCount = checkedItems.size
                )

                // 輸入欄
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("想買什麼菜？") },
                        placeholder = { Text("例：高麗菜、番茄") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { addItem(newItemName) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { addItem(newItemName) },
                        enabled = newItemName.isNotBlank() && !isSearching,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "新增", tint = Color.White)
                        }
                    }
                }

                // 清單列表
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            message = "清單是空的！\n輸入蔬菜名稱自動查詢今日均價",
                            icon = Icons.Default.ShoppingCart
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 待購品項
                        if (pendingItems.isNotEmpty()) {
                            item {
                                Text(
                                    "待購（${pendingItems.size} 項）",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF558B2F),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(pendingItems, key = { it.id }) { item ->
                                GroceryItemCard(
                                    item = item,
                                    onToggle = {
                                        items = items.map { if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it }
                                    },
                                    onQtyChange = { delta ->
                                        items = items.map {
                                            if (it.id == item.id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(1)) else it
                                        }
                                    },
                                    onDelete = { items = items.filter { it.id != item.id } }
                                )
                            }
                        }

                        // 已購品項
                        if (checkedItems.isNotEmpty()) {
                            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                            item {
                                Text(
                                    "已購（${checkedItems.size} 項）",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(checkedItems, key = { it.id }) { item ->
                                GroceryItemCard(
                                    item = item,
                                    onToggle = {
                                        items = items.map { if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it }
                                    },
                                    onQtyChange = { delta ->
                                        items = items.map {
                                            if (it.id == item.id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(1)) else it
                                        }
                                    },
                                    onDelete = { items = items.filter { it.id != item.id } }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// MARK: - Hero Banner（預估總花費）

@Composable
private fun GroceryHeroBanner(totalCost: Double, pendingCount: Int, checkedCount: Int) {
    val totalItems = pendingCount + checkedCount
    val progress = if (totalItems > 0) checkedCount.toFloat() / totalItems else 0f
    val animatedProgress by animateFloatAsState(progress, tween(600), label = "progress")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("預估待購金額", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text(
                        "${"%.0f".format(totalCost)} 元",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    // 購買進度圓形
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFF81C784),
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 5.dp
                        )
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "已買 $checkedCount/$totalItems",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (totalItems > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF81C784),
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

// MARK: - 單一品項卡片（支援 SwipeToDismiss 刪除）

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryItemCard(
    item: GroceryItem,
    onToggle: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD32F2F).copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 勾選框
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF4CAF50),
                        uncheckedColor = Color(0xFF81C784)
                    )
                )

                // 品項資訊
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isChecked) Color.Gray else Color(0xFF1B5E20),
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (item.estimatedPricePerUnit > 0) {
                        Text(
                            text = "均價 ${"%.1f".format(item.estimatedPricePerUnit)}/kg × ${item.quantity} = ${"%.0f".format(item.estimatedPricePerUnit * item.quantity)} 元",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isChecked) Color.LightGray else Color(0xFF558B2F)
                        )
                    } else {
                        Text(
                            text = "×${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // 數量調整 ± 按鈕（已勾選項目隱藏）
                AnimatedVisibility(visible = !item.isChecked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 減少
                        SmallIconButton(
                            onClick = { onQtyChange(-1) },
                            enabled = item.quantity > 1,
                            icon = Icons.Default.Remove
                        )
                        Text(
                            "${item.quantity}",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // 增加
                        SmallIconButton(
                            onClick = { onQtyChange(1) },
                            icon = Icons.Default.Add
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color(0xFFDCEDC8),
            contentColor = Color(0xFF2E7D32)
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}
