package com.kedokato_dev.meemusic.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val homeRepository: SongRepository) : ViewModel() {
//    private val _message = MutableStateFlow("Welcome to Home Screen")
//    val message: StateFlow<String> = _message

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song


//    fun loadUser(userId: Int) {
//        viewModelScope.launch {
//            try {
//                _song.value = homeRepository.getSongs()
//            } catch (e: Exception) {
//                // Xử lý lỗi (ví dụ: hiển thị thông báo lỗi)
//                println("Error: ${e.message}")
//            }
//        }
//    }


}
