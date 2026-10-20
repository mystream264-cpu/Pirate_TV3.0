package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.data.util.maskPasswordInUrl
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PanelAltDark
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.util.dpadFocusable
import kotlinx.coroutines.delay

fun formatTime(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, sec)
    } else {
        String.format("%02d:%02d", m, sec)
    }
}

private fun findHttpErrorCode(throwable: Throwable?): Int? {
    var cur = throwable
    while (cur != null) {
        if (cur is HttpDataSource.InvalidResponseCodeException) {
            return cur.responseCode
        }
        cur = cur.cause
    }
    return null
}

data class PlayerErrorState(
    val errorCodeName: String,
    val httpCode: Int?,
    val maskedUrl: String
)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    resumeSeconds: Long = 0,
    isLive: Boolean = false,
    onProgressUpdate: (currentSeconds: Long, durationSeconds: Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Mantener pantalla encendida y ocultar barras de sistema (modo inmersivo)
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Configuración robusta de DefaultHttpDataSource con User-Agent VLC y cross protocol redirects
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setUserAgent(com.example.data.api.NetworkConfig.USER_AGENT)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 50000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 5000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val livePlaybackSpeedControl = DefaultLivePlaybackSpeedControl.Builder()
            .setFallbackMinPlaybackSpeed(1.0f)
            .setFallbackMaxPlaybackSpeed(1.0f)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setLivePlaybackSpeedControl(livePlaybackSpeedControl)
            .build()
    }

    var activeUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
    var hasRetriedFallback by remember(streamUrl) { mutableStateOf(false) }
    var isReconnecting by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var errorState by remember { mutableStateOf<PlayerErrorState?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    // Focus requesters para navegación con control remoto
    val retryFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    fun buildMediaItem(url: String): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(url))
        if (isLive) {
            if (url.endsWith(".m3u8", ignoreCase = true)) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else if (url.endsWith(".ts", ignoreCase = true)) {
                builder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }
        return builder.build()
    }

    fun startPlayback(url: String) {
        errorState = null
        isLoading = true
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(buildMediaItem(url))
        exoPlayer.prepare()
        if (resumeSeconds > 0 && !isLive) {
            exoPlayer.seekTo(resumeSeconds * 1000)
        }
        exoPlayer.playWhenReady = true
    }

    // Configuración del listener de reproducción
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        isReconnecting = false
                        errorState = null
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isLoading = false
                        isReconnecting = false
                        onProgressUpdate(durationMs / 1000, durationMs / 1000)
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                // Reintento automático una sola vez cambiando entre .m3u8 y .ts si es TV en vivo
                if (isLive && !hasRetriedFallback) {
                    hasRetriedFallback = true
                    isReconnecting = true
                    val alternateUrl = if (activeUrl.endsWith(".m3u8", ignoreCase = true)) {
                        activeUrl.substringBeforeLast(".m3u8") + ".ts"
                    } else if (activeUrl.endsWith(".ts", ignoreCase = true)) {
                        activeUrl.substringBeforeLast(".ts") + ".m3u8"
                    } else {
                        "$activeUrl.ts"
                    }
                    activeUrl = alternateUrl
                    startPlayback(alternateUrl)
                } else {
                    isReconnecting = false
                    val httpCode = findHttpErrorCode(error)
                    val masked = maskPasswordInUrl(activeUrl)
                    errorState = PlayerErrorState(
                        errorCodeName = error.errorCodeName,
                        httpCode = httpCode,
                        maskedUrl = masked
                    )
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            if (!isLive && exoPlayer.currentPosition > 0) {
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                onProgressUpdate(exoPlayer.currentPosition / 1000, dur / 1000)
            }
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Preparar medio inicial
    LaunchedEffect(streamUrl) {
        hasRetriedFallback = false
        activeUrl = streamUrl
        startPlayback(streamUrl)
    }

    // Ticker para actualizar posición y guardar progreso periódicamente
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                currentPositionMs = current
                durationMs = dur

                if (!isLive && current > 0) {
                    onProgressUpdate(current / 1000, dur / 1000)
                }
            }
            delay(1000)
        }
    }

    // Auto-ocultar controles tras 4 segundos
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && errorState == null) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Al aparecer error o abrir controles, enfocar elemento clave
    LaunchedEffect(errorState) {
        if (errorState != null) {
            delay(100)
            try {
                retryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible && errorState == null) {
            delay(100)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!controlsVisible) {
                                controlsVisible = true
                                true
                            } else false
                        }
                        Key.DirectionLeft -> {
                            if (!isLive) {
                                controlsVisible = true
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!isLive) {
                                controlsVisible = true
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                true
                            } else false
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            controlsVisible = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
            .testTag("player_screen")
    ) {
        // PlayerView de Media3
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Spinner de carga y reconexión
        if (isLoading || isReconnecting) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    modifier = Modifier.size(52.dp)
                )
                if (isReconnecting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Reconectando...",
                        color = GoldLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Mensaje de error útil, selectable y botón Reintentar
        if (errorState != null) {
            val err = errorState!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0D1016)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(28.dp)
                        .widthIn(max = 680.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No se pudo reproducir el canal",
                        color = GoldLight,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SelectionContainer {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PanelDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Error: ${err.errorCodeName}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (err.httpCode != null) {
                                Text(
                                    text = "Código HTTP: ${err.httpCode}",
                                    color = Color(0xFFFF7B72),
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = err.maskedUrl,
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    focusRequester = retryFocusRequester,
                                    shape = RoundedCornerShape(6.dp),
                                    onClick = {
                                        hasRetriedFallback = false
                                        activeUrl = streamUrl
                                        startPlayback(streamUrl)
                                    }
                                )
                                .background(GoldPrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("player_retry_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Reintentar",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    shape = RoundedCornerShape(6.dp),
                                    onClick = onClose
                                )
                                .background(PanelAltDark, RoundedCornerShape(6.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("player_error_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Volver",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Controles superpuestos
        AnimatedVisibility(
            visible = controlsVisible && errorState == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                // Barra superior de controles: botón atrás y título
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(
                                shape = CircleShape,
                                onClick = onClose
                            )
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .testTag("player_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Botones centrales: Rewind, Play/Pause, Fast Forward
                if (!isLive) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    shape = CircleShape,
                                    onClick = {
                                        val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                        exoPlayer.seekTo(target)
                                    }
                                )
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Retroceder 10s",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    focusRequester = playPauseFocusRequester,
                                    shape = CircleShape,
                                    onClick = {
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            exoPlayer.play()
                                        }
                                    }
                                )
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .dpadFocusable(
                                    shape = CircleShape,
                                    onClick = {
                                        val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                        exoPlayer.seekTo(target)
                                    }
                                )
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Adelantar 10s",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Barra inferior: Slider de tiempo y duración
                if (!isLive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 30.dp, vertical = 20.dp)
                    ) {
                        val posSec = if (isSeeking) sliderValue.toLong() else currentPositionMs / 1000
                        val durSec = durationMs / 1000

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(posSec),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = formatTime(durSec),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Slider(
                            value = if (isSeeking) sliderValue else currentPositionMs.toFloat() / 1000f,
                            onValueChange = {
                                isSeeking = true
                                sliderValue = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                exoPlayer.seekTo((sliderValue * 1000).toLong())
                            },
                            valueRange = 0f..durSec.coerceAtLeast(1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_seek_bar")
                        )
                    }
                }
            }
        }
    }
}
