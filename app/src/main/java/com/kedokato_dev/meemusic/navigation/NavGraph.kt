package com.kedokato_dev.meemusic.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.google.gson.Gson
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.screens.detailSong.DetailSongScreen
import com.kedokato_dev.meemusic.screens.home.HomeScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(
            route = Screen.DetailSong.route + "/{songId}", // Thêm tham số vào route
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songJson = backStackEntry.arguments?.getString("songId")?.let { encodedJson ->
                URLDecoder.decode(encodedJson, StandardCharsets.UTF_8.toString())
            } ?: return@composable
            val song = Gson().fromJson(songJson, Song::class.java)
            DetailSongScreen(song = song)
        }
    }
}

