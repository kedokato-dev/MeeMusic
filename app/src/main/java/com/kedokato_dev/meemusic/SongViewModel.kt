package com.kedokato_dev.meemusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SongState {
    object Loading : SongState()
    data class Success(val songs: List<Song>) : SongState()
    data class Error(val message: String) : SongState()
}

class SongViewModel : ViewModel() {
    private val _state = MutableStateFlow<SongState>(SongState.Loading)
    val state: StateFlow<SongState> = _state

    init {
        fetchSongs()
    }

    private fun fetchSongs() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.songApi.getSongs()
                _state.value = SongState.Success(response.songs)
            } catch (e: Exception) {
                _state.value = SongState.Error("Lỗi tải dữ liệu: ${e.message}")
            }
        }
    }

}