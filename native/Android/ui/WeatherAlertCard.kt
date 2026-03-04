package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeatherAlertCard(
    modifier: Modifier = Modifier,
    alertTitle: String = "⚠️ 颱風即將登陸！",
    alertMessage: String = "根據 AI 預測，葉菜類明日可能上漲 30%，建議今日提早採買高麗菜、青江菜！"
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFFFFEBEE) // 淺紅色背景，警告意味
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "警告",
                tint = Color(0xFFD32F2F), // 深紅色
                modifier = Modifier.size(32.dp).padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = alertTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alertMessage,
                    style = MaterialTheme.typography.body2,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
