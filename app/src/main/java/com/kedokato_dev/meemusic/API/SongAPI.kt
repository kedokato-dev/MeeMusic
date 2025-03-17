package com.kedokato_dev.meemusic.API

import com.kedokato_dev.meemusic.Models.Song
import retrofit2.http.GET

data class SongResponse(
    val status: String,
    val songs: List<Song>
)

interface SongAPI {
    @GET("resources/braniumapis/songs.json")
    suspend fun getSongs() : SongResponse
}


