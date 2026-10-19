package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BottomBarDark
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.TextMuted

@Composable
fun BottomBar(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(BottomBarDark)
            .border(width = 1.dp, color = PanelAltDark)
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag("status_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = statusText.ifEmpty { "Listo." },
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
