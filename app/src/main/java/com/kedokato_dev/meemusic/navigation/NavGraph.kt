package com.kedokato_dev.meemusic.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.kedokato_dev.meemusic.screens.home.HomeScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
//        composable(
//            route = Screen.Detail.route,
//            arguments = listOf(navArgument("userId") { type = NavType.StringType })
//        ) { backStackEntry ->
//            DetailScreen(navController, backStackEntry)
//        }
    }
}
