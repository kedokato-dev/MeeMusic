package com.kedokato_dev.meemusic.screens.home


import SongViewModel
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.kedokato_dev.meemusic.R
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Repository.SongRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@SuppressLint("SuspiciousIndentation")
@Composable
fun HomeScreen(navController: NavController) {
    val repository = remember { SongRepository() }
    val viewModel: SongViewModel = viewModel { SongViewModel(repository) } // Tạo trực tiếp
    SongScreen(viewModel, navController)
}

@Composable
fun SongScreen(viewModel: SongViewModel = viewModel(), navController: NavController) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchSongs()
    }

    when (val result = state) {
        is SongState.Loading -> LoadingScreen()
        is SongState.Success -> SongList(songs = result.songs, navController)
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
fun SongList(songs: List<Song>, navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs) { song ->
            SongItem(song, navController)
        }
    }
}


@Composable
fun SongItem(song: Song, navController: NavController) {
    val context = LocalContext.current;
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
            onClick = {
                val songJson = Gson().toJson(song)
                val encodedSongJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
                navController.navigate("detailSong/$encodedSongJson") // Sử dụng trực tiếp route mới
            },
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
                    verticalArrangement = Arrangement.Center
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator( // Hiệu ứng vòng tròn loading
            modifier = Modifier.size(50.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MeeMusic đang tải dữ liệu...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreView() {
    val fakeNav = rememberNavController()
    SongItem(song = Song(
        id = "1",
        title = "Title",
        album = "Album",
        artist = "Artist",
        source = "Source",
        image = "Image",
        duration = 100,
        favorite = false,
        counter = 0,
        replay = 0
    ), navController = fakeNav)
}