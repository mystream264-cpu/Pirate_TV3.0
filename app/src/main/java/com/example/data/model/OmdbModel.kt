package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OmdbRating(
    @Json(name = "Source") val source: String? = null,
    @Json(name = "Value") val value: String? = null
)

@JsonClass(generateAdapter = true)
data class OmdbResponse(
    @Json(name = "Title") val title: String? = null,
    @Json(name = "Year") val year: String? = null,
    @Json(name = "Rated") val rated: String? = null,
    @Json(name = "Released") val released: String? = null,
    @Json(name = "Runtime") val runtime: String? = null,
    @Json(name = "Genre") val genre: String? = null,
    @Json(name = "Director") val director: String? = null,
    @Json(name = "Writer") val writer: String? = null,
    @Json(name = "Actors") val actors: String? = null,
    @Json(name = "Plot") val plot: String? = null,
    @Json(name = "Awards") val awards: String? = null,
    @Json(name = "Poster") val poster: String? = null,
    @Json(name = "Ratings") val ratings: List<OmdbRating>? = null,
    @Json(name = "imdbRating") val imdbRating: String? = null,
    @Json(name = "imdbVotes") val imdbVotes: String? = null,
    @Json(name = "imdbID") val imdbId: String? = null,
    @Json(name = "Response") val response: String? = null,
    @Json(name = "Error") val error: String? = null
) {
    val rottenTomatoesScore: String?
        get() = ratings?.firstOrNull { it.source == "Rotten Tomatoes" }?.value
}

data class ImdbInfo(
    val id: String,
    val title: String,
    val year: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    val votes: String? = null,
    val rt: String? = null,
    val genre: String? = null,
    val runtime: String? = null,
    val director: String? = null,
    val actors: String? = null,
    val rated: String? = null,
    val awards: String? = null,
    val img: String? = null,
    val omdbError: String? = null
)
