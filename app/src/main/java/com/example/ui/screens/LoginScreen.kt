package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.AssetUtils
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BrandGoldSerif
import com.example.ui.theme.GoldGradient
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InputBorderDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    initialUser: String,
    initialPass: String,
    isLoading: Boolean,
    errorMessage: String,
    diagnosticText: String? = null,
    onLogin: (user: String, pass: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var user by remember(initialUser) { mutableStateOf(initialUser) }
    var pass by remember(initialPass) { mutableStateOf(initialPass) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val hasLogo = AssetUtils.rememberAssetExists("img_logo.png")
    val scrollState = rememberScrollState()

    val bgBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF34404F),
            Color(0xFF232A35),
            Color(0xFF14181F)
        ),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .testTag("login_screen")
    ) {
        // Contenedor principal desplazable con soporte de teclado en pantalla
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo limitado a max 140dp para que quepa en 1080p o menores sin recortar
                if (hasLogo) {
                    AsyncImage(
                        model = AssetUtils.assetUri("img_logo.png"),
                        contentDescription = "Pirate_TV Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 140.dp)
                            .widthIn(max = 230.dp)
                            .padding(bottom = 20.dp)
                    )
                } else {
                    Text(
                        text = "Pirate_TV",
                        color = BrandGoldSerif,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 22.dp)
                    )
                }

                // Campo Usuario (.linp)
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    placeholder = { Text("Usuario", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = InputBorderDark,
                        focusedContainerColor = Color(0x8C0A0D12),
                        unfocusedContainerColor = Color(0x8C0A0D12)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_username_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Contraseña (.linp) con botón para ver/ocultar contraseña
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    placeholder = { Text("Contraseña", color = TextMuted) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val description = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.testTag("toggle_password_visibility")
                        ) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = if (isPasswordVisible) GoldPrimary else TextMuted
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = InputBorderDark,
                        focusedContainerColor = Color(0x8C0A0D12),
                        unfocusedContainerColor = Color(0x8C0A0D12)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPasswordVisible) KeyboardType.Text else KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isLoading) onLogin(user, pass)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_password_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Botón ENTRAR (.lgo) con degradado dorado y tracking
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldGradient)
                        .clickable(enabled = !isLoading) {
                            focusManager.clearFocus()
                            onLogin(user, pass)
                        }
                        .testTag("login_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = BackgroundDark,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "ENTRAR",
                            color = BackgroundDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp
                        )
                    }
                }

                // Mensaje de estado / error (lmsg) con diagnóstico visible si falla
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFE8C986),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.testTag("login_error_text")
                        )
                    }
                    if (!diagnosticText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = diagnosticText,
                                color = Color(0xFF8F9BAB),
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .testTag("login_diagnostic_text")
                            )
                        }
                    }
                }
            }
        }

        // Botón Ajustes circular en la esquina inferior derecha (#lgear)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, BorderDark, CircleShape)
                .background(Color(0x590A0D12))
                .clickable { onOpenSettings() }
                .testTag("login_gear_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
