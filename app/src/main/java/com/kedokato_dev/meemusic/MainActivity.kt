package com.kedokato_dev.meemusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.kedokato_dev.meemusic.navigation.AppNavHost
import com.kedokato_dev.meemusic.ui.theme.MeeMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeeMusicTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MeeMusicTheme {

    }
}