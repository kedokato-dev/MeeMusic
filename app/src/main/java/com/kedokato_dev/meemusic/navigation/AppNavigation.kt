package com.kedokato_dev.meemusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.google.gson.Gson
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.screens.detailSong.DetailSongScreen
import com.kedokato_dev.meemusic.screens.home.HomeScreen
import com.kedokato_dev.meemusic.screens.library.LibraryScreen
import com.kedokato_dev.meemusic.screens.setting.SettingScreen
import com.kedokato_dev.meemusic.ui.screens.SearchScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier // Thêm modifier vào đây
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("search") {
            SearchScreen()
        }
        composable("settings") {
            SettingScreen()
        }
        composable("library") {
            LibraryScreen()
        }
        composable(
            route = "detailSong/{songJson}",
            arguments = listOf(navArgument("songJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val songJson = backStackEntry.arguments?.getString("songJson")?.let { encodedJson ->
                URLDecoder.decode(encodedJson, StandardCharsets.UTF_8.toString())
            } ?: return@composable

            val song = Gson().fromJson(songJson, Song::class.java)
            DetailSongScreen(song = song)
        }
    }
}
