package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class UserInfo(
    val username: String? = null,
    val password: String? = null,
    val auth: Any? = null,
    val status: String? = null,
    val expDate: String? = null,
    val activeCons: String? = null,
    val maxConnections: String? = null,
    val message: String? = null,
    val isTrial: String? = null,
    val createdAt: String? = null
) {
    val isAuthed: Boolean
        get() = com.example.data.util.isExplicitlyOneOrTrue(auth) ||
                (status.equals("Active", ignoreCase = true) && !com.example.data.util.isExplicitlyZeroOrFalse(auth))
}

@JsonClass(generateAdapter = true)
data class ServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null
)

data class XtreamAuthResponse(
    val userInfo: UserInfo? = null,
    val serverInfo: ServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: Any? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "parent_id") val parentId: Any? = null
) {
    val idString: String
        get() = categoryId?.toString() ?: ""
    val nameString: String
        get() = categoryName ?: ""
}

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_type") val streamType: String? = null,
    @Json(name = "stream_id") val streamId: Any? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @Json(name = "added") val added: String? = null,
    @Json(name = "category_id") val categoryId: Any? = null
) {
    val idString: String
        get() = streamId?.toString() ?: ""
    val catString: String
        get() = categoryId?.toString() ?: ""
}

@JsonClass(generateAdapter = true)
data class XtreamVodStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_type") val streamType: String? = null,
    @Json(name = "stream_id") val streamId: Any? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "rating") val rating: Any? = null,
    @Json(name = "added") val added: String? = null,
    @Json(name = "category_id") val categoryId: Any? = null,
    @Json(name = "container_extension") val containerExtension: String? = null
) {
    val idString: String
        get() = streamId?.toString() ?: ""
    val catString: String
        get() = categoryId?.toString() ?: ""
    val ratingFloat: Float
        get() = rating?.toString()?.toFloatOrNull() ?: 0f
}

@JsonClass(generateAdapter = true)
data class XtreamSeriesItem(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "series_id") val seriesId: Any? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "director") val director: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "last_modified") val lastModified: String? = null,
    @Json(name = "rating") val rating: Any? = null,
    @Json(name = "category_id") val categoryId: Any? = null,
    @Json(name = "backdrop_path") val backdropPath: Any? = null
) {
    val idString: String
        get() = seriesId?.toString() ?: ""
    val catString: String
        get() = categoryId?.toString() ?: ""
    val ratingFloat: Float
        get() = rating?.toString()?.toFloatOrNull() ?: 0f
    val backdropUrl: String
        get() {
            if (backdropPath is List<*>) {
                return (backdropPath as List<*>).firstOrNull()?.toString() ?: ""
            }
            return backdropPath?.toString() ?: ""
        }
}

// Representación unificada en memoria para buscador y listas
data class CatalogItem(
    val mode: String, // "live", "vod", "series"
    val sid: String,
    val name: String,
    val img: String,
    val rating: Float,
    val added: Long,
    val ext: String = "mp4",
    val cid: String = "",
    val plot: String = "",
    val bg: String = "",
    val nl: String = "" // Clave normalizada de búsqueda
)
