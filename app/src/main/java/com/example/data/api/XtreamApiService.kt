package com.example.data.api

import com.example.data.model.XtreamAuthResponse
import com.example.data.model.XtreamCategory
import com.example.data.model.XtreamLiveStream
import com.example.data.model.XtreamSeriesItem
import com.example.data.model.XtreamVodStream
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamApiService {

    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String
    ): XtreamAuthResponse

    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getVodCategories(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getSeriesCategories(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_streams"
    ): List<XtreamLiveStream>

    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<XtreamVodStream>

    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_series"
    ): List<XtreamSeriesItem>

    @GET
    suspend fun getVodInfoRaw(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: String
    ): ResponseBody

    @GET
    suspend fun getSeriesInfoRaw(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): ResponseBody
}
