package com.kedokato_dev.meemusic.navigation

import com.kedokato_dev.meemusic.R

enum class AppNavigation(val route: String) {
    HOME("home"),
    SEARCH("search"),
    LIBRARY("library"),
    SETTINGS("settings")
}

sealed class NagavitionItem(
    val route: AppNavigation,
    val title: String,
    val icon: Int
) {
    object Home : NagavitionItem(
        AppNavigation.HOME, "Home",
        R.drawable.home_24dp_e3e3e3_fill0_wght400_grad0_opsz24
    )
    object Search : NagavitionItem(
        AppNavigation.SEARCH, "Search",
        R.drawable.search_24dp_e3e3e3_fill0_wght400_grad0_opsz24
    )
    object Library : NagavitionItem(
        AppNavigation.LIBRARY, "Library",
        R.drawable.music_note_24dp_e3e3e3_fill0_wght400_grad0_opsz24
    )
    object Settings : NagavitionItem(
        AppNavigation.SETTINGS, "Settings",
        R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24
    )
}