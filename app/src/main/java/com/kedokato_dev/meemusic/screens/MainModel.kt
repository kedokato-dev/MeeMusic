package com.kedokato_dev.meemusic.screens

import androidx.lifecycle.ViewModel
import com.kedokato_dev.meemusic.Models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    // Bài hát đang phát
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    // Trạng thái phát nhạc
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun updateCurrentSong(song: Song?) {
        _currentSong.value = song
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }
}