package com.example.data.api

import com.example.data.model.OmdbResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface OmdbApiService {

    @GET
    suspend fun getOmdbById(
        @Url url: String = "https://www.omdbapi.com/",
        @Query("i") imdbId: String,
        @Query("plot") plot: String = "full",
        @Query("apikey") apiKey: String
    ): OmdbResponse

    @GET
    suspend fun getOmdbByTitle(
        @Url url: String = "https://www.omdbapi.com/",
        @Query("t") title: String,
        @Query("y") year: Int? = null,
        @Query("plot") plot: String = "full",
        @Query("apikey") apiKey: String
    ): OmdbResponse

    @GET
    suspend fun getImdbSuggestions(
        @Url url: String
    ): ResponseBody

    @GET
    suspend fun translateText(
        @Url url: String
    ): ResponseBody
}
