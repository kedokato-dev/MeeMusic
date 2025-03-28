package com.kedokato_dev.meemusic.screens.home

import SongViewModel
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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

    LaunchedEffect(Unit) {
        // Force refresh current song data when returning to this screen
        mainViewModel.refreshCurrentSongData(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Song list takes most of the screen
            Box(modifier = Modifier.weight(1f)) {
                HomeContent(viewModel, navController)
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
                                putExtra("SEEK_POSITION", 0L)
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
fun HomeContent(viewModel: SongViewModel, navController: NavController) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)

    LaunchedEffect(Unit) {
        viewModel.fetchSongs()
    }

    when (val result = state) {
        is SongState.Loading -> LoadingScreen()
        is SongState.Success -> CategorySongList(
            randomSongs = result.randomSongs,
            heartbreakSongs = result.heartbreakSongs,
            cheerfulSongs = result.cheerfulSongs,
            relaxingSongs = result.relaxingSongs,
            reflectiveSongs = result.reflectiveSongs,
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
    }
}

@Composable
fun CategorySongList(
    randomSongs: List<Song>,
    heartbreakSongs: List<Song>,
    cheerfulSongs: List<Song>,
    relaxingSongs: List<Song>,
    reflectiveSongs: List<Song>,
    navController: NavController,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Featured random songs section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bài Hát Đề Xuất",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(randomSongs) { song ->
                    FeatureSongItem(song, onSongClick)
                }
            }
        }

        // Heartbreak category
        item {
            CategorySection(
                title = "Thất Tình",
                songs = heartbreakSongs,
                onSongClick = onSongClick
            )
        }

        // Cheerful category
        item {
            CategorySection(
                title = "Vui Tươi",
                songs = cheerfulSongs,
                onSongClick = onSongClick
            )
        }

        // Relaxing category
        item {
            CategorySection(
                title = "Thư Giãn",
                songs = relaxingSongs,
                onSongClick = onSongClick
            )
        }

        // Reflective category
        item {
            CategorySection(
                title = "Suy",
                songs = reflectiveSongs,
                onSongClick = onSongClick
            )
        }

        // Add space at the bottom for mini player
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
fun CategorySection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                CategorySongItem(song, onSongClick)
            }
        }
    }
}

@Composable
fun FeatureSongItem(song: Song, onSongClick: (Song) -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp),
        onClick = { onSongClick(song) },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.image)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                placeholder = painterResource(R.drawable.mee_music_logo),
                error = painterResource(R.drawable.mee_music_logo),
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategorySongItem(song: Song, onSongClick: (Song) -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp),
        onClick = { onSongClick(song) },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.image)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                placeholder = painterResource(R.drawable.mee_music_logo),
                error = painterResource(R.drawable.mee_music_logo),
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
    val progress by animateLottieCompositionAsState(composition)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MeeMusic đang tải dữ liệu...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}