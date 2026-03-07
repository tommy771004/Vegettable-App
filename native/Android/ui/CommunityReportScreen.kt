package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// =============================================================================
// CommunityReportScreen.kt — 社群零售價回報畫面
//
// 功能說明：
//   讓使用者回報在超市、傳統市場親眼看到的農產品零售價。
//   每次成功回報後端累積 5 點貢獻點數，可在「探索 → 我的貢獻」中查看等級。
//
// 對應後端端點：POST /api/produce/community-price
// 對應 iOS 版本：CommunityReportView.swift
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityReportScreen(viewModel: ProduceViewModel) {
    var marketName by remember { mutableStateOf("") }
    var produceName by remember { mutableStateOf("") }
    var retailPrice by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 使用者統計狀態
    var userStats by remember { mutableStateOf<com.example.produceapp.data.UserStatsDto?>(null) }

    val scope = rememberCoroutineScope()
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
    )

    // 載入使用者統計
    LaunchedEffect(Unit) {
        userStats = viewModel.getUserStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社群物價回報", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 使用者貢獻統計卡片
                userStats?.let { stats ->
                    Box(modifier = Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "等級",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stats.level,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "累積 ${stats.contributionPoints} 點 · 已回報 ${stats.reportCount} 次",
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // 說明文字
                Box(modifier = Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📢 回報說明", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text(
                            "您在超市或傳統市場親眼看到的零售價，\n" +
                            "回報後可幫助社群了解批發/零售價差。\n" +
                            "每次成功回報獲得 5 點貢獻點數 🎯",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF558B2F)
                        )
                    }
                }

                // 表單
                OutlinedTextField(
                    value = marketName,
                    onValueChange = { marketName = it },
                    label = { Text("市場名稱") },
                    placeholder = { Text("例：全聯信義店、板橋湳興市場") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFFA5D6A7)
                    )
                )

                OutlinedTextField(
                    value = produceName,
                    onValueChange = { produceName = it },
                    label = { Text("農產品名稱") },
                    placeholder = { Text("例：高麗菜、番茄、青椒") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFFA5D6A7)
                    )
                )

                OutlinedTextField(
                    value = retailPrice,
                    onValueChange = { retailPrice = it },
                    label = { Text("零售價 (元/公斤)") },
                    placeholder = { Text("例：35.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFFA5D6A7)
                    )
                )

                // 錯誤訊息
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 送出按鈕
                val isFormValid = marketName.isNotBlank() &&
                    produceName.isNotBlank() &&
                    retailPrice.toDoubleOrNull() != null
                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorMessage = null
                            val price = retailPrice.toDoubleOrNull()
                            if (price == null) {
                                errorMessage = "請輸入有效的價格數字"
                                isSubmitting = false
                                return@launch
                            }
                            val success = viewModel.submitCommunityPrice(
                                marketName = marketName,
                                cropName = produceName,
                                retailPrice = price
                            )
                            if (success) {
                                submitSuccess = true
                                marketName = ""
                                produceName = ""
                                retailPrice = ""
                                // 重新載入統計
                                userStats = viewModel.getUserStats()
                            } else {
                                errorMessage = "回報失敗，請確認網路連線後再試"
                            }
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = isFormValid && !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("送出回報", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 成功提示
                if (submitSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "成功",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "感謝回報！已獲得 5 點貢獻點數 🎉",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    "您的回報幫助社群了解零售與批發的價差",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF558B2F)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
