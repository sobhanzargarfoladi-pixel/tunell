package com.foxyvpn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FoxyGradient = Brush.linearGradient(listOf(Color(0xFFFFB65A), Color(0xFFFF6B5E), Color(0xFFA66CFF), Color(0xFF59D7FF)))

@Composable
fun FoxyWordmark(modifier: Modifier = Modifier, size: TextUnit = 32.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Tunnela",
            style = TextStyle(
                brush = FoxyGradient,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = size,
                letterSpacing = (-0.8).sp,
            ),
        )
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF8C6CFF), Color(0xFF55CFFF))))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(
                "VIP",
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.36f).sp,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

fun Modifier.foxyBackground(): Modifier = this

@Composable
fun FoxyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}
