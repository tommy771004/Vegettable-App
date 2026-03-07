package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.BudgetRecipeDto
import com.example.produceapp.util.Resource

@Composable
fun BudgetRecipeGenerator(
    viewModel: ProduceViewModel,
    modifier: Modifier = Modifier
) {
    val recipesState by viewModel.budgetRecipes.collectAsState()

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "🍳 今天吃什麼？省錢食譜推薦",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        when (recipesState) {
            is Resource.Loading -> {
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(140.dp))
            }
            is Resource.Error -> {
                Text(
                    text = "食譜載入失敗",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            is Resource.Success -> {
                val recipes = (recipesState as Resource.Success<List<BudgetRecipeDto>>).data
                    ?: emptyList()

                if (recipes.isEmpty()) {
                    Text(
                        text = "今日暫無省錢食譜推薦",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(recipes) { recipe ->
                            Card(
                                modifier = Modifier.width(200.dp).height(140.dp),
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = Color(0xFFFFF8E1)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = recipe.imageUrl, fontSize = 32.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = recipe.recipeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = recipe.reason,
                                            style = MaterialTheme.typography.caption,
                                            color = Color(0xFFFF5722),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "食材：${recipe.mainIngredients.joinToString("、")}",
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
        }
    }
}
