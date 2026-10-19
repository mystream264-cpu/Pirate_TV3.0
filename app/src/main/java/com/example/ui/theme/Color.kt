package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta de colores exacta de Pirate_TV
val BackgroundDark = Color(0xFF1A1F27)
val PanelDark = Color(0xFF232A35)
val PanelAltDark = Color(0xFF2C343F)
val BorderDark = Color(0xFF3E4756)
val InputBorderDark = Color(0xFF4A586B)
val InputBgDark = Color(0x8C0A0D12)
val BottomBarDark = Color(0xFF10141A)

// Dorados y degradados
val GoldPrimary = Color(0xFFCFA65C)
val GoldLight = Color(0xFFF1D9A7)
val GoldDark = Color(0xFFB08843)
val BrandGoldSerif = Color(0xFFE3BD75)

// Textos
val TextPrimary = Color(0xFFF4ECDC)
val TextSecondary = Color(0xFFAEB8C5)
val TextMuted = Color(0xFF7D8999)

// Estados y etiquetas
val ImdbGold = Color(0xFFF5C518)
val RottenRed = Color(0xFFFA320A)
val RottenGreen = Color(0xFF5B8C2A)
val WatchedGreen = Color(0xFF46D369)
val WarningBrown = Color(0xFF4A3A1A)
val ErrorRed = Color(0xFFFF9D8A)

// Degradado dorado oficial de los botones principales (lgo, btnw, btn)
val GoldGradient = Brush.verticalGradient(
    colors = listOf(GoldLight, GoldPrimary, GoldDark)
)

val GoldGradientHover = Brush.verticalGradient(
    colors = listOf(Color(0xFFF7E5BD), Color(0xFFDCB56A), Color(0xFFBD9550))
)
