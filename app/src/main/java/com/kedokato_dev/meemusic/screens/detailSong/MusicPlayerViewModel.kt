package com.kedokato_dev.meemusic.screens.detailSong

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {

    var isDragging = mutableStateOf(false)
    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)
    private var lastPlayedPosition = 0L
    private var currentSongUrl: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var positionUpdateReceiver: BroadcastReceiver? = null

    val progress: Float
        get() = if (duration.longValue > 0) currentPosition.longValue.toFloat() / duration.longValue else 0f

    fun registerPositionReceiver(context: Context) {
        positionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "POSITION_UPDATE" -> {
                        if (!isDragging.value) {
                            val position = intent.getLongExtra("POSITION", 0L)
                            val audioDuration = intent.getLongExtra("DURATION", 0L)

                            currentPosition.longValue = position
                            duration.longValue = audioDuration
                        }
                    }
                    "MUSIC_EVENT" -> {
                        when (intent.getStringExtra("ACTION")) {
                            "LOADING" -> {
                                _isPlaying.value = false
                            }
                            "LOADED" -> _isPlaying.value = true
                            "PAUSED" -> _isPlaying.value = false
                            "RESUMED" -> _isPlaying.value = true
                            "COMPLETED" -> {
                                _isPlaying.value = false
                                currentPosition.longValue = 0
                            }
                            "NEXT" -> {
                                currentPosition.longValue = 0
                               _isPlaying.value = true
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("POSITION_UPDATE")
            addAction("MUSIC_EVENT")
        }
        ContextCompat.registerReceiver(
            context,
            positionUpdateReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }



    fun unregisterPositionReceiver(context: Context) {
        positionUpdateReceiver?.let {
            context.unregisterReceiver(it)
            positionUpdateReceiver = null
        }
    }

    fun playSong(context: Context,  song: Song) {
        Intent(context, MusicService::class.java).also { intent ->
            intent.action = "PLAY"
            intent.putExtra("SONG_PATH", song.source)
            intent.putExtra("SONG_TITLE", song.title)
            intent.putExtra("SONG_ARTIST", song.artist)
            intent.putExtra("SONG_IMAGE", song.image)
            intent.putExtra("SONG_ID", song.id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun playNextSong(context: Context) {
        sendCommandToService(context, "NEXT")
        _isPlaying.value = false
    }

    fun playPreviousSong(context: Context) {
        sendCommandToService(context, "PREVIOUS")
        _isPlaying.value = false
    }

     fun resumeSong(context: Context) {
        sendCommandToService(context, "RESUME")
        _isPlaying.value = true
    }

    fun pauseSong(context: Context) {
        lastPlayedPosition = currentPosition.longValue
        sendCommandToService(context, "PAUSE")
        _isPlaying.value = false
    }

    fun stopSong(context: Context) {
        sendCommandToService(context, "STOP")
        _isPlaying.value = false
        currentPosition.longValue = 0
        lastPlayedPosition = 0
    }

    fun seekTo(context: Context, position: Long) {
        sendCommandToService(context, "SEEK_POSITION", position = position)
        currentPosition.longValue = position
        lastPlayedPosition = position
    }

    private fun sendCommandToService(
        context: Context, action: String, position: Long? = null
    ) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            position?.let { putExtra("POSITION", it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }


    override fun onCleared() {
        super.onCleared()
        // Clean up resources
    }
}