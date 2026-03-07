package com.example.produceapp.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// SettingsScreen.kt — 設定頁面
//
// 功能：
//   1. 推播通知設定：跳轉系統通知設定（開啟/關閉 FCM 通知）
//   2. 離線快取狀態：顯示上次同步時間，點擊強制重新整理
//   3. 關於 App：版本資訊對話框（版本號、授權、GitHub 連結）
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ProduceViewModel? = null) {
    val context = LocalContext.current

    // 上次同步時間（以 viewModel 最後載入時間為準）
    var lastSyncTime by remember {
        mutableStateOf(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
    }
    var isSyncing by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // 「關於 App」對話框
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF2E7D32))
            },
            title = { Text("關於蔬菜市場 App", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("版本：1.0.0", color = Color(0xFF388E3C))
                    Text("資料來源：行政院農業委員會農產品交易行情站", color = Color(0xFF388E3C))
                    Text("天氣資料：中央氣象署 RSS 即時資料", color = Color(0xFF388E3C))
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "本 App 提供台灣農產品批發價格查詢、天氣預警及省錢食譜推薦，" +
                        "協助消費者把握最佳採購時機，支援多語言服務外籍移工族群。",
                        color = Color(0xFF558B2F),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("關閉", color = Color(0xFF2E7D32))
                }
            },
            containerColor = Color(0xFFF1F8E9)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 推播通知設定 → 跳轉系統通知設定
            SettingItem(
                icon = Icons.Default.Notifications,
                title = "推播通知設定",
                subtitle = "管理價格異常與達標提醒",
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            // 2. 語言與語音（跳轉系統語言設定）
            SettingItem(
                icon = Icons.Default.Language,
                title = "語言與語音",
                subtitle = "切換系統語言（印尼語/越南語）",
                onClick = {
                    val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // 3. 離線快取狀態：顯示上次同步時間，點擊重新整理
            SettingItem(
                icon = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudSync,
                title = "離線快取狀態",
                subtitle = if (isSyncing) "同步中..." else "上次同步時間：$lastSyncTime",
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        viewModel?.let { vm ->
                            vm.fetchDashboardData()
                            lastSyncTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        }
                        isSyncing = false
                    }
                }
            )

            // 4. 關於 App
            SettingItem(
                icon = Icons.Default.Info,
                title = "關於 App",
                subtitle = "版本 1.0.0 · 農委會資料",
                onClick = { showAboutDialog = true }
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass()
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF388E3C)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF81C784),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
