package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.produceapp.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: ProduceViewModel) {
    val favoritesState by viewModel.favorites.collectAsState()
    
    // 漸層背景，與 HomeScreen 保持一致的 Liquid Glass 風格
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏與追蹤", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
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
                            items(favorites) { item ->
                                // 複用 HomeScreen.kt 中定義的 liquidGlass() Modifier
                                Box(modifier = Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = item.produceName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "目前價格: $${item.currentPrice}/kg",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF388E3C)
                                            )
                                            Text(
                                                text = "目標提醒: $${item.targetPrice}/kg",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF558B2F)
                                            )
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (item.isAlertTriggered) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "已達標",
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "已達標！",
                                                    color = Color(0xFFE65100),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsNone,
                                                    contentDescription = "追蹤中",
                                                    tint = Color(0xFF81C784),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "追蹤中",
                                                    color = Color(0xFF81C784),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
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
