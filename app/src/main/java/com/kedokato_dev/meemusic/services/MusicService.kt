package com.kedokato_dev.meemusic

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val channelId = "music_notification_channel"
    private var songTitle = "Mee Music"


    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { stopSelf() }
        }
        createNotificationChannel()
    }

    // Trong MusicService.kt
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                "PLAY" -> {
                    val songPath = incomingIntent.getStringExtra("SONG_PATH")
                    songTitle = incomingIntent.getStringExtra("SONG_TITLE").toString()
                    if (songPath != null) {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.stop()
                            mediaPlayer.reset()
                        }
                        mediaPlayer.setDataSource(songPath)
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener {
                            mediaPlayer.start()
                            showNotification() // Cập nhật Notification khi nhạc bắt đầu phát
                        }
                    } else if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                        showNotification()
                    }
                }
                "PAUSE" -> {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        showNotification() // 🎶 Cập nhật trạng thái phát/tạm dừng
                    }
                }
                "STOP" -> stopSelf()
            }
        }
        return START_STICKY
    }




    @SuppressLint("ForegroundServiceType")
    private fun showNotification() {
        val isPlaying = mediaPlayer.isPlaying
        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24
            else R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            if (isPlaying) "Pause" else "Play",
            getPendingIntent(if (isPlaying) "PAUSE" else "PLAY")
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Stop",
            getPendingIntent("STOP")
        ).build()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.mee_music_logo)
            .setContentTitle("MeeMusic")
            .setContentText(songTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
            )
            .build()

        startForeground(1, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mee Music",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification channel for music playback control by Mee Music"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}