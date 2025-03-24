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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.kedokato_dev.meemusic.navigation.MainScreen
import com.kedokato_dev.meemusic.screens.MainViewModel
import com.kedokato_dev.meemusic.ui.theme.MeeMusicTheme
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

                // If coming from notification, navigate to detail screen
                LaunchedEffect(fromNotification) {
                    if (fromNotification && songId != null) {
                        // Get the current song from MainViewModel
                        val currentSong = mainViewModel.currentSong.value

                        // If we have the current song and it matches the ID
                        if (currentSong != null && currentSong.id == songId) {
                            // Navigate to detail with fromMiniPlayer=true to prevent restart
                            val songJson = Gson().toJson(currentSong)
                            val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                            navController.navigate("detailSong/$encodedSongJson?fromMiniPlayer=true") {
                                // Clear backstack to home and make this the only destination
                                popUpTo("home") { inclusive = false }
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