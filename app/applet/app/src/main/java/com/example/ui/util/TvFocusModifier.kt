package com.example.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

val TvFocusGold = Color(0xFFCFA65C)

/**
 * Modificador para elementos interactivos en TV Box / Android TV:
 * - Foco visible con borde dorado #CFA65C de 3dp
 * - Ligera ampliación (scale 1.05) con animación suave
 * - Elevación en zIndex para no ser tapado por elementos contiguos
 * - Manejo robusto de eventos de control remoto (DPAD_CENTER, Enter, NumPadEnter)
 */
@Composable
fun Modifier.dpadFocusable(
    focusRequester: FocusRequester? = null,
    shape: Shape = RoundedCornerShape(6.dp),
    scale: Float = 1.05f,
    borderWidth: Dp = 3.dp,
    focusedBorderColor: Color = TvFocusGold,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scale else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_focus_scale"
    )

    return this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .zIndex(if (isFocused) 10f else 1f)
        .then(
            if (isFocused) Modifier.border(borderWidth, focusedBorderColor, shape)
            else Modifier
        )
        .then(
            if (onClick != null) {
                Modifier
                    .onKeyEvent { keyEvent ->
                        if (enabled && keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                        ) {
                            onClick()
                            true
                        } else false
                    }
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            } else {
                Modifier.focusable(enabled = enabled)
            }
        )
}

/**
 * Modificador de foco para campos de texto en TV Box
 */
@Composable
fun Modifier.tvTextFieldFocusable(
    focusRequester: FocusRequester? = null,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    return this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            val sc = if (isFocused) 1.02f else 1.0f
            scaleX = sc
            scaleY = sc
        }
        .zIndex(if (isFocused) 10f else 1f)
        .then(
            if (isFocused) Modifier.border(3.dp, TvFocusGold, shape)
            else Modifier
        )
}
