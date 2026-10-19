package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CatalogItem
import com.example.data.model.FlatEpisode
import com.example.data.model.ImdbInfo
import com.example.data.model.PlaybackProgress
import com.example.data.model.VodInfoDetails
import com.example.data.repository.CleanTitle
import com.example.data.repository.cleanTitle
import com.example.ui.screens.formatTime
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldGradient
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ImdbGold
import com.example.ui.theme.InputBorderDark
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.RottenGreen
import com.example.ui.theme.RottenRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WatchedGreen

@Composable
fun DetailDialog(
    item: CatalogItem,
    vodDetails: VodInfoDetails?,
    episodes: List<FlatEpisode>,
    imdbInfo: ImdbInfo?,
    isImdbLoading: Boolean,
    translatedPlot: String?,
    showSpanishPlot: Boolean,
    vodProgress: PlaybackProgress?,
    episodeProgressMap: Map<String, PlaybackProgress>,
    onPlayVod: (fromStart: Boolean) -> Unit,
    onToggleVodWatched: () -> Unit,
    onPlayEpisode: (episode: FlatEpisode, fromStart: Boolean) -> Unit,
    onToggleEpisodeWatched: (episode: FlatEpisode) -> Unit,
    onToggleTranslatePlot: () -> Unit,
    onCustomImdbSearch: (query: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isSeries = item.mode == "series"
    val clean = remember(item.name) { cleanTitle(item.name) }

    // Backdrop URL
    val backdropUrl = vodDetails?.backdropUrl?.ifEmpty { item.bg } ?: item.bg
    val posterUrl = item.img.ifEmpty { vodDetails?.movieImage ?: (imdbInfo?.img ?: "") }

    // Metadatos
    val year = clean.year?.toString() ?: imdbInfo?.year ?: vodDetails?.releaseDate?.take(4) ?: ""
    val runtime = imdbInfo?.runtime ?: vodDetails?.duration ?: ""
    val genre = imdbInfo?.genre ?: vodDetails?.genre ?: ""
    val rating = imdbInfo?.rating ?: vodDetails?.rating?.toString() ?: (if (item.rating > 0) item.rating.toString() else "")
    val rtScore = imdbInfo?.rt ?: ""

    // Episodios y temporadas para series
    val seasons = remember(episodes) {
        episodes.map { it.season }.distinct().sortedBy { it.toIntOrNull() ?: 0 }
    }
    var selectedSeason by remember(seasons) {
        mutableStateOf(seasons.firstOrNull() ?: "1")
    }
    var seasonDropdownOpen by remember { mutableStateOf(false) }

    var customSearchText by remember { mutableStateOf(clean.title) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC7000000))
                .padding(horizontal = 40.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // BANNER PRINCIPAL (dh)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3A3326), Color(0xFF232A35))
                                )
                            )
                    ) {
                        if (backdropUrl.isNotBlank()) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Sombreado elegante sobre el banner
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x99232A35),
                                            PanelDark
                                        ),
                                        startY = 100f
                                    )
                                )
                        )

                        // Contenido textual y botones sobre el banner
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = clean.title.ifEmpty { item.name },
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Chips de información (IMDb, RT, Año, Duración, Género)
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (rating.isNotBlank() && rating != "0" && rating != "0.0") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(ImdbGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "IMDb $rating/10",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                if (rtScore.isNotBlank()) {
                                    val sc = rtScore.replace("%", "").trim().toIntOrNull() ?: 100
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (sc < 60) RottenGreen else RottenRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RT $rtScore",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                if (year.isNotBlank()) {
                                    Text(text = year, color = GoldLight, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                if (runtime.isNotBlank()) {
                                    Text(text = runtime, color = TextSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                if (genre.isNotBlank()) {
                                    Text(text = genre, color = TextSecondary, fontSize = 13.sp)
                                }
                            }

                            // Botones de acción principales
                            if (!isSeries) {
                                val resumeSec = vodProgress?.resumeSeconds ?: 0L
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (resumeSec > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GoldGradient)
                                                .clickable { onPlayVod(false) }
                                                .padding(horizontal = 22.dp, vertical = 11.dp)
                                                .testTag("resume_vod_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "▶ Continuar (${formatTime(resumeSec)})",
                                                color = BackgroundDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF788698).copy(alpha = 0.5f))
                                                .clickable { onPlayVod(true) }
                                                .padding(horizontal = 18.dp, vertical = 11.dp)
                                                .testTag("restart_vod_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Desde el inicio",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GoldGradient)
                                                .clickable { onPlayVod(true) }
                                                .padding(horizontal = 24.dp, vertical = 11.dp)
                                                .testTag("play_vod_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "▶ Reproducir",
                                                color = BackgroundDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Barra de progreso y texto
                                    if (vodProgress != null && vodProgress.d > 0) {
                                        if (vodProgress.isWatched) {
                                            Text(
                                                text = "✓ Vista",
                                                color = WatchedGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "${formatTime(vodProgress.t)} / ${formatTime(vodProgress.d)}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Para series: botón de continuar al episodio correspondiente
                                val targetEp = episodes.firstOrNull()
                                if (targetEp != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldGradient)
                                            .clickable { onPlayEpisode(targetEp, false) }
                                            .padding(horizontal = 24.dp, vertical = 11.dp)
                                            .testTag("play_series_target_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "▶ Reproducir T${targetEp.season} E${targetEp.num}",
                                            color = BackgroundDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CUERPO DE LA FICHA (dbody)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 20.dp)
                    ) {
                        // Columna Izquierda: Botones secundarios y Sinopsis
                        Column(modifier = Modifier.weight(2f).padding(end = 30.dp)) {
                            Row(
                                modifier = Modifier.padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isSeries) {
                                    val isWatched = vodProgress?.isWatched == true
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3A4554))
                                            .clickable { onToggleVodWatched() }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isWatched) "Quitar de vistas" else "Marcar como vista",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (imdbInfo != null && imdbInfo.id.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3A4554))
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.imdb.com/title/${imdbInfo.id}/"))
                                                context.startActivity(intent)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Abrir en IMDb",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                val trailer = vodDetails?.youtubeTrailer
                                if (!trailer.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3A4554))
                                            .clickable {
                                                val uri = if (trailer.startsWith("http", ignoreCase = true)) {
                                                    Uri.parse(trailer)
                                                } else {
                                                    Uri.parse("https://www.youtube.com/watch?v=$trailer")
                                                }
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Tráiler",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (!imdbInfo?.plot.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3A4554))
                                            .clickable { onToggleTranslatePlot() }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (showSpanishPlot) "Ver original" else "Traducir al español",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Sinopsis
                            val plotText = if (showSpanishPlot && !translatedPlot.isNullOrBlank()) {
                                translatedPlot
                            } else {
                                imdbInfo?.plot?.ifEmpty { vodDetails?.plot ?: item.plot } ?: (vodDetails?.plot ?: item.plot)
                            }

                            if (plotText.isNotBlank()) {
                                Text(
                                    text = "Sinopsis" + if (showSpanishPlot && translatedPlot != null) " (IMDb, traducida)" else if (imdbInfo?.plot?.isNotBlank() == true) " (IMDb)" else "",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = plotText,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            } else {
                                Text(
                                    text = "Sin sinopsis disponible.",
                                    color = TextMuted,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        // Columna Derecha: Reparto, Director, Votos y Búsqueda en IMDb
                        Column(modifier = Modifier.weight(1.2f)) {
                            val cast = imdbInfo?.actors ?: vodDetails?.cast ?: ""
                            if (cast.isNotBlank()) {
                                Text(text = "Reparto: $cast", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }

                            val director = imdbInfo?.director ?: vodDetails?.director ?: ""
                            if (director.isNotBlank()) {
                                Text(text = "Director: $director", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }

                            val votes = imdbInfo?.votes ?: ""
                            if (votes.isNotBlank()) {
                                Text(text = "Votos IMDb: $votes", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }

                            val awards = imdbInfo?.awards ?: ""
                            if (awards.isNotBlank()) {
                                Text(text = "Premios: $awards", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }

                            if (isImdbLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = GoldPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Buscando en IMDb...", color = TextMuted, fontSize = 12.sp)
                                }
                            }

                            // Búsqueda personalizada en IMDb
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = "¿No es la correcta? Buscar en IMDb:",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = customSearchText,
                                        onValueChange = { customSearchText = it },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = GoldPrimary,
                                            unfocusedBorderColor = InputBorderDark,
                                            focusedContainerColor = Color(0x800A0D12),
                                            unfocusedContainerColor = Color(0x800A0D12)
                                        ),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3A4554))
                                            .clickable { onCustomImdbSearch(customSearchText) }
                                            .padding(horizontal = 12.dp, vertical = 12.dp)
                                    ) {
                                        Text(text = "Buscar", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN DE EPISODIOS PARA SERIES
                    if (isSeries) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episodios",
                                    color = GoldLight,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Selector de Temporada
                                if (seasons.isNotEmpty()) {
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, InputBorderDark, RoundedCornerShape(6.dp))
                                                .background(PanelAltDark)
                                                .clickable { seasonDropdownOpen = true }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Temporada $selectedSeason",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = seasonDropdownOpen,
                                            onDismissRequest = { seasonDropdownOpen = false }
                                        ) {
                                            for (s in seasons) {
                                                DropdownMenuItem(
                                                    text = { Text("Temporada $s") },
                                                    onClick = {
                                                        selectedSeason = s
                                                        seasonDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Lista de episodios de la temporada seleccionada
                            val seasonEpisodes = episodes.filter { it.season == selectedSeason }
                            if (seasonEpisodes.isEmpty()) {
                                Text(
                                    text = "No hay episodios disponibles para esta temporada.",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (ep in seasonEpisodes) {
                                        val epProg = episodeProgressMap[ep.key]
                                        val epWatched = epProg?.isWatched == true
                                        val epResume = epProg?.resumeSeconds ?: 0L

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2A333F))
                                                .clickable { onPlayEpisode(ep, false) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Número de episodio
                                            Text(
                                                text = ep.num.toString(),
                                                color = GoldLight,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(36.dp)
                                            )

                                            // Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .height(76.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PanelDark)
                                            ) {
                                                if (ep.image.isNotBlank()) {
                                                    AsyncImage(
                                                        model = ep.image,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = GoldLight,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .align(Alignment.Center)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            // Título, duración y sinopsis
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = ep.title.ifEmpty { "Episodio ${ep.num}" },
                                                        color = TextPrimary,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (ep.durationText.isNotBlank()) {
                                                        Text(
                                                            text = ep.durationText,
                                                            color = TextSecondary,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }

                                                if (epResume > 0) {
                                                    Text(
                                                        text = "Quedaste en ${formatTime(epProg?.t ?: 0)}",
                                                        color = GoldPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                if (ep.plot.isNotBlank()) {
                                                    Text(
                                                        text = ep.plot,
                                                        color = TextSecondary,
                                                        fontSize = 12.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Botón marcar vista
                                            if (epWatched) {
                                                Text(
                                                    text = "✓ Vista",
                                                    color = WatchedGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF3A4554))
                                                    .clickable { onToggleEpisodeWatched(ep) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (epWatched) "Quitar vista" else "Marcar vista",
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Botón Cerrar circular arriba a la derecha (mclose)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PanelDark)
                        .border(1.dp, InputBorderDark, CircleShape)
                        .testTag("detail_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
