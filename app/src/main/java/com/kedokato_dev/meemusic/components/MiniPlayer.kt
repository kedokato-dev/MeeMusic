package com.kedokato_dev.meemusic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.R

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPlayerClick: (Song) -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit
) {
    song?.let { currenntSong ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.DarkGray)
                .clickable { onPlayerClick(currenntSong) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currenntSong.image)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text =currenntSong.title,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currenntSong.artist,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPreviousClick) {
                    Icon(
                        painter = painterResource(
                                R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                        ),
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying)
                                R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            else
                                R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                        ),
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNextClick) {
                    Icon(
                        painter = painterResource(
                            R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                        ),
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }
    }
}


