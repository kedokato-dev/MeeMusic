package com.kedokato_dev.meemusic.screens.detailSong

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                        duration.value = maxOf(exoPlayer?.duration ?: 0L, 0L)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }
    }



    fun playSong(url: String) {
        exoPlayer?.let { player ->
            if (currentUrl != url) {
                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                player.setMediaItem(mediaItem)
                player.prepare()
                currentUrl = url
            }
            player.play()
            _isPlaying.value = true // ✅ Cập nhật trạng thái
            startProgressTracking()
        }
    }

    fun pauseSong() {
        exoPlayer?.pause()
        _isPlaying.value = false // ✅ Cập nhật trạng thái
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


    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        currentUrl = null
        _isPlaying.value = false
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition.value = position
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
