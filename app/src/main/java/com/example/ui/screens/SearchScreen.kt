package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogItem
import com.example.ui.components.CardItem
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldGradient
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    filterMode: String, // "all", "live", "vod", "series"
    searchResults: Map<String, List<CatalogItem>>,
    categoryNames: Map<String, Map<String, String>>,
    onFilterChange: (mode: String) -> Unit,
    onItemClick: (item: CatalogItem) -> Unit
) {
    val liveResults = searchResults["live"] ?: emptyList()
    val vodResults = searchResults["vod"] ?: emptyList()
    val seriesResults = searchResults["series"] ?: emptyList()
    val totalCount = liveResults.size + vodResults.size + seriesResults.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen"),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 20.dp)
    ) {
        // Título de la búsqueda
        item {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "Resultados para ",
                    color = TextPrimary,
                    fontSize = 22.sp
                )
                Text(
                    text = "“$searchQuery”",
                    color = GoldLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chips de filtro ("Todo", "TV en vivo", "Películas", "Series")
        item {
            Row(
                modifier = Modifier.padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchChip(
                    label = "Todo",
                    count = totalCount,
                    isSelected = filterMode == "all",
                    onClick = { onFilterChange("all") }
                )
                SearchChip(
                    label = "TV en vivo",
                    count = liveResults.size,
                    isSelected = filterMode == "live",
                    onClick = { onFilterChange("live") }
                )
                SearchChip(
                    label = "Películas",
                    count = vodResults.size,
                    isSelected = filterMode == "vod",
                    onClick = { onFilterChange("vod") }
                )
                SearchChip(
                    label = "Series",
                    count = seriesResults.size,
                    isSelected = filterMode == "series",
                    onClick = { onFilterChange("series") }
                )
            }
        }

        if (totalCount == 0) {
            item {
                Text(
                    text = "No encontré nada con esa búsqueda.",
                    color = TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        } else {
            // Sección TV en vivo
            if ((filterMode == "all" || filterMode == "live") && liveResults.isNotEmpty()) {
                item {
                    SearchSectionHeader(title = "TV en vivo", count = liveResults.size)
                }
                val chunkedLive = liveResults.chunked(5)
                items(chunkedLive) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (it in rowItems) {
                            val catName = categoryNames["live"]?.get(it.cid)
                            CardItem(
                                item = it,
                                categoryName = catName,
                                onClick = { onItemClick(it) }
                            )
                        }
                    }
                }
            }

            // Sección Películas
            if ((filterMode == "all" || filterMode == "vod") && vodResults.isNotEmpty()) {
                item {
                    SearchSectionHeader(title = "Películas", count = vodResults.size)
                }
                val chunkedVod = vodResults.chunked(4)
                items(chunkedVod) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (it in rowItems) {
                            val catName = categoryNames["vod"]?.get(it.cid)
                            CardItem(
                                item = it,
                                categoryName = catName,
                                onClick = { onItemClick(it) }
                            )
                        }
                    }
                }
            }

            // Sección Series
            if ((filterMode == "all" || filterMode == "series") && seriesResults.isNotEmpty()) {
                item {
                    SearchSectionHeader(title = "Series", count = seriesResults.size)
                }
                val chunkedSeries = seriesResults.chunked(4)
                items(chunkedSeries) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (it in rowItems) {
                            val catName = categoryNames["series"]?.get(it.cid)
                            CardItem(
                                item = it,
                                categoryName = catName,
                                onClick = { onItemClick(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, if (isSelected) GoldPrimary else BorderDark, RoundedCornerShape(26.dp))
            .background(if (isSelected) GoldGradient else androidx.compose.ui.graphics.SolidColor(PanelDark))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .testTag("search_chip_$label"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (isSelected) BackgroundDark else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                color = if (isSelected) BackgroundDark.copy(alpha = 0.8f) else TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = GoldLight,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}
