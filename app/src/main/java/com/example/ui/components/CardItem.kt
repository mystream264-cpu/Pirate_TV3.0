package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CatalogItem
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.RottenGreen
import com.example.ui.theme.RottenRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WatchedGreen

@Composable
fun CardItem(
    item: CatalogItem,
    categoryName: String? = null,
    ratingText: String? = null,
    rottenTomatoes: String? = null,
    isWatched: Boolean = false,
    progressPercent: Int = 0,
    onClick: () -> Unit
) {
    val isLive = item.mode == "live"
    val cardWidth = if (isLive) 156.dp else 176.dp
    val posterHeight = if (isLive) 104.dp else 264.dp

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
            .testTag("card_${item.sid}")
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(posterHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isLive) PanelDark else PanelAltDark)
        ) {
            if (item.img.isNotBlank()) {
                AsyncImage(
                    model = item.img,
                    contentDescription = item.name,
                    contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name,
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Calificación por estrellas (VOD y Series)
            if (!isLive && item.rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xC7000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ " + String.format("%.1f", item.rating),
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rotten Tomatoes badge
            if (!isLive && !rottenTomatoes.isNullOrBlank()) {
                val score = rottenTomatoes.replace("%", "").trim().toIntOrNull() ?: 100
                val rtBg = if (score < 60) RottenGreen else RottenRed
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 10.dp)
                        .background(rtBg, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "RT $rottenTomatoes",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Distintivo de Vista (VOD)
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(WatchedGreen, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "✓",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (progressPercent > 0) {
                // Barra de progreso de reproducción
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0x4DFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(progressPercent / 100f)
                            .background(GoldPrimary)
                    )
                }
            }
        }

        // Título del elemento
        Text(
            text = item.name,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )

        // Categoría si está disponible
        if (!categoryName.isNullOrBlank()) {
            Text(
                text = categoryName,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ContinueCardItem(
    title: String,
    subtitle: String,
    imageUrl: String,
    progressPercent: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .clickable(onClick = onClick)
            .testTag("continue_card")
    ) {
        Box(
            modifier = Modifier
                .width(176.dp)
                .height(264.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PanelAltDark)
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Botón de eliminar de continuar viendo (rm)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000))
                    .border(1.dp, TextMuted, RoundedCornerShape(14.dp))
                    .clickable { onRemove() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar de la lista",
                    tint = TextSecondary,
                    modifier = Modifier.width(16.dp).height(16.dp)
                )
            }

            // Indicador de reproducción central sutil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x22000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.width(42.dp).height(42.dp)
                )
            }

            // Barra de progreso
            if (progressPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0x4DFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(progressPercent / 100f)
                            .background(GoldPrimary)
                    )
                }
            }
        }

        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )

        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
