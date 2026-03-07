package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.UUID

data class GroceryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var quantity: Int,
    val estimatedPricePerUnit: Double,
    var isChecked: Boolean = false
)

@Composable
fun SmartGroceryListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProduceViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(emptyList<GroceryItem>()) }
    
    var newItemName by remember { mutableStateOf("") }
    var priceNotFoundMessage by remember { mutableStateOf<String?>(null) }

    val totalEstimatedCost = items.filter { !it.isChecked }.sumOf { it.quantity * it.estimatedPricePerUnit }

    val scaffoldState = rememberScaffoldState()
    LaunchedEffect(priceNotFoundMessage) {
        priceNotFoundMessage?.let {
            scaffoldState.snackbarHostState.showSnackbar(it)
            priceNotFoundMessage = null
        }
    }

    Scaffold(scaffoldState = scaffoldState) { innerPadding ->
    Column(modifier = modifier.fillMaxSize().padding(16.dp).padding(innerPadding)) {
        Text(
            text = "🛒 智慧買菜清單",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 新增物品區塊
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemName,
                onValueChange = { newItemName = it },
                label = { Text("想買什麼菜？") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newItemName.isNotBlank()) {
                        scope.launch {
                            val price = viewModel.searchCropPrice(newItemName)
                            if (price == null) {
                                priceNotFoundMessage = "找不到「$newItemName」的價格，已設為 0"
                            }
                            items = items + GroceryItem(
                                name = newItemName,
                                quantity = 1,
                                estimatedPricePerUnit = price ?: 0.0
                            )
                            newItemName = ""
                        }
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        }

        // 清單列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = if (item.isChecked) Color(0xFFF5F5F5) else Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { checked ->
                                items = items.map { if (it.id == item.id) it.copy(isChecked = checked) else it }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isChecked) Color.Gray else Color.Black
                            )
                            Text(
                                text = "預估單價: $${item.estimatedPricePerUnit}",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                            )
                        }
                        
                        // 數量調整
                        if (!item.isChecked) {
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { 
                                    val newQ = (it.toIntOrNull() ?: 1).coerceAtLeast(1)
                                    items = items.map { i -> if (i.id == item.id) i.copy(quantity = newQ) else i }
                                },
                                modifier = Modifier.width(60.dp).height(50.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            )
                        }

                        IconButton(onClick = {
                            items = items.filter { it.id != item.id }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // 總計區塊
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFFE8F5E9) // 淺綠色背景
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "預估總花費 (未買)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$${totalEstimatedCost}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color(0xFF2E7D32) // 深綠色
                )
            }
        }
    } // end Column
    } // end Scaffold
}
