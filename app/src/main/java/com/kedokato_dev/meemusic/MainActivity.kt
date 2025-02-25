package com.kedokato_dev.meemusic

import android.R
import android.R.id
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedokato_dev.meemusic.ui.theme.MeeMusicTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeeMusicTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current // Lấy Context hiện tại

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mee Music",
                        color = Color.Blue,
                        fontSize = 40.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp)
        ) {
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Hello", Toast.LENGTH_SHORT)
                            .show() // Sử dụng context
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),


                    ) {
                    Text(text = "Favorite", modifier = Modifier.padding(end = 10.dp))
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(24.dp)
                    )

                }
                loadImage()


            }
        }
    }
}

@Composable
fun loadImage() {
    Image(
        painter = painterResource(R.drawable.),
        contentDescription = "Camera",
    )

}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    MeeMusicTheme {
        MainScreen()
    }
}
