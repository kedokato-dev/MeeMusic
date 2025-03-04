package com.kedokato_dev.meemusic.screens.detailSong

import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.R

@Composable
fun DetailSongScreen(song: Song) {
    val context = LocalContext.current
    val musicPlayerViewModel : MusicPlayerViewModel = viewModel()

    LaunchedEffect(Unit) {
        musicPlayerViewModel.initializePlayer(context)
    }

    // Render UI
    PlaySong(song = song, musicPlayerViewModel)
}

@Composable
fun PlaySong(song: Song, musicPlayerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }



    LaunchedEffect(song.image) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(song.image)
            .allowHardware(false)
            .build()

        val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
        result?.let {
            val bitmap = it.toBitmap()
            Palette.from(bitmap).generate { palette ->
                val dominantColor =
                    palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color.DarkGray
                val secondaryColor = palette?.mutedSwatch?.rgb?.let { Color(it) } ?: Color.Black

                backgroundBrush = Brush.verticalGradient(listOf(dominantColor, secondaryColor))
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            Text(
                text = song.album,
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.roboto)),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            LoadImage(url = song.image)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.roboto))
            )
            Text(
                text = song.artist,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            MusicControls(
                isPlaying = isPlaying,
                onPlayPause = {
                    if (isPlaying) {
                        musicPlayerViewModel.pauseSong()
                    } else {
                        musicPlayerViewModel.playSong(song.source)
                    }
                    isPlaying = !isPlaying
                },
                onNext = {},
                onPrevious = {},
                progress = 0.5f,
                onSeek = {},
                song.duration
            )
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
        placeholder = painterResource(R.drawable.logo),
        error = painterResource(R.drawable.logo),
        contentDescription = "Ảnh tải từ mạng",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(20.dp))
    )
}

@Composable
fun MusicControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    progress: Float,
    onSeek: (Float) -> Unit,
    time: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
            ),
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.CenterHorizontally)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "00:00",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Left
            )

            Text(
                text = "${time}", color = Color.White,
                textAlign = TextAlign.Right
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Previous",
                    modifier = Modifier.size(50.dp)
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    painter = painterResource(id = if (isPlaying) R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24 else R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(60.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Next",
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreView() {
    val musicPlayerViewModel = MusicPlayerViewModel()
    PlaySong(
        song = Song(
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
        ), musicPlayerViewModel
    )
}
