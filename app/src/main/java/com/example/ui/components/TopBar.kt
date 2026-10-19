package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BrandGoldSerif
import com.example.ui.theme.GoldLight
import com.example.ui.theme.InputBorderDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopBar(
    currentScreen: String, // "home", "sec", "search"
    sectionTitle: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onGoHome: () -> Unit,
    onToggleCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val hasSmallLogo = AssetUtils.rememberAssetExists("img_logo_small.png")

    val barGradient = Brush.verticalGradient(
        colors = listOf(Color(0xF70C0F14), Color(0x9E1A1F27))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(barGradient)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Marca Pirate_TV (Logo + Nombre en Serif dorada)
        Row(
            modifier = Modifier
                .clickable { onGoHome() }
                .padding(end = 12.dp)
                .testTag("brand_home_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSmallLogo) {
                AsyncImage(
                    model = AssetUtils.assetUri("img_logo_small.png"),
                    contentDescription = "Pirate_TV Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )
            }
            Text(
                text = "Pirate_TV",
                color = BrandGoldSerif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }

        // Botón Inicio ("◀ Inicio") visible en secciones o búsqueda
        if (currentScreen == "sec" || currentScreen == "search") {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .background(Color(0x0FFFFFFF))
                    .clickable { onGoHome() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("back_home_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◀ Inicio",
                    color = TextPrimary,
                    fontSize = 13.5.sp
                )
            }
        }

        // Título de la sección
        if (sectionTitle.isNotBlank()) {
            Text(
                text = sectionTitle,
                color = GoldLight,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Campo de búsqueda global "q"
        Row(
            modifier = Modifier
                .width(320.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, InputBorderDark, RoundedCornerShape(22.dp))
                .background(Color(0x990A0D12))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(GoldLight),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Buscar en TV, películas y series...",
                            color = TextMuted,
                            fontSize = 13.5.sp
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = onSearchClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Botón Categorías ("☰ Categorías") solo en secciones
        if (currentScreen == "sec") {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .background(Color(0x0FFFFFFF))
                    .clickable { onToggleCategories() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("categories_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "☰ Categorías",
                    color = TextPrimary,
                    fontSize = 13.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón Ajustes (Engrane)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, BorderDark, CircleShape)
                .background(Color(0x0FFFFFFF))
                .clickable { onOpenSettings() }
                .testTag("settings_gear_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón Salir (Cerrar sesión)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                .background(Color(0x0FFFFFFF))
                .clickable { onLogout() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("logout_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Salir",
                color = TextPrimary,
                fontSize = 13.5.sp
            )
        }
    }
}
