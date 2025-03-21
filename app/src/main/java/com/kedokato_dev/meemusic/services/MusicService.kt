package com.kedokato_dev.meemusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private val channelId = "music_notification_channel"
    private var songTitle = "Mee Music"
    private var positionUpdateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        stopForeground(true)
                        stopSelf()
                    }

                    // Broadcast playback state changes
                    sendBroadcast(Intent("PLAYBACK_STATE_CHANGED").apply {
                        putExtra("IS_PLAYING", isPlaying)
                    })
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    // Broadcast playback state changes
                    sendBroadcast(Intent("PLAYBACK_STATE_CHANGED").apply {
                        putExtra("IS_PLAYING", playWhenReady)
                    })
                }
            })
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                "PLAY" -> playSong(it)
                "PAUSE" -> pauseSong()
                "RESUME" -> resumeSong(it)
                "STOP" -> stopSong()
                "SEEK" -> seekTo(it.getLongExtra("SEEK_POSITION", 0L))
            }
        }
        return START_STICKY
    }

    private fun playSong(intent: Intent) {
        val songPath = intent.getStringExtra("SONG_PATH") ?: return
        songTitle = intent.getStringExtra("SONG_TITLE") ?: "Mee Music"

        exoPlayer.stop()
        val mediaItem = MediaItem.fromUri(songPath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        showNotification()
        startPositionUpdater()
    }

    private fun resumeSong(intent: Intent) {
        val position = intent.getLongExtra("SEEK_POSITION", 0L)
        if (position > 0) {
            exoPlayer.seekTo(position)
        }
        exoPlayer.play()
        showNotification()
        startPositionUpdater()
    }

    private fun pauseSong() {
        exoPlayer.pause()
        positionUpdateJob?.cancel()
        showNotification()
    }

    private fun stopSong() {
        exoPlayer.stop()
        positionUpdateJob?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        // Send broadcast to update position in ViewModel
        sendBroadcast(Intent("UPDATE_POSITION").apply {
            putExtra("CURRENT_POSITION", position)
        })
    }

    private fun startPositionUpdater() {
        // Cancel any existing job
        positionUpdateJob?.cancel()

        // Start a new position update job
        positionUpdateJob = serviceScope.launch {
            // Chờ đến khi có giá trị duration hợp lệ
            while (withContext(Dispatchers.Main) { exoPlayer.duration } <= 0 && isActive) {
                delay(100)
            }

            withContext(Dispatchers.Main) {
                sendBroadcast(Intent("UPDATE_DURATION").apply {
                    putExtra("DURATION", exoPlayer.duration)
                })
            }

            // Regularly update position
            while (isActive) {
                if (withContext(Dispatchers.Main) { exoPlayer.isPlaying }) {
                    withContext(Dispatchers.Main) {
                        sendBroadcast(Intent("UPDATE_POSITION").apply {
                            putExtra("CURRENT_POSITION", exoPlayer.currentPosition)
                        })
                    }
                }
                delay(500) // Update position every 500ms
            }
        }
    }


    private fun showNotification() {
        val isPlaying = exoPlayer.isPlaying
        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24 else R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            if (isPlaying) "Pause" else "Play",
            getPendingIntent(if (isPlaying) "PAUSE" else "RESUME")
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24, "Stop", getPendingIntent("STOP")
        ).build()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.mee_music_logo)
            .setContentTitle("MeeMusic")
            .setContentText(songTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
            .build()

        startForeground(1, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Mee Music", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Music playback control"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        positionUpdateJob?.cancel()
        exoPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}