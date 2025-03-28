package com.kedokato_dev.meemusic.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SearchViewModel(private val repository: SongRepository) : ViewModel() {
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults
    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchSongs(query: String) {
        // Cancel previous search if still running
        searchJob?.cancel()

        // Start new search with delay
        searchJob = viewModelScope.launch {
            delay(300) // Delay to avoid excessive calls while typing
            val results = repository.searchSongs(query)
            _searchResults.value = results ?: emptyList()
        }
    }
}