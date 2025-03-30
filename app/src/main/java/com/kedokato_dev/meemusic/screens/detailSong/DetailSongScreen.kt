package com.kedokato_dev.meemusic.screens.detailSong

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.MusicService
import com.kedokato_dev.meemusic.R
import com.kedokato_dev.meemusic.Repository.SongRepository
import com.kedokato_dev.meemusic.screens.MainViewModel
import kotlin.compareTo

@Composable
fun DetailSongScreen(song: Song, fromMiniPlayer: Boolean = false, navController: NavController) {
    val context = LocalContext.current
    val musicPlayerViewModel: MusicPlayerViewModel = viewModel()
    val mainViewModel: MainViewModel =
        viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)
    val currentSongInMainViewModel by mainViewModel.currentSong.collectAsState()
    val isPlayingInMainViewModel by mainViewModel.isPlaying.collectAsState()

    // Sử dụng state để theo dõi bài hát hiện tại
    var currentDisplayedSong by remember { mutableStateOf(song) }

    LaunchedEffect(Unit) {
        // Register position receiver in both cases
        musicPlayerViewModel.registerPositionReceiver(context)

        // Check if the current song is already loaded
        val isSameSong = currentSongInMainViewModel?.id == song.id

        // Only start/resume playback if not coming from MiniPlayer
        if (!fromMiniPlayer) {
            if (!isSameSong) {
                // If it's a new song, play from beginning
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "PLAY"
                    putExtra("SONG_PATH", song.source)
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_IMAGE", song.image)
                    putExtra("SONG_ID", song.id)
                }
                context.startService(intent)
                mainViewModel.updateCurrentSong(song)
                mainViewModel.updatePlaybackState(true)
            } else if (!isPlayingInMainViewModel) {
                // If same song but paused, resume playback
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "RESUME"
                }
                context.startService(intent)
                mainViewModel.updatePlaybackState(true)
            }
        }
    }

    DisposableEffect(Unit) {
        val songUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "MUSIC_EVENT") {
                    val action = intent.getStringExtra("ACTION")
                    if (action == "NEXT" || action == "PREVIOUS") {
                        // Lấy thông tin bài hát mới
                        val songId = intent.getStringExtra("SONG_ID") ?: return
                        val songTitle = intent.getStringExtra("SONG_TITLE") ?: return
                        val songArtist = intent.getStringExtra("SONG_ARTIST") ?: return
                        val songImage = intent.getStringExtra("SONG_IMAGE") ?: return
                        val songSource = intent.getStringExtra("SONG_SOURCE") ?: return
                        val songAlbum = intent.getStringExtra("SONG_ALBUM") ?: return

                        // Tạo đối tượng Song mới
                        val updatedSong = Song(
                            id = songId,
                            title = songTitle,
                            artist = songArtist,
                            image = songImage,
                            source = songSource,
                            album = songAlbum,
                            duration = currentDisplayedSong.duration,
                            favorite = currentDisplayedSong.favorite,
                            counter = currentDisplayedSong.counter,
                            replay = currentDisplayedSong.replay
                        )

                        // Cập nhật UI với bài hát mới
                        currentDisplayedSong = updatedSong

                        // Cập nhật trong MainViewModel
                        mainViewModel.updateCurrentSong(updatedSong)
                    } else if (action == "DOWNLOAD_COMPLETE") {
                        val filePath = intent.getStringExtra("FILE_PATH")
                        Toast.makeText(context, "Lưu nhạc thành công ❤ ", Toast.LENGTH_LONG).show()
                        // Update UI or perform any necessary actions
                    }
                }
            }
        }

        // Đăng ký receiver
        val filter = IntentFilter("MUSIC_EVENT")
        ContextCompat.registerReceiver(
            context,
            songUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(songUpdateReceiver)
        }
    }

    // Hiển thị giao diện người dùng
    PlaySong(
        song = currentDisplayedSong,
        musicPlayerViewModel = musicPlayerViewModel,
        navController
    )

}


@Composable
fun PlaySong(song: Song, musicPlayerViewModel: MusicPlayerViewModel, navController: NavController) {
    val context = LocalContext.current
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    var isFavorite by remember { mutableStateOf(false) }
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }

    LaunchedEffect(song.image) {
        isFavorite = SongRepository().isFavoriteSong(context, song.id)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Back button positioned at the top left
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.keyboard_backspace),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = {
                    Toast.makeText(context, "Đang tải bài hát ${song.title}", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = "DOWNLOAD_SONG"
                        putExtra("SONG_ID", song.id)
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }


        }

        // Main content in the center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 56.dp) // Add extra padding at top to account for back button
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

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.artist,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )

                IconButton(
                    onClick = {
                        isFavorite = !isFavorite
                        val intent = Intent(context, MusicService::class.java).apply {
                            action = if (isFavorite) "ADD_TO_FAVORITES" else "REMOVE_FROM_FAVORITES"
                            putExtra("SONG_ID", song.id)
                        }
                        context.startService(intent)
                        Toast.makeText(
                            context,
                            if (isFavorite) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Favorite/Unfavorite",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFE91E63),
                    )
                }


            }



            Spacer(modifier = Modifier.height(20.dp))

            MusicControls(
                isPlaying = isPlaying,
                onPlayPause = {
                    if (isPlaying) {
                        musicPlayerViewModel.pauseSong(context)
                    } else {
                        if (musicPlayerViewModel.currentPosition.longValue > 0) {
                            musicPlayerViewModel.resumeSong(context)
                        } else {
                            musicPlayerViewModel.playSong(context, song)
                        }
                    }
                },
                onNext = {
                    musicPlayerViewModel.playNextSong(context)
                },
                onPrevious = {
                    musicPlayerViewModel.playPreviousSong(context)
                },
                progress = musicPlayerViewModel.progress,
                duration = musicPlayerViewModel.duration,
                onSeek = { newProgress ->
                    val newPosition =
                        (newProgress * musicPlayerViewModel.duration.longValue).toLong()
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
) {
    var isLoopEnabled by remember { mutableStateOf(false) }
    var isRandomEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly, // Căn đều khoảng cách giữa các nút
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isLoopEnabled = !isLoopEnabled
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = if (isLoopEnabled) "LOOP_ON" else "LOOP_OFF"
                    }
                    context.startService(intent)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isLoopEnabled) R.drawable.repeat_one else R.drawable.repeat
                    ),
                    contentDescription = "Loop",
                    tint = if (isLoopEnabled) Color(0xFFFCFFFC) else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Previous",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White) // Thêm nền trắng
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
                    modifier = Modifier.size(64.dp),
                    tint = Color.Black
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Next",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    isRandomEnabled = !isRandomEnabled
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = if (isRandomEnabled) "SHUFFLE_ON" else "SHUFFLE_OFF"
                    }
                    context.startService(intent)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (isRandomEnabled) Color(0xFF03A9F4) else Color.White,
                    modifier = Modifier.size(48.dp)
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
        ), musicPlayerViewModel, NavController(LocalContext.current)
    )
}

@SuppressLint("DefaultLocale")
private fun formatTime(timeMs: Long): String {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}