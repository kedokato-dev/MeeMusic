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
import com.kedokato_dev.meemusic.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    init {
        startPositionUpdater()
    }

    private fun startPositionUpdater() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_isPlaying.value && !isDragging.value) {
                    // Only update UI if playing and not dragging
                    currentPosition.longValue += 200
                }
                delay(200) // Update roughly 5 times per second
            }
        }
    }

    fun registerPositionReceiver(context: Context) {
        positionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "UPDATE_POSITION" -> {
                        currentPosition.longValue = intent.getLongExtra("CURRENT_POSITION", 0L)
                    }
                    "UPDATE_DURATION" -> {
                        duration.longValue = intent.getLongExtra("DURATION", 0L)
                    }
                    "PLAYBACK_STATE_CHANGED" -> {
                        _isPlaying.value = intent.getBooleanExtra("IS_PLAYING", false)
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("UPDATE_POSITION")
            addAction("UPDATE_DURATION")
            addAction("PLAYBACK_STATE_CHANGED")
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

    fun playSong(context: Context, url: String, title: String) {
        if (url == currentSongUrl && lastPlayedPosition > 0) {
            // Resume from last position if it's the same song
            resumeSong(context)
        } else {
            // Start new song
            currentSongUrl = url
            sendCommandToService(context, "PLAY", url, title)
            _isPlaying.value = true
            currentPosition.longValue = 0
        }
    }

    private fun resumeSong(context: Context) {
        sendCommandToService(context, "RESUME", position = lastPlayedPosition)
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
        sendCommandToService(context, "SEEK", position = position)
        currentPosition.longValue = position
        lastPlayedPosition = position
    }

    private fun sendCommandToService(
        context: Context, action: String, url: String? = null,
        title: String? = null, position: Long? = null
    ) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            url?.let { putExtra("SONG_PATH", it) }
            title?.let { putExtra("SONG_TITLE", it) }
            position?.let { putExtra("SEEK_POSITION", it) }
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