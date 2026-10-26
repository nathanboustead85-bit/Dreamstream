package com.dreamstream.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

data class LiveCategory(
    val category_id: String? = null,
    val category_name: String? = null,
    val parent_id: Int? = null
)

data class VodCategory(
    val category_id: String? = null,
    val category_name: String? = null,
    val parent_id: Int? = null
)

data class SeriesCategory(
    val category_id: String? = null,
    val category_name: String? = null,
    val parent_id: Int? = null
)

interface XtreamApi {

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<LiveCategory>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<VodCategory>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<SeriesCategory>
}
