package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
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
            SettingItem(
                icon = Icons.Default.Notifications,
                title = "推播通知設定",
                subtitle = "管理價格異常與達標提醒"
            )
            SettingItem(
                icon = Icons.Default.Language,
                title = "語言與語音",
                subtitle = "切換印尼語/越南語及語音速度"
            )
            SettingItem(
                icon = Icons.Default.CloudSync,
                title = "離線快取狀態",
                subtitle = "上次同步時間: 剛剛"
            )
            SettingItem(
                icon = Icons.Default.Info,
                title = "關於 App",
                subtitle = "版本 1.0.0 (iOS 26 Liquid Glass Style)"
            )
        }
    }
}

@Composable
fun SettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
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
        }
    }
}
