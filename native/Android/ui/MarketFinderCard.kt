package com.example.produceapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarketFinderCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFFFFF3E0) // 溫暖的淺橘色背景
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("找尋最近的果菜市場", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6)
                Text("開啟地圖查看附近的傳統市場與批發市場", style = MaterialTheme.typography.body2, color = Color.Gray)
            }
            Button(
                onClick = {
                    // 核心邏輯：使用 Google Maps Intent 搜尋附近的果菜市場
                    // 這樣做的好處是不用自己刻地圖，還能直接看營業時間與導航！
                    val gmmIntentUri = Uri.parse("geo:0,0?q=果菜市場 傳統市場")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    
                    // 檢查手機是否有安裝 Google Maps
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        // 如果沒有安裝，退回使用網頁版 Google Maps
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/果菜市場"))
                        context.startActivity(webIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800), contentColor = Color.White),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Place, contentDescription = "地圖")
                Spacer(modifier = Modifier.width(4.dp))
                Text("導航")
            }
        }
    }
}
