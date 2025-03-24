package com.kedokato_dev.meemusic.screens.home

import SongViewModel
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.kedokato_dev.meemusic.MusicService
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.R
import com.kedokato_dev.meemusic.Repository.SongRepository
import com.kedokato_dev.meemusic.components.MiniPlayer
import com.kedokato_dev.meemusic.screens.MainViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("SuspiciousIndentation")
@Composable
fun HomeScreen(navController: NavController) {
    val repository = remember { SongRepository() }
    val viewModel: SongViewModel = viewModel { SongViewModel(repository) }
    val mainViewModel: MainViewModel = viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val context = LocalContext.current

    // Register the broadcast receiver to listen for song changes
    DisposableEffect(context) {
        mainViewModel.registerMusicEventReceiver(context)
        onDispose {
            mainViewModel.unregisterMusicEventReceiver(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Song list takes most of the screen
            Box(modifier = Modifier.weight(1f)) {
                SongScreen(viewModel, navController)
            }

            // Mini player at the bottom if a song is playing
            currentSong?.let {
                MiniPlayer(
                    song = it,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            // Pause music
                            val intent = Intent(context, MusicService::class.java).apply {
                                action = "PAUSE"
                            }
                            context.startService(intent)
                            mainViewModel.updatePlaybackState(false)
                        } else {
                            // Resume music
                            val intent = Intent(context, MusicService::class.java).apply {
                                action = "RESUME"
                                putExtra("SEEK_POSITION", 0L) // This can be improved by storing current position
                            }
                            context.startService(intent)
                            mainViewModel.updatePlaybackState(true)
                        }
                    },
                    onPlayerClick = { song ->
                        // Navigate to detail screen without restarting playback
                        val songJson = Gson().toJson(song)
                        val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                        navController.navigate("detailSong/$encodedSongJson?fromMiniPlayer=true") {
                            launchSingleTop = true
                        }
                    },
                    onNextClick = {
                        // Play next song
                        val intent = Intent(context, MusicService::class.java).apply {
                            action = "NEXT"
                        }
                        context.startService(intent)
                        mainViewModel.updatePlaybackState(true)
                    },
                    onPreviousClick = {
                       // Play previous song
                        val intent = Intent(context, MusicService::class.java).apply {
                            action = "PREVIOUS"
                        }
                        context.startService(intent)
                        mainViewModel.updatePlaybackState(true)
                    }
                )
            }
        }
    }
}

@Composable
fun SongScreen(viewModel: SongViewModel = viewModel(), navController: NavController) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)

    LaunchedEffect(Unit) {
        viewModel.fetchSongs()
    }

    when (val result = state) {
        is SongState.Loading -> LoadingScreen()
        is SongState.Success -> SongList(
            songs = result.songs,
            navController = navController,
            onSongClick = { song ->
                // Start playing the song and navigate to detail
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "PLAY"
                    putExtra("SONG_PATH", song.source)
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_IMAGE", song.image)
                    putExtra("SONG_ID", song.id)
                }
                context.startService(intent)

                // Update MainViewModel
                mainViewModel.updateCurrentSong(song)
                mainViewModel.updatePlaybackState(true)

                // Navigate to detail screen
                val songJson = Gson().toJson(song)
                val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                navController.navigate("detailSong/$encodedSongJson")
            }
        )
        is SongState.Error -> Text(
            text = result.message,
            modifier = Modifier.padding(16.dp),
            color = Color.Red
        )
        else -> {
            Text(
                text = "Không có dữ liệu!",
                modifier = Modifier.padding(16.dp),
                color = Color.Red
            )
        }
    }
}

@Composable
fun SongList(songs: List<Song>, navController: NavController, onSongClick: (Song) -> Unit) {
    // Add padding at the bottom to ensure content isn't covered by mini player
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        items(songs) { song ->
            SongItem(song, onSongClick)
        }
        // Add some space at the bottom to prevent content being hidden by mini player
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
fun SongItem(song: Song, onSongClick: (Song) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = { onSongClick(song) },
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row {
                LoadImage(url = song.image)
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun LoadImage(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        placeholder = painterResource(R.drawable.mee_music_logo),
        error = painterResource(R.drawable.mee_music_logo),
        contentDescription = "Ảnh tải từ mạng",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(50.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(15.dp))
    )
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MeeMusic đang tải dữ liệu...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}