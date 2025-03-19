package com.kedokato_dev.meemusic.screens.detailSong

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kedokato_dev.meemusic.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class MusicPlayerViewModel : ViewModel() {
    private var exoPlayer: ExoPlayer? = null
    private var currentUrl: String? = null  // Lưu URL bài hát hiện tại
    var isDragging = mutableStateOf(false)

    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun initializePlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        duration.longValue = maxOf(exoPlayer?.duration ?: 0L, 0L)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }
    }

    fun playSong(context: Context, url: String, title: String) {
        exoPlayer?.let { player ->
            if (currentUrl != url) {
                val mediaItem = MediaItem.fromUri(url.toUri())
                player.setMediaItem(mediaItem)
                player.prepare()
                currentUrl = url
            }
            player.play()
            _isPlaying.value = true
            startProgressTracking()

            val intent = Intent(context, MusicService::class.java).apply {
                action = "PLAY"
                putExtra("SONG_PATH", url) // Truyền URL bài hát
                putExtra("SONG_TITLE", title) // Truyền tiêu đề bài hát
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }

        }
    }

    fun pauseSong(context: Context) {
        exoPlayer?.pause()
        _isPlaying.value = false

//        // Gửi Intent để tạm dừng MusicService
//        val serviceIntent = Intent(context, MusicService::class.java).apply {
//            action = "PAUSE"
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(serviceIntent)
//        }
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while (true) {
                if (exoPlayer?.isPlaying == true && !isDragging.value) {
                    currentPosition.value = exoPlayer?.currentPosition ?: 0L
                }
                delay(500)
            }
        }
    }

    fun releasePlayer(context: Context) {
        exoPlayer?.release()
        exoPlayer = null
        currentUrl = null
        _isPlaying.value = false

//        // Dừng MusicService
//        val serviceIntent = Intent(context, MusicService::class.java).apply {
//            action = "STOP"
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(serviceIntent)
//        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition.value = position
    }

    override fun onCleared() {
        super.onCleared()
        // Không gọi releasePlayer ở đây nếu bạn muốn giữ MusicService chạy nền
    }
}