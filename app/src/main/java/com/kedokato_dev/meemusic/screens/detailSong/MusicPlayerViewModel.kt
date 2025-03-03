package com.kedokato_dev.meemusic.screens.detailSong

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayerViewModel: ViewModel() {
    private var exoPlayer: ExoPlayer? = null

    fun initializePlayer(context: Context) {
        exoPlayer = ExoPlayer.Builder(context).build()
    }

    fun playSong(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}