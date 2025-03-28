package com.kedokato_dev.meemusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.google.gson.Gson
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.screens.MainViewModel
import com.kedokato_dev.meemusic.screens.detailSong.DetailSongScreen
import com.kedokato_dev.meemusic.screens.home.HomeScreen
import com.kedokato_dev.meemusic.screens.library.LibraryScreen
import com.kedokato_dev.meemusic.screens.setting.SettingScreen
import com.kedokato_dev.meemusic.ui.screens.SearchScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    val mainViewModel: MainViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier // Thêm modifier vào đây
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("search") {
          SearchScreen(
              onSongClick = { song ->
                  val songJson = Gson().toJson(song)
                  val encodedJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                  navController.navigate("detailSong/$encodedJson?fromMiniPlayer=false")
              }
          )
        }
        composable("settings") {
            SettingScreen()
        }
        composable("library") {
            LibraryScreen()
        }
        composable(
            "detailSong/{songJson}?fromMiniPlayer={fromMiniPlayer}",
            arguments = listOf(
                navArgument("songJson") { type = NavType.StringType },
                navArgument("fromMiniPlayer") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val songJson = backStackEntry.arguments?.getString("songJson") ?: ""
            val fromMiniPlayer = backStackEntry.arguments?.getBoolean("fromMiniPlayer") ?: false
            val decodedJson = URLDecoder.decode(songJson, StandardCharsets.UTF_8.toString())
            val song = Gson().fromJson(decodedJson, Song::class.java)
            DetailSongScreen(song = song, fromMiniPlayer = fromMiniPlayer, navController)
        }
    }
}
