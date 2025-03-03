package com.kedokato_dev.meemusic.screens.detailSong

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kedokato_dev.meemusic.Models.Song


@Composable
fun DetailSongScreen(song: Song) {
    Column {
        Text(text = song.title)
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun PreviewDetailSongScreen() {
//    DetailSongScreen()
}