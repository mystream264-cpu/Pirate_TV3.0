package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaybackProgress(
    val t: Long = 0, // Posición en segundos
    val d: Long = 0, // Duración total en segundos
    val w: Int = 0,  // 1 si fue vista completamente, 0 si no
    val u: Long = 0  // Timestamp de última actualización (milisegundos)
) {
    val isWatched: Boolean
        get() = w == 1

    val percent: Int
        get() {
            if (isWatched) return 100
            if (d > 0) return ((t * 100) / d).toInt().coerceIn(0, 100)
            return 0
        }

    val resumeSeconds: Long
        get() {
            if (isWatched || t < 30) return 0
            return (t - 3).coerceAtLeast(0)
        }
}

@JsonClass(generateAdapter = true)
data class LastPlayedEntry(
    val kind: String, // "vod" o "series"
    val sid: String,
    val name: String,
    val img: String = "",
    val ext: String = "mp4",
    val epId: String? = null,
    val label: String? = null,
    val u: Long = 0
)

data class ContinueWatchingItem(
    val key: String,
    val entry: LastPlayedEntry,
    val progress: PlaybackProgress?
)
