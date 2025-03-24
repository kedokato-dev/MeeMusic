package com.kedokato_dev.meemusic

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.kedokato_dev.meemusic.Repository.SongRepository
import com.kedokato_dev.meemusic.navigation.MainScreen
import com.kedokato_dev.meemusic.screens.MainViewModel
import com.kedokato_dev.meemusic.ui.theme.MeeMusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val fromNotification = intent.getBooleanExtra("FROM_NOTIFICATION", false)
        val songId = intent.getStringExtra("SONG_ID")

        setContent {
            MeeMusicTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                val currentSong by mainViewModel.currentSong.collectAsState()

                // If coming from notification, navigate to detail screen
                LaunchedEffect(fromNotification, songId) {
                    if (fromNotification && songId != null) {
                        // First check if the song is already in the ViewModel
                        if (currentSong != null && currentSong?.id == songId) {
                            // Navigate with existing song data
                            val songJson = Gson().toJson(currentSong)
                            val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                            navController.navigate("detailSong/$encodedSongJson?fromMiniPlayer=true") {
                                popUpTo("home") { inclusive = false }
                            }
                        } else {
                            // If song isn't in ViewModel, fetch it from repository
                            withContext(Dispatchers.IO) {
                                val song = SongRepository().getSongById(songId)
                                if (song != null) {
                                    withContext(Dispatchers.Main) {
                                        // Update ViewModel with the song
                                        mainViewModel.updateCurrentSong(song)

                                        // Navigate with fetched song data
                                        val songJson = Gson().toJson(song)
                                        val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                                        navController.navigate("detailSong/$encodedSongJson?fromMiniPlayer=true") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(navController = navController)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MeeMusicTheme {
        // Preview content
    }
}