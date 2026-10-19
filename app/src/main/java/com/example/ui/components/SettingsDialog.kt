package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldGradient
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InputBorderDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val CFG_PASS = "3623"

@Composable
fun SettingsPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun checkPassword() {
        if (password == CFG_PASS) {
            onSuccess()
        } else {
            errorMessage = "Contraseña incorrecta."
            password = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PanelDark)
                .border(1.dp, InputBorderDark, RoundedCornerShape(12.dp))
                .padding(24.dp)
                .testTag("settings_password_dialog")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ajustes",
                    color = GoldLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Escribe la contraseña para continuar.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    placeholder = { Text("Contraseña", color = TextMuted) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val description = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.testTag("toggle_settings_password_visibility")
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
                        keyboardType = if (isPasswordVisible) KeyboardType.Number else KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { checkPassword() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row {
                    // Botón Aceptar con degradado dorado
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldGradient)
                            .clickable { checkPassword() }
                            .testTag("accept_password_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aceptar",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Botón Cancelar gris
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3A4554))
                            .clickable { onDismiss() }
                            .testTag("cancel_password_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContentDialog(
    initialServer: String,
    initialLiveExt: String,
    initialOmdbKey: String,
    onDismiss: () -> Unit,
    onSave: (serverUrl: String, liveExt: String, omdbKey: String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialServer) }
    var liveExt by remember { mutableStateOf(initialLiveExt) }
    var omdbKey by remember { mutableStateOf(initialOmdbKey) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PanelDark)
                .border(1.dp, InputBorderDark, RoundedCornerShape(10.dp))
                .padding(24.dp)
                .testTag("settings_content_dialog")
        ) {
            Column {
                Text(
                    text = "Ajustes",
                    color = GoldLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Campo Servidor
                Text(
                    text = "Servidor",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = InputBorderDark,
                        focusedContainerColor = Color(0x990A0D12),
                        unfocusedContainerColor = Color(0x990A0D12)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("server_input")
                )

                // Selector de formato de TV en vivo
                Text(
                    text = "Formato de TV en vivo",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(modifier = Modifier.padding(bottom = 14.dp)) {
                    val formats = listOf("ts", "m3u8")
                    for (fmt in formats) {
                        val isSelected = liveExt.lowercase() == fmt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) GoldPrimary else BorderDark, RoundedCornerShape(6.dp))
                                .background(if (isSelected) GoldPrimary else Color(0xFF2C343F))
                                .clickable { liveExt = fmt }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .testTag("format_$fmt"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ".$fmt",
                                color = if (isSelected) BackgroundDark else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Campo Clave OMDb
                Text(
                    text = "Clave OMDb (gratis en omdbapi.com) – trae la sinopsis, nota de IMDb y puntuación de Rotten Tomatoes",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = omdbKey,
                    onValueChange = { omdbKey = it },
                    singleLine = true,
                    placeholder = { Text("Ej: ab12cd34", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = InputBorderDark,
                        focusedContainerColor = Color(0x990A0D12),
                        unfocusedContainerColor = Color(0x990A0D12)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("omdb_key_input")
                )

                Text(
                    text = "El progreso y las películas/series vistas se guardan automáticamente en el dispositivo.",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                Row {
                    // Botón Guardar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldGradient)
                            .clickable { onSave(com.example.data.util.normalizeServerUrl(serverUrl), liveExt, omdbKey) }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                            .testTag("save_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Guardar",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Botón Cerrar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3A4554))
                            .clickable { onDismiss() }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                            .testTag("close_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cerrar",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
