package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.BudgetRecipeDto
import com.example.produceapp.util.Resource

// =============================================================================
// BudgetRecipeCard.kt — 省錢食譜推薦（Material 3 沉浸式版本）
//
// 更新說明：
//   原版使用 Material 2 Card，本版升級為：
//   - 漸層暖橙背景卡片，與首頁 LiquidGlass 風格統一
//   - 可展開的「料理步驟」區塊（點擊卡片展開/收合）
//   - 食材標籤 Chip 顯示，而非純文字列表
//   - 價格優惠理由以深色徽章方式醒目呈現
// =============================================================================

@Composable
fun BudgetRecipeGenerator(
    viewModel: ProduceViewModel,
    modifier: Modifier = Modifier
) {
    val recipesState by viewModel.budgetRecipes.collectAsState()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "🍳 今天吃什麼？省錢食譜推薦",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2E7D32)
        )

        when (recipesState) {
            is Resource.Loading -> {
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(160.dp))
            }
            is Resource.Error -> {
                Text(
                    text = "食譜載入失敗",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            is Resource.Success -> {
                val recipes = (recipesState as Resource.Success<List<BudgetRecipeDto>>).data ?: emptyList()
                if (recipes.isEmpty()) {
                    Text(text = "今日暫無省錢食譜推薦", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recipes) { recipe ->
                            RecipeCard(recipe = recipe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: BudgetRecipeDto) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 標題列：Emoji + 食譜名稱
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = recipe.imageUrl, fontSize = 36.sp)
                Text(
                    text = recipe.recipeName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color(0xFF4E342E),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // 優惠理由徽章
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF5722).copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = recipe.reason,
                    fontSize = 11.sp,
                    color = Color(0xFFBF360C),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 食材標籤
            if (recipe.mainIngredients.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    recipe.mainIngredients.take(3).forEach { ingredient ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ingredient,
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 展開/收合料理步驟
            if (recipe.steps.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (expanded) "▲ 收合步驟" else "▼ 查看步驟 (${recipe.steps.size}步)",
                        fontSize = 11.sp,
                        color = Color(0xFF795548)
                    )
                }
                if (expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipe.steps.forEachIndexed { idx, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5722)
                                )
                                Text(text = step, fontSize = 11.sp, color = Color(0xFF5D4037), lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
