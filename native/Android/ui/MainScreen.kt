package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.produceapp.util.TextToSpeechHelper

enum class BottomTab(val title: String, val icon: ImageVector) {
    HOME("首頁", Icons.Default.Home),
    FAVORITES("收藏", Icons.Default.Favorite),
    SETTINGS("設定", Icons.Default.Settings),
    GROCERY_LIST("清單", Icons.Default.ShoppingCart)
}

@Composable
fun MainScreen(
    viewModel: ProduceViewModel,
    ttsHelper: TextToSpeechHelper,
    deepLinkProduceId: String? = null
) {
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    val context = LocalContext.current
    
    // 全站漸層底色 (iOS 26 Liquid Glass 風格)
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        // 主要內容區域
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
            when (currentTab) {
                BottomTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    ttsHelper = ttsHelper,
                    onNavigateToGroceryList = { currentTab = BottomTab.GROCERY_LIST },
                    onNavigateToElderlyMode = {
                        val intent = Intent(context, ElderlyModeActivity::class.java)
                        context.startActivity(intent)
                    },
                    highlightedProduceId = deepLinkProduceId
                )
                BottomTab.FAVORITES -> FavoritesScreen(viewModel)
                BottomTab.SETTINGS -> SettingsScreen()
                BottomTab.GROCERY_LIST -> SmartGroceryListScreen()
            }
        }

        // 懸浮式底部導覽列 (Floating Liquid Glass Bottom Navigation)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .liquidGlass()
                .padding(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF81C784)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { currentTab = tab }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = color,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.title,
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
