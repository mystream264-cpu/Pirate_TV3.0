package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VodInfoDetails(
    @Json(name = "movie_image") val movieImage: String? = null,
    @Json(name = "backdrop_path") val backdropPath: Any? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "releasedate") val releaseDate: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "director") val director: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "rating") val rating: Any? = null,
    @Json(name = "youtube_trailer") val youtubeTrailer: String? = null
) {
    val backdropUrl: String
        get() {
            if (backdropPath is List<*>) {
                return (backdropPath as List<*>).firstOrNull()?.toString() ?: ""
            }
            return backdropPath?.toString() ?: ""
        }
}

@JsonClass(generateAdapter = true)
data class VodMovieData(
    @Json(name = "stream_id") val streamId: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeInfo(
    @Json(name = "duration_secs") val durationSecs: Any? = null,
    @Json(name = "movie_image") val movieImage: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class SeriesEpisode(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "episode_num") val episodeNum: Any? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = null,
    @Json(name = "info") val info: EpisodeInfo? = null,
    @Json(name = "season") val season: Any? = null
) {
    val idString: String
        get() = id?.toString() ?: ""
    val numInt: Int
        get() = episodeNum?.toString()?.toIntOrNull() ?: 1
    val seasonInt: Int
        get() = season?.toString()?.toIntOrNull() ?: 1
    val ext: String
        get() = containerExtension?.ifEmpty { "mp4" } ?: "mp4"
}

data class FlatEpisode(
    val season: String,
    val num: Int,
    val id: String,
    val title: String,
    val ext: String,
    val durationText: String,
    val durationSecs: Long,
    val image: String,
    val plot: String,
    val key: String,
    val streamUrl: String
)
