package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CatalogItem
import com.example.data.model.ContinueWatchingItem
import com.example.data.model.PlaybackProgress
import com.example.data.model.XtreamCategory
import com.example.data.repository.cleanTitle
import com.example.ui.components.CardItem
import com.example.ui.components.ContinueCardItem
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
fun SectionScreen(
    mode: String, // "live", "vod", "series"
    categories: List<XtreamCategory>,
    selectedCategoryId: String?, // null = Todas
    isCategoriesOpen: Boolean,
    categoryCounts: Map<String, Int>,
    totalCount: Int,
    heroItem: CatalogItem?,
    continueList: List<ContinueWatchingItem>,
    catalogItems: List<CatalogItem>,
    categoryNames: Map<String, String>,
    progressMap: Map<String, PlaybackProgress>,
    isLoading: Boolean,
    onSelectCategory: (categoryId: String?) -> Unit,
    onCloseCategories: () -> Unit,
    onItemClick: (item: CatalogItem) -> Unit,
    onContinueClick: (entry: ContinueWatchingItem) -> Unit,
    onRemoveContinue: (entry: ContinueWatchingItem) -> Unit,
    onOpenHero: (item: CatalogItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("section_screen")
    ) {
        if (isLoading && catalogItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val modeLabel = if (mode == "live") "TV en vivo" else if (mode == "vod") "películas" else "series"
                Text(
                    text = "Cargando $modeLabel... (la primera vez puede tardar un poco)",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                // HERO BANNER PARA VOD Y SERIES
                if (mode != "live" && heroItem != null) {
                    item {
                        HeroBanner(
                            item = heroItem,
                            onPlay = { onOpenHero(heroItem) },
                            onMoreInfo = { onOpenHero(heroItem) }
                        )
                    }
                }

                // CONTINUAR VIENDO (Para VOD y Series)
                if (mode != "live" && continueList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)) {
                            Text(
                                text = "Continuar viendo",
                                color = GoldLight,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 40.dp, bottom = 12.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 40.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(continueList, key = { it.key }) { cItem ->
                                    val prog = cItem.progress
                                    val isSeries = cItem.entry.kind == "series"
                                    val subtitle = if (isSeries) {
                                        (cItem.entry.label ?: "") + if (prog?.isWatched == true) " · vista" else ""
                                    } else {
                                        if (prog != null && prog.d > 0) {
                                            val remainMin = ((prog.d - prog.t) / 60).coerceAtLeast(1)
                                            "Quedan $remainMin min"
                                        } else {
                                            formatTime(prog?.t ?: 0)
                                        }
                                    }

                                    ContinueCardItem(
                                        title = cItem.entry.name,
                                        subtitle = subtitle,
                                        imageUrl = cItem.entry.img,
                                        progressPercent = prog?.percent ?: 0,
                                        onClick = { onContinueClick(cItem) },
                                        onRemove = { onRemoveContinue(cItem) }
                                    )
                                }
                            }
                        }
                    }
                }

                // TÍTULO DE LA SECCIÓN / CATEGORÍA ACTUAL
                item {
                    val currentCatName = if (selectedCategoryId == null) {
                        "Todas las categorías"
                    } else {
                        categoryNames[selectedCategoryId] ?: "Categoría"
                    }

                    Row(
                        modifier = Modifier.padding(start = 40.dp, top = 20.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentCatName,
                            color = GoldLight,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = catalogItems.size.toString(),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                // GRID DE ITEMS
                if (catalogItems.isEmpty()) {
                    item {
                        Text(
                            text = "Sin resultados.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 40.dp, top = 10.dp)
                        )
                    }
                } else {
                    // Dividimos en filas de tarjetas para LazyColumn vertical fluido
                    val isLive = mode == "live"
                    val itemsPerRow = if (isLive) 5 else 4
                    val chunkedItems: List<List<CatalogItem>> = catalogItems.chunked(itemsPerRow)

                    items(chunkedItems) { rowItems: List<CatalogItem> ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            for (item in rowItems) {
                                val prog = progressMap["m${item.sid}"] ?: progressMap["s${item.sid}"]
                                CardItem(
                                    item = item,
                                    categoryName = categoryNames[item.cid],
                                    isWatched = prog?.isWatched == true,
                                    progressPercent = prog?.percent ?: 0,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // TELÓN OSCURO DEL CAJÓN LATERAL DE CATEGORÍAS (sideBg)
        if (isCategoriesOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable { onCloseCategories() }
            )
        }

        // CAJÓN LATERAL DE CATEGORÍAS (side)
        AnimatedVisibility(
            visible = isCategoriesOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(Color(0xFA10141A))
                    .border(width = 1.dp, color = BorderDark)
                    .padding(top = 10.dp, bottom = 10.dp)
                    .testTag("categories_drawer")
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Opción "Todas"
                    item {
                        val isAll = selectedCategoryId == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isAll) PanelAltDark else Color.Transparent)
                                .clickable {
                                    onSelectCategory(null)
                                    onCloseCategories()
                                }
                                .padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Todas",
                                color = if (isAll) Color.White else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = totalCount.toString(),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Categorías individuales
                    items(categories, key = { it.idString }) { cat ->
                        val isSelected = selectedCategoryId == cat.idString
                        val count = categoryCounts[cat.idString] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) PanelAltDark else Color.Transparent)
                                .clickable {
                                    onSelectCategory(cat.idString)
                                    onCloseCategories()
                                }
                                .padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.nameString,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = count.toString(),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    item: CatalogItem,
    onPlay: () -> Unit,
    onMoreInfo: () -> Unit
) {
    val clean = remember(item.name) { cleanTitle(item.name) }
    val plotText = if (item.plot.length > 220) item.plot.take(220) + "..." else item.plot

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(PanelAltDark)
            .testTag("hero_banner")
    ) {
        if (item.bg.isNotBlank()) {
            AsyncImage(
                model = item.bg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Shading gradientes izquierdo e inferior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BackgroundDark.copy(alpha = 0.95f),
                            BackgroundDark.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 900f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundDark
                        ),
                        startY = 180f
                    )
                )
        )

        // Textos y botones
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(start = 40.dp, bottom = 44.dp)
        ) {
            Text(
                text = clean.title.ifEmpty { item.name },
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 40.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (plotText.isNotBlank()) {
                Text(
                    text = plotText,
                    color = Color(0xFFECE4D2),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldGradient)
                        .clickable { onPlay() }
                        .padding(horizontal = 24.dp, vertical = 11.dp)
                        .testTag("hero_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶ Ver ahora",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF788698).copy(alpha = 0.5f))
                        .clickable { onMoreInfo() }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                        .testTag("hero_more_info_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Más información",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
