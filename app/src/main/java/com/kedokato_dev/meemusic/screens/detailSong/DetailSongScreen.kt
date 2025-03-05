package com.kedokato_dev.meemusic.screens.detailSong

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import okhttp3.internal.concurrent.formatDuration

@Composable
fun DetailSongScreen(song: Song) {
    val context = LocalContext.current
    val musicPlayerViewModel: MusicPlayerViewModel = viewModel()

    LaunchedEffect(Unit) {
        musicPlayerViewModel.initializePlayer(context)
        musicPlayerViewModel.playSong(song.source)
    }

    // Render UI
    PlaySong(song = song, musicPlayerViewModel)

}

@Composable
fun PlaySong(song: Song, musicPlayerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }

    val currentPosition by musicPlayerViewModel.currentPosition
    val duration by musicPlayerViewModel.duration

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
                val dominantColor = palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color.DarkGray
                val secondaryColor = palette?.mutedSwatch?.rgb?.let { Color(it) } ?: Color.Black

                backgroundBrush = Brush.verticalGradient(listOf(dominantColor, secondaryColor))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = song.album,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.alpha(0.85f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            LoadImage(song.image)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = song.title,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = song.artist,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
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

                },
                onNext = {},
                onPrevious = {},
                progress = if (musicPlayerViewModel.duration.value > 0)
                    musicPlayerViewModel.currentPosition.value.toFloat() / musicPlayerViewModel.duration.value
                else 0f,
                duration = musicPlayerViewModel.duration,
                onSeek = { newProgress ->
                    val newPosition = (newProgress * musicPlayerViewModel.duration.value).toLong()
                    musicPlayerViewModel.seekTo(newPosition)
                },
                time = song.duration,
                viewModel = musicPlayerViewModel
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
    duration: MutableLongState,
    onSeek: (Float) -> Unit,
    time: Int,
    viewModel: MusicPlayerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thanh trượt bài hát
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
            ),
            value = if (duration.value > 0) viewModel.currentPosition.value / duration.value.toFloat() else 0f,
            onValueChange = { newValue ->
                viewModel.isDragging.value = true
                viewModel.currentPosition.value = (newValue * duration.value).toLong()
            },
            onValueChangeFinished = {
                viewModel.seekTo(viewModel.currentPosition.value)
                viewModel.isDragging.value = false
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )


        // Hiển thị thời gian nhạc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(viewModel.currentPosition.value),
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = formatTime(viewModel.duration.longValue),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Điều khiển nhạc
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Nút Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Previous",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            // Nút Play/Pause có hiệu ứng
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .scale(if (isPlaying) 1.1f else 1.0f) // Hiệu ứng khi bấm
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying)
                            R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                        else
                            R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                    ),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }

            // Nút Next
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Next",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
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

@SuppressLint("DefaultLocale")
fun formatTime(timeMs: Long): String {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

