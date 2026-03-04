package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BudgetRecipe(
    val title: String,
    val reason: String,
    val icon: String,
    val ingredients: String
)

@Composable
fun BudgetRecipeGenerator(modifier: Modifier = Modifier) {
    // 模擬從後端 GET /api/produce/budget-recipes 取得的資料
    val recipes = listOf(
        BudgetRecipe("番茄炒蛋", "今日番茄大跌價！", "🍅", "番茄、雞蛋、蔥"),
        BudgetRecipe("蒜炒高麗菜", "高麗菜價格平穩", "🥬", "高麗菜、蒜頭"),
        BudgetRecipe("蘿蔔排骨湯", "當季蘿蔔最便宜", "🍲", "白蘿蔔、排骨")
    )

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "🍳 今天吃什麼？省錢食譜推薦",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(recipes) { recipe ->
                Card(
                    modifier = Modifier.width(200.dp).height(140.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFFFFF8E1) // 淺黃色背景，溫暖感
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = recipe.icon, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = recipe.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Column {
                            Text(
                                text = recipe.reason,
                                style = MaterialTheme.typography.caption,
                                color = Color(0xFFFF5722), // 橘紅色強調便宜
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "食材: ${recipe.ingredients}",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
