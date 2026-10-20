package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.model.CatalogItem
import com.example.data.model.EpisodeInfo
import com.example.data.model.FlatEpisode
import com.example.data.model.ImdbInfo
import com.example.data.model.SeriesEpisode
import com.example.data.model.UserInfo
import com.example.data.model.VodInfoDetails
import com.example.data.model.XtreamAuthResponse
import com.example.data.model.XtreamCategory
import com.example.data.model.XtreamLiveStream
import com.example.data.model.XtreamSeriesItem
import com.example.data.model.XtreamVodStream
import com.example.data.storage.PlaybackProgressManager
import com.example.data.util.LoginParseResult
import com.example.data.util.maskPasswordInUrl
import com.example.data.util.normalizeServerUrl
import com.example.data.util.parseAuthResponse
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class ApiDiagnostic(
    val maskedUrl: String,
    val httpCode: Int?,
    val preview: String
) {
    fun formatDisplay(): String {
        val codeText = httpCode?.toString() ?: "N/A"
        val previewClean = preview.replace("\r", " ").replace("\n", " ").trim()
        val previewSnippet = if (previewClean.length > 120) previewClean.take(120) + "..." else previewClean
        return "URL: $maskedUrl\nCódigo HTTP: $codeText\nRespuesta: $previewSnippet"
    }
}

class ApiException(
    val userMessage: String,
    val diagnostic: ApiDiagnostic? = null,
    cause: Throwable? = null
) : Exception(userMessage, cause)

// Utilidades de normalización según HTA original
val PUNCT_REGEX = Regex("""[\-_.:,;!?'"()\[\]|/\\&+*#@~`\u00b4\u2018\u2019\u201c\u201d]+""")
const val FROM_CHARS = "\u00e1\u00e0\u00e4\u00e2\u00e3\u00e9\u00e8\u00eb\u00ea\u00ed\u00ec\u00ef\u00ee\u00f3\u00f2\u00f6\u00f4\u00f5\u00fa\u00f9\u00fc\u00fb\u00f1\u00e7"
const val TO_CHARS   = "aaaaaeeeeiiiiooooouuuunc"

fun fold(s: String?): String {
    if (s == null) return ""
    val lower = s.lowercase()
    val sb = StringBuilder(lower.length)
    for (ch in lower) {
        val idx = FROM_CHARS.indexOf(ch)
        if (idx >= 0) sb.append(TO_CHARS[idx]) else sb.append(ch)
    }
    return sb.toString()
}

fun keyOf(s: String?): String {
    return fold(s).replace(PUNCT_REGEX, " ").trim().replace(Regex("""\s+"""), " ")
}

fun tokens(q: String): List<String> {
    val k = keyOf(q)
    return if (k.isEmpty()) emptyList() else k.split(" ").filter { it.isNotEmpty() }
}

fun hashNs(base: String, user: String): String {
    val s = "$base|$user"
    var h = 5381L
    for (ch in s) {
        h = (((h shl 5) + h) + ch.code) and 0xFFFFFFFFL
    }
    return java.lang.Long.toUnsignedString(h, 36)
}

data class CleanTitle(val title: String, val year: Int?)

fun cleanTitle(name: String?): CleanTitle {
    var s = name ?: ""
    var year: Int? = null
    val yearRegex = Regex("""[\(\[]\s*((?:19|20)\d{2})\s*[\)\]]""")
    val m = yearRegex.find(s)
    if (m != null) {
        year = m.groupValues[1].toIntOrNull()
    }
    s = s.replace(Regex("""^\s*[A-Z]{2,4}\s*\|\s*"""), "")
        .replace(Regex("""^\s*[A-Z]{2,4}\s+-\s+"""), "")
        .replace(Regex("""\[[^\]]*\]"""), " ")
        .replace(Regex("""\([^\)]*\)"""), " ")
        .replace(Regex("""\b(4K|UHD|FHD|FULLHD|HD|SD|1080P|720P|2160P|HEVC|H265|X265|LATINO|CASTELLANO|ESPA.OL|DUAL|MULTI|SUBTITULADO|SUBS?|VOSE|VOS)\b""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""[\|_]+"""), " ")

    val endYearRegex = Regex("""^(.*\S)[\s\-:]+((?:19|20)\d{2})\s*$""")
    val endM = endYearRegex.find(s)
    if (endM != null) {
        if (year == null) year = endM.groupValues[2].toIntOrNull()
        s = endM.groupValues[1]
    }
    s = s.replace(Regex("""\s{2,}"""), " ").trim(' ', '-', ':')
    return CleanTitle(s.ifEmpty { name ?: "" }, year)
}

class XtreamRepository(
    private val progressManager: PlaybackProgressManager
) {

    private val omdbApi = ApiClient.omdbService

    // Caché en memoria de catálogos y categorías
    val catalogCache = mutableMapOf<String, List<CatalogItem>>() // "live", "vod", "series"
    val categoryCache = mutableMapOf<String, List<XtreamCategory>>()
    val categoryCounts = mutableMapOf<String, Map<String, Int>>()
    val categoryNames = mutableMapOf<String, Map<String, String>>()
    val imdbCache = mutableMapOf<String, ImdbInfo>()

    var currentBaseUrl = "http://sdmx.vip:80"
    var currentUser = ""
    var currentPass = ""
    var currentNs = ""

    var lastErrorMessage: String? = null

    fun setupSession(baseUrl: String, user: String, pass: String) {
        currentBaseUrl = normalizeServerUrl(baseUrl)
        currentUser = user.trim()
        currentPass = pass
        currentNs = hashNs(currentBaseUrl, currentUser)
    }

    fun clearSession() {
        catalogCache.clear()
        categoryCache.clear()
        categoryCounts.clear()
        categoryNames.clear()
        lastErrorMessage = null
    }

    /**
     * Ejecuta una petición directa HTTP a través de OkHttp con el User-Agent centralizado,
     * obteniendo la respuesta en crudo y la URL exacta con la contraseña enmascarada para diagnóstico.
     */
    private suspend fun executeXtreamRaw(
        action: String? = null,
        extraParams: Map<String, String> = emptyMap()
    ): Pair<Response, String> = withContext(Dispatchers.IO) {
        val normalizedBase = normalizeServerUrl(currentBaseUrl)
        val httpUrlBuilder = normalizedBase.toHttpUrlOrNull()?.newBuilder()
            ?: throw ApiException("Dirección del servidor inválida: $currentBaseUrl")

        httpUrlBuilder.addPathSegment("player_api.php")
        httpUrlBuilder.addQueryParameter("username", currentUser)
        httpUrlBuilder.addQueryParameter("password", currentPass)
        if (!action.isNullOrEmpty()) {
            httpUrlBuilder.addQueryParameter("action", action)
        }
        for ((k, v) in extraParams) {
            httpUrlBuilder.addQueryParameter(k, v)
        }

        val requestUrl = httpUrlBuilder.build()
        val maskedUrl = maskPasswordInUrl(requestUrl.toString())

        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        try {
            val response = ApiClient.okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            Pair(response, bodyString)
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado al conectar con el servidor."
                is java.net.UnknownHostException -> "No se pudo resolver el nombre del servidor. Verifica la dirección web."
                is java.net.ConnectException -> "No se pudo establecer conexión con el servidor (puerto cerrado o fuera de línea)."
                else -> "Error de red al conectar con el servidor (${e.localizedMessage ?: "desconocido"})."
            }
            throw ApiException(
                userMessage = errorMsg,
                diagnostic = ApiDiagnostic(
                    maskedUrl = maskedUrl,
                    httpCode = null,
                    preview = e.localizedMessage ?: e.javaClass.simpleName
                ),
                cause = e
            )
        }
    }

    /**
     * Valida que la respuesta HTTP sea satisfactoria y que el cuerpo comience con '{' o '['
     * antes de intentar convertirlo a JSON, evitando errores técnicos crudos.
     */
    private fun validateJsonBody(response: Response, rawBody: String, maskedUrl: String): String {
        val trimmed = rawBody.trim()
        val isJson = trimmed.startsWith("{") || trimmed.startsWith("[")
        val preview = if (trimmed.length > 120) trimmed.take(120) else trimmed

        if (!response.isSuccessful || !isJson) {
            val userMsg = if (!isJson) {
                "El servidor respondió algo inesperado (código HTTP ${response.code})"
            } else {
                "El servidor respondió con error (código HTTP ${response.code})"
            }
            throw ApiException(
                userMessage = userMsg,
                diagnostic = ApiDiagnostic(
                    maskedUrl = maskedUrl,
                    httpCode = response.code,
                    preview = preview
                )
            )
        }
        return trimmed
    }

    suspend fun login(baseUrl: String, user: String, pass: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        try {
            setupSession(baseUrl, user, pass)
            val (response, rawBody) = executeXtreamRaw()
            val maskedUrl = maskPasswordInUrl(response.request.url.toString())
            val trimmed = validateJsonBody(response, rawBody, maskedUrl)

            val preview = if (trimmed.length > 120) trimmed.take(120) else trimmed

            when (val parseResult = parseAuthResponse(trimmed)) {
                is LoginParseResult.Success -> {
                    Result.success(parseResult.userInfo)
                }
                is LoginParseResult.InvalidCredentials -> {
                    throw ApiException(
                        userMessage = parseResult.message,
                        diagnostic = ApiDiagnostic(maskedUrl, response.code, preview)
                    )
                }
                is LoginParseResult.AccountExpiredOrInactive -> {
                    throw ApiException(
                        userMessage = parseResult.message,
                        diagnostic = ApiDiagnostic(maskedUrl, response.code, preview)
                    )
                }
                is LoginParseResult.UnexpectedResponse -> {
                    throw ApiException(
                        userMessage = parseResult.message,
                        diagnostic = ApiDiagnostic(maskedUrl, response.code, preview)
                    )
                }
            }
        } catch (e: ApiException) {
            lastErrorMessage = e.userMessage
            Result.failure(e)
        } catch (e: Exception) {
            val genericMsg = "No se pudo conectar con el servidor (${e.localizedMessage ?: "error de red"})."
            lastErrorMessage = genericMsg
            Result.failure(ApiException(genericMsg, cause = e))
        }
    }

    suspend fun loadCategories(mode: String): List<XtreamCategory> = withContext(Dispatchers.IO) {
        categoryCache[mode]?.let { return@withContext it }
        val action = when (mode) {
            "live" -> "get_live_categories"
            "vod" -> "get_vod_categories"
            else -> "get_series_categories"
        }

        try {
            val (response, rawBody) = executeXtreamRaw(action = action)
            val maskedUrl = maskPasswordInUrl(response.request.url.toString())
            val trimmed = validateJsonBody(response, rawBody, maskedUrl)

            val list: List<XtreamCategory> = if (trimmed.startsWith("[")) {
                val type = Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
                val adapter = ApiClient.moshi.adapter<List<XtreamCategory>>(type)
                adapter.fromJson(trimmed) ?: emptyList()
            } else {
                emptyList()
            }

            categoryCache[mode] = list
            categoryNames[mode] = list.associate { it.idString to it.nameString }
            list
        } catch (e: ApiException) {
            lastErrorMessage = e.userMessage
            emptyList()
        } catch (e: Exception) {
            lastErrorMessage = "Error al conectar con el servidor."
            emptyList()
        }
    }

    suspend fun loadCatalog(mode: String): List<CatalogItem> = withContext(Dispatchers.IO) {
        catalogCache[mode]?.let { return@withContext it }
        loadCategories(mode)

        val action = when (mode) {
            "live" -> "get_live_streams"
            "vod" -> "get_vod_streams"
            else -> "get_series"
        }

        try {
            val (response, rawBody) = executeXtreamRaw(action = action)
            val maskedUrl = maskPasswordInUrl(response.request.url.toString())
            val trimmed = validateJsonBody(response, rawBody, maskedUrl)

            val items: List<CatalogItem> = when (mode) {
                "live" -> {
                    val type = Types.newParameterizedType(List::class.java, XtreamLiveStream::class.java)
                    val adapter = ApiClient.moshi.adapter<List<XtreamLiveStream>>(type)
                    val raw = if (trimmed.startsWith("[")) adapter.fromJson(trimmed) ?: emptyList() else emptyList()
                    raw.map {
                        CatalogItem(
                            mode = "live",
                            sid = it.idString,
                            name = it.name ?: "",
                            img = it.streamIcon ?: "",
                            rating = 0f,
                            added = it.added?.toLongOrNull() ?: 0L,
                            cid = it.catString,
                            nl = keyOf(it.name)
                        )
                    }
                }
                "vod" -> {
                    val type = Types.newParameterizedType(List::class.java, XtreamVodStream::class.java)
                    val adapter = ApiClient.moshi.adapter<List<XtreamVodStream>>(type)
                    val raw = if (trimmed.startsWith("[")) adapter.fromJson(trimmed) ?: emptyList() else emptyList()
                    raw.map {
                        CatalogItem(
                            mode = "vod",
                            sid = it.idString,
                            name = it.name ?: "",
                            img = it.streamIcon ?: "",
                            rating = it.ratingFloat,
                            added = it.added?.toLongOrNull() ?: 0L,
                            ext = it.containerExtension?.ifEmpty { "mp4" } ?: "mp4",
                            cid = it.catString,
                            nl = keyOf(it.name)
                        )
                    }.sortedWith(compareByDescending<CatalogItem> { it.rating }.thenByDescending { it.added })
                }
                else -> {
                    val type = Types.newParameterizedType(List::class.java, XtreamSeriesItem::class.java)
                    val adapter = ApiClient.moshi.adapter<List<XtreamSeriesItem>>(type)
                    val raw = if (trimmed.startsWith("[")) adapter.fromJson(trimmed) ?: emptyList() else emptyList()
                    raw.map {
                        CatalogItem(
                            mode = "series",
                            sid = it.idString,
                            name = it.name ?: "",
                            img = it.cover ?: "",
                            rating = it.ratingFloat,
                            added = it.lastModified?.toLongOrNull() ?: 0L,
                            cid = it.catString,
                            plot = it.plot ?: "",
                            bg = it.backdropUrl,
                            nl = keyOf(it.name)
                        )
                    }.sortedWith(compareByDescending<CatalogItem> { it.rating }.thenByDescending { it.added })
                }
            }

            // Calcular conteo de elementos por categoría
            val counts = mutableMapOf<String, Int>()
            for (it in items) {
                if (it.cid.isNotEmpty()) {
                    counts[it.cid] = (counts[it.cid] ?: 0) + 1
                }
            }
            categoryCounts[mode] = counts
            catalogCache[mode] = items
            items
        } catch (e: ApiException) {
            lastErrorMessage = e.userMessage
            emptyList()
        } catch (e: Exception) {
            lastErrorMessage = "Error al conectar con el servidor."
            emptyList()
        }
    }

    fun search(query: String, modeFilter: String = "all"): Map<String, List<CatalogItem>> {
        val qTokens = tokens(query)
        if (qTokens.isEmpty()) return emptyMap()

        val modes = if (modeFilter == "all") listOf("live", "vod", "series") else listOf(modeFilter)
        val result = mutableMapOf<String, List<CatalogItem>>()

        for (m in modes) {
            val list = catalogCache[m] ?: emptyList()
            val filtered = list.filter { item ->
                qTokens.all { token -> item.nl.contains(token) }
            }
            result[m] = filtered
        }
        return result
    }

    suspend fun getVodDetails(vodId: String): Pair<VodInfoDetails, String> = withContext(Dispatchers.IO) {
        try {
            val (response, rawBody) = executeXtreamRaw(
                action = "get_vod_info",
                extraParams = mapOf("vod_id" to vodId)
            )
            val maskedUrl = maskPasswordInUrl(response.request.url.toString())
            val trimmed = validateJsonBody(response, rawBody, maskedUrl)
            if (!trimmed.startsWith("{")) {
                return@withContext Pair(VodInfoDetails(), "mp4")
            }

            val jsonObj = JSONObject(trimmed)
            val infoObj = jsonObj.optJSONObject("info")
            val movieDataObj = jsonObj.optJSONObject("movie_data")

            val details = if (infoObj != null) {
                var bg = ""
                val bgOpt = infoObj.opt("backdrop_path")
                if (bgOpt is JSONArray && bgOpt.length() > 0) {
                    bg = bgOpt.optString(0)
                } else if (bgOpt != null && bgOpt !is JSONObject && bgOpt !is JSONArray) {
                    bg = bgOpt.toString()
                }

                VodInfoDetails(
                    movieImage = infoObj.optString("movie_image"),
                    backdropPath = bg,
                    plot = infoObj.optString("plot").ifEmpty { infoObj.optString("description") },
                    releaseDate = infoObj.optString("releasedate").ifEmpty { infoObj.optString("release_date") },
                    duration = infoObj.optString("duration"),
                    genre = infoObj.optString("genre"),
                    director = infoObj.optString("director"),
                    cast = infoObj.optString("cast"),
                    rating = infoObj.optString("rating"),
                    youtubeTrailer = infoObj.optString("youtube_trailer")
                )
            } else {
                VodInfoDetails()
            }

            val ext = movieDataObj?.optString("container_extension")?.ifEmpty { "mp4" } ?: "mp4"
            Pair(details, ext)
        } catch (_: Exception) {
            Pair(VodInfoDetails(), "mp4")
        }
    }

    suspend fun getSeriesDetails(seriesId: String): Pair<VodInfoDetails, List<FlatEpisode>> = withContext(Dispatchers.IO) {
        try {
            val (response, rawBody) = executeXtreamRaw(
                action = "get_series_info",
                extraParams = mapOf("series_id" to seriesId)
            )
            val maskedUrl = maskPasswordInUrl(response.request.url.toString())
            val trimmed = validateJsonBody(response, rawBody, maskedUrl)
            if (!trimmed.startsWith("{")) {
                return@withContext Pair(VodInfoDetails(), emptyList())
            }

            val jsonObj = JSONObject(trimmed)
            val infoObj = jsonObj.optJSONObject("info")
            var bg = ""
            val bgOpt = infoObj?.opt("backdrop_path")
            if (bgOpt is JSONArray && bgOpt.length() > 0) {
                bg = bgOpt.optString(0)
            } else if (bgOpt != null && bgOpt !is JSONObject && bgOpt !is JSONArray) {
                bg = bgOpt.toString()
            }

            val details = VodInfoDetails(
                movieImage = infoObj?.optString("cover"),
                backdropPath = bg,
                plot = infoObj?.optString("plot"),
                releaseDate = infoObj?.optString("releaseDate"),
                duration = infoObj?.optString("episode_run_time")?.let { if (it.isNotEmpty()) "$it min" else "" },
                genre = infoObj?.optString("genre"),
                director = infoObj?.optString("director"),
                cast = infoObj?.optString("cast"),
                rating = infoObj?.optString("rating"),
                youtubeTrailer = infoObj?.optString("youtube_trailer")
            )

            val flatEpisodes = mutableListOf<FlatEpisode>()
            val episodesObj = jsonObj.optJSONObject("episodes")

            if (episodesObj != null) {
                val seasonKeys = episodesObj.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
                for (sn in seasonKeys) {
                    val arr = episodesObj.optJSONArray(sn) ?: continue
                    for (i in 0 until arr.length()) {
                        val epObj = arr.optJSONObject(i) ?: continue
                        val epId = epObj.optString("id")
                        val epNum = epObj.optInt("episode_num", i + 1)
                        val epTitle = epObj.optString("title")
                        val ext = epObj.optString("container_extension").ifEmpty { "mp4" }
                        val epInfo = epObj.optJSONObject("info")

                        val epImg = epInfo?.optString("movie_image") ?: ""
                        val epPlot = epInfo?.optString("plot") ?: ""
                        val epDur = epInfo?.optString("duration") ?: ""
                        val epDurSecs = epInfo?.optLong("duration_secs", 0L) ?: 0L

                        val streamUrl = "$currentBaseUrl/series/$currentUser/$currentPass/$epId.$ext"

                        flatEpisodes.add(
                            FlatEpisode(
                                season = sn,
                                num = epNum,
                                id = epId,
                                title = epTitle,
                                ext = ext,
                                durationText = epDur,
                                durationSecs = epDurSecs,
                                image = epImg,
                                plot = epPlot,
                                key = PlaybackProgressManager.ekKey(currentNs, epId),
                                streamUrl = streamUrl
                            )
                        )
                    }
                }
            }

            Pair(details, flatEpisodes)
        } catch (_: Exception) {
            Pair(VodInfoDetails(), emptyList())
        }
    }

    suspend fun lookupImdb(
        rawName: String,
        mode: String,
        customQuery: String? = null,
        omdbKey: String = ""
    ): ImdbInfo? = withContext(Dispatchers.IO) {
        val clean = cleanTitle(rawName)
        val title = (customQuery ?: clean.title).trim()
        val cacheKey = "$mode|${title.lowercase()}|${if (customQuery != null) "" else (clean.year?.toString() ?: "")}"

        imdbCache[cacheKey]?.let { return@withContext it }

        if (title.isEmpty()) return@withContext null

        try {
            if (omdbKey.isNotEmpty()) {
                val omdbRes = omdbApi.getOmdbByTitle(
                    title = title,
                    year = if (customQuery != null) null else clean.year,
                    apiKey = omdbKey
                )
                if (omdbRes.response.equals("True", ignoreCase = true) && omdbRes.imdbId != null) {
                    val info = ImdbInfo(
                        id = omdbRes.imdbId,
                        title = omdbRes.title ?: title,
                        year = omdbRes.year,
                        plot = omdbRes.plot,
                        rating = omdbRes.imdbRating,
                        votes = omdbRes.imdbVotes,
                        rt = omdbRes.rottenTomatoesScore,
                        genre = omdbRes.genre,
                        runtime = omdbRes.runtime,
                        director = omdbRes.director,
                        actors = omdbRes.actors,
                        rated = omdbRes.rated,
                        awards = omdbRes.awards,
                        img = omdbRes.poster
                    )
                    imdbCache[cacheKey] = info
                    return@withContext info
                }
            }

            val firstChar = title.firstOrNull()?.lowercaseChar() ?: 'x'
            val ch = if (firstChar in 'a'..'z' || firstChar in '0'..'9') firstChar else 'x'
            val encodedTitle = URLEncoder.encode(title.lowercase(), "UTF-8")
            val suggUrl = "https://v2.sg.media-imdb.com/suggestion/$ch/$encodedTitle.json"

            val body = omdbApi.getImdbSuggestions(suggUrl).string()
            val root = JSONObject(body)
            val dArr = root.optJSONArray("d") ?: return@withContext null

            var bestId: String? = null
            var bestTitle = title
            var bestYear: String? = null
            var bestImg: String? = null

            for (i in 0 until dArr.length()) {
                val item = dArr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (!id.startsWith("tt")) continue
                val q = item.optString("q").lowercase()
                if (q.contains("video game")) continue

                bestId = id
                bestTitle = item.optString("l", title)
                bestYear = item.optInt("y", 0).let { if (it > 0) it.toString() else null }
                bestImg = item.optJSONObject("i")?.optString("imageUrl")
                break
            }

            if (bestId != null) {
                if (omdbKey.isNotEmpty()) {
                    val omdbDetail = omdbApi.getOmdbById(imdbId = bestId, apiKey = omdbKey)
                    if (omdbDetail.response.equals("True", ignoreCase = true)) {
                        val info = ImdbInfo(
                            id = bestId,
                            title = omdbDetail.title ?: bestTitle,
                            year = omdbDetail.year ?: bestYear,
                            plot = omdbDetail.plot,
                            rating = omdbDetail.imdbRating,
                            votes = omdbDetail.imdbVotes,
                            rt = omdbDetail.rottenTomatoesScore,
                            genre = omdbDetail.genre,
                            runtime = omdbDetail.runtime,
                            director = omdbDetail.director,
                            actors = omdbDetail.actors,
                            rated = omdbDetail.rated,
                            awards = omdbDetail.awards,
                            img = omdbDetail.poster?.ifEmpty { bestImg } ?: bestImg
                        )
                        imdbCache[cacheKey] = info
                        return@withContext info
                    }
                }

                val basic = ImdbInfo(
                    id = bestId,
                    title = bestTitle,
                    year = bestYear,
                    img = bestImg
                )
                imdbCache[cacheKey] = basic
                return@withContext basic
            }
        } catch (_: Exception) {
        }
        null
    }

    suspend fun translateToSpanish(text: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t&q=$encoded"
            val resp = omdbApi.translateText(url).string()
            val jsonArr = JSONArray(resp)
            val segments = jsonArr.optJSONArray(0) ?: return@withContext null
            val sb = StringBuilder()
            for (i in 0 until segments.length()) {
                val seg = segments.optJSONArray(i) ?: continue
                sb.append(seg.optString(0))
            }
            sb.toString().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    fun buildLiveStreamUrl(streamId: String, ext: String): String {
        val cleanBase = currentBaseUrl.trim().trimEnd('/')
        val cleanUser = currentUser.trim()
        val cleanPass = currentPass.trim()
        val cleanId = streamId.trim()
        val extension = if (ext.trim().lowercase() == "ts") "ts" else "m3u8"
        return "$cleanBase/live/$cleanUser/$cleanPass/$cleanId.$extension"
    }

    fun buildVodStreamUrl(streamId: String, ext: String): String {
        val cleanBase = currentBaseUrl.trim().trimEnd('/')
        val cleanUser = currentUser.trim()
        val cleanPass = currentPass.trim()
        val cleanId = streamId.trim()
        val cleanExt = ext.trim().ifEmpty { "mp4" }
        return "$cleanBase/movie/$cleanUser/$cleanPass/$cleanId.$cleanExt"
    }
}
