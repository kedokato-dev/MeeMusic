package com.kedokato_dev.meemusic.navigation

import com.kedokato_dev.meemusic.R

sealed class NavigationItem(val route: String, val title: String, val icon: Int) {
    object Home : NavigationItem("home", "Home", R.drawable.home_24dp_e3e3e3_fill0_wght400_grad0_opsz24)
    object Search : NavigationItem("search", "Search", R.drawable.search_24dp_e3e3e3_fill0_wght400_grad0_opsz24)
    object Library : NavigationItem("library", "Library", R.drawable.music_note_24dp_e3e3e3_fill0_wght400_grad0_opsz24)
    object Settings : NavigationItem("settings", "Settings", R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24)

    companion object {
        val bottomNavItems = listOf(Home, Search, Library, Settings)
    }
}
