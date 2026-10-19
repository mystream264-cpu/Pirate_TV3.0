package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.AssetUtils
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onOpenSection: (mode: String) -> Unit
) {
    val liveExists = AssetUtils.rememberAssetExists("img_live.png")
    val vodExists = AssetUtils.rememberAssetExists("img_vod.png")
    val seriesExists = AssetUtils.rememberAssetExists("img_series.png")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("home_screen"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTile(
                title = "TV en vivo",
                mode = "live",
                assetName = "img_live.png",
                hasAsset = liveExists,
                icon = Icons.Default.LiveTv,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                onClick = { onOpenSection("live") }
            )

            HomeTile(
                title = "Películas",
                mode = "vod",
                assetName = "img_vod.png",
                hasAsset = vodExists,
                icon = Icons.Default.Movie,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                onClick = { onOpenSection("vod") }
            )

            HomeTile(
                title = "Series",
                mode = "series",
                assetName = "img_series.png",
                hasAsset = seriesExists,
                icon = Icons.Default.Tv,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                onClick = { onOpenSection("series") }
            )
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    mode: String,
    assetName: String,
    hasAsset: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("tile_$mode")
    ) {
        if (hasAsset) {
            AsyncImage(
                model = AssetUtils.assetUri(assetName),
                contentDescription = title,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Tarjeta de reemplazo elegante cuando aún no se han subido las imágenes
            val tileGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C343F),
                    Color(0xFF232A35)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(tileGradient)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    color = GoldLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
