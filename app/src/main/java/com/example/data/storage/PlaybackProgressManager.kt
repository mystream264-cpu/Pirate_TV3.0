package com.example.data.storage

import android.content.Context
import com.example.data.model.ContinueWatchingItem
import com.example.data.model.LastPlayedEntry
import com.example.data.model.PlaybackProgress
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlaybackProgressManager(context: Context) {

    private val file = File(context.filesDir, "progress.json")
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val progMapType = Types.newParameterizedType(Map::class.java, String::class.java, PlaybackProgress::class.java)
    private val lastMapType = Types.newParameterizedType(Map::class.java, String::class.java, LastPlayedEntry::class.java)

    private val progAdapter = moshi.adapter<Map<String, PlaybackProgress>>(progMapType)
    private val lastAdapter = moshi.adapter<Map<String, LastPlayedEntry>>(lastMapType)

    val prog: MutableMap<String, PlaybackProgress> = mutableMapOf()
    val last: MutableMap<String, LastPlayedEntry> = mutableMapOf()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!file.exists()) return
        try {
            val jsonString = file.readText()
            val root = moshi.adapter(Map::class.java).fromJson(jsonString) as? Map<*, *> ?: return
            
            val progJson = root["prog"]?.let { moshi.adapter(Any::class.java).toJson(it) }
            if (progJson != null) {
                progAdapter.fromJson(progJson)?.let { prog.putAll(it) }
            }

            val lastJson = root["last"]?.let { moshi.adapter(Any::class.java).toJson(it) }
            if (lastJson != null) {
                lastAdapter.fromJson(lastJson)?.let { last.putAll(it) }
            }
        } catch (_: Exception) {
            // Error silencioso al leer progreso corrupto
        }
    }

    suspend fun saveToDisk() = withContext(Dispatchers.IO) {
        try {
            val root = mapOf(
                "prog" to prog,
                "last" to last
            )
            val json = moshi.adapter(Map::class.java).toJson(root)
            file.writeText(json)
        } catch (_: Exception) {
        }
    }

    fun getProgress(key: String): PlaybackProgress? = prog[key]

    fun getLast(key: String): LastPlayedEntry? = last[key]

    suspend fun recordProgress(
        key: String,
        lastKey: String? = null,
        lastEntry: LastPlayedEntry? = null,
        currentSeconds: Long,
        durationSeconds: Long
    ) {
        val existing = prog[key] ?: PlaybackProgress()
        val duration = if (durationSeconds > 0) durationSeconds else existing.d
        val now = System.currentTimeMillis()
        val watched = if (duration > 0 && currentSeconds >= (duration * 0.9).toLong()) 1 else existing.w

        prog[key] = PlaybackProgress(
            t = currentSeconds,
            d = duration,
            w = watched,
            u = now
        )

        if (lastKey != null && lastEntry != null) {
            last[lastKey] = lastEntry.copy(u = now)
        }

        saveToDisk()
    }

    suspend fun toggleWatched(key: String, lastKey: String? = null, lastEntry: LastPlayedEntry? = null) {
        val existing = prog[key] ?: PlaybackProgress()
        val now = System.currentTimeMillis()
        val newProgress = if (existing.isWatched) {
            existing.copy(w = 0, t = 0, u = now)
        } else {
            val dur = if (existing.d > 0) existing.d else 100L
            existing.copy(w = 1, t = dur, d = dur, u = now)
        }
        prog[key] = newProgress

        if (lastKey != null && lastEntry != null) {
            last[lastKey] = lastEntry.copy(u = now)
        }

        saveToDisk()
    }

    suspend fun removeContinueItem(lastKey: String) {
        last.remove(lastKey)
        saveToDisk()
    }

    fun getContinueWatchingList(ns: String, mode: String): List<ContinueWatchingItem> {
        val prefix = if (mode == "vod") "m$ns:" else "s$ns:"
        val results = mutableListOf<ContinueWatchingItem>()

        for ((k, entry) in last) {
            if (!k.startsWith(prefix)) continue
            if (mode == "vod") {
                val p = prog[k]
                // En VOD solo mostramos si no está terminada y tiene al menos 30 segundos vistos
                if (p == null || p.isWatched || p.t < 30) continue
            }
            val progress = if (entry.kind == "series" && entry.epId != null) {
                prog[ekKey(ns, entry.epId)]
            } else {
                prog[k]
            }
            results.add(ContinueWatchingItem(key = k, entry = entry, progress = progress))
        }

        return results.sortedByDescending { it.entry.u }.take(20)
    }

    companion object {
        fun mkKey(ns: String, sid: String): String = "m$ns:$sid"
        fun ekKey(ns: String, epId: String): String = "e$ns:$epId"
        fun skKey(ns: String, sid: String): String = "s$ns:$sid"
    }
}
