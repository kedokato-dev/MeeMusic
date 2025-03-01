package com.kedokato_dev.meemusic.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {
    private val _message = MutableStateFlow("Welcome to Home Screen")
    val message: StateFlow<String> = _message
}
