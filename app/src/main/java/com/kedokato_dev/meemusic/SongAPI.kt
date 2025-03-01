package com.kedokato_dev.meemusic

import retrofit2.http.GET

interface SongAPI {
    @GET("resources/braniumapis/songs.json")
    suspend fun getSongs(): SongResponse
}