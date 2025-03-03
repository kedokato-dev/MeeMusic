package com.kedokato_dev.meemusic.navigation

import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Models.toJson

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object DetailSong : Screen("detailSong/{songId}") {
        fun createRoute(song: Song) = "detailSong/${song.toJson()}"
    }
}

