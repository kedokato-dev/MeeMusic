package com.kedokato_dev.meemusic.screens.detailSong

import android.R.attr.action
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.text.toLong
import kotlin.times

@Composable
fun DetailSongScreen(song: Song) {
    val context = LocalContext.current
    val musicPlayerViewModel: MusicPlayerViewModel = viewModel()

    LaunchedEffect(Unit) {
        // Register position receiver
        musicPlayerViewModel.registerPositionReceiver(context)
        musicPlayerViewModel.playSong(context, song.source, song.title)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Clean up when screen is disposed
            musicPlayerViewModel.unregisterPositionReceiver(context)
        }
    }

    // Render UI
    PlaySong(song = song, musicPlayerViewModel = musicPlayerViewModel)
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
                        musicPlayerViewModel.pauseSong(context)
                    } else {
                        musicPlayerViewModel.playSong(context, song.source, song.title)
                    }
                },
                onNext = { /* TODO: Thêm logic chuyển bài tiếp theo */ },
                onPrevious = { /* TODO: Thêm logic quay lại bài trước */ },
                progress = musicPlayerViewModel.progress,
                duration = musicPlayerViewModel.duration,
                onSeek = { newProgress ->
                    val newPosition = (newProgress * musicPlayerViewModel.duration.longValue).toLong()
                    musicPlayerViewModel.currentPosition.longValue = newPosition
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
        placeholder = painterResource(R.drawable.mee_music_logo),
        error = painterResource(R.drawable.mee_music_logo),
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
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current

        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
            ),
            value = viewModel.progress,
            onValueChange = { newValue ->
                viewModel.isDragging.value = true
                viewModel.currentPosition.longValue = (newValue * duration.longValue).toLong()
            },
            onValueChangeFinished = {
                viewModel.seekTo(context, viewModel.currentPosition.longValue)
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
                text = formatTime(viewModel.currentPosition.longValue),
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
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Previous",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .scale(if (isPlaying) 1.1f else 1.0f)
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

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
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
private fun formatTime(timeMs: Long): String {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}