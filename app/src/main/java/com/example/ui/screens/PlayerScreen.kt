package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
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

    // Mantener pantalla encendida y modo inmersivo
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

    // Inicializar ExoPlayer con Media3
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setUserAgent(com.example.data.api.NetworkConfig.USER_AGENT)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    // Preparar el medio
    LaunchedEffect(streamUrl) {
        errorMessage = null
        isLoading = true

        val uri = Uri.parse(streamUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isLoading = false
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
                errorMessage = "Error de reproducción: ${error.localizedMessage ?: "código ${error.errorCodeName}"}"
            }
        }

        exoPlayer.addListener(listener)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (resumeSeconds > 0 && !isLive) {
            exoPlayer.seekTo(resumeSeconds * 1000)
        }
        exoPlayer.playWhenReady = true
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
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Limpieza al cerrar reproductor
    DisposableEffect(exoPlayer) {
        onDispose {
            if (!isLive && exoPlayer.currentPosition > 0) {
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                onProgressUpdate(exoPlayer.currentPosition / 1000, dur / 1000)
            }
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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

        // Spinner de carga
        if (isLoading) {
            CircularProgressIndicator(
                color = GoldPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }

        // Mensaje de error si falla
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldPrimary)
                            .clickable { onClose() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Volver",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Controles superpuestos
        AnimatedVisibility(
            visible = controlsVisible,
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x80000000))
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Botones centrales: Rewind, Play/Pause, Fast Forward
                if (!isLive) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Retroceder 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Adelantar 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
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
                            .padding(horizontal = 24.dp, vertical = 16.dp)
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
