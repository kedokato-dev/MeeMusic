
package com.kedokato_dev.meemusic

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "music_channel"
    private val ACTION_PLAY = "com.kedokato_dev.meemusic.ACTION_PLAY"
    private val ACTION_PAUSE = "com.kedokato_dev.meemusic.ACTION_PAUSE"
    private val ACTION_PREVIOUS = "com.kedokato_dev.meemusic.ACTION_PREVIOUS"
    private val ACTION_NEXT = "com.kedokato_dev.meemusic.ACTION_NEXT"

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private val binder = LocalBinder()

    private var currentSongTitle: String? = null
    private var currentSongArtist: String? = null
    private var currentSongImage: String? = null
    private var currentSongId: String? = null
    private var isPlaying = false

    private var exoPlayer: ExoPlayer? = null


    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        // Create notification channel for Android O and above
        createNotificationChannel()

        // Create media session
        mediaSession = MediaSessionCompat(this, "MeeMusicSession")

        // Register broadcast receiver for notification actions
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_NEXT)
        }
        registerReceiver(notificationActionReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> {
                val songPath = intent.getStringExtra("SONG_PATH")
                currentSongTitle = intent.getStringExtra("SONG_TITLE")
                currentSongArtist = intent.getStringExtra("SONG_ARTIST")
                currentSongImage = intent.getStringExtra("SONG_IMAGE")
                currentSongId = intent.getStringExtra("SONG_ID")

                songPath?.let { playSong(it) }
            }
            "PAUSE", ACTION_PAUSE -> pauseSong()
            "RESUME", ACTION_PLAY -> resumeSong()
            "SEEK_POSITION" -> {
                val position = intent.getLongExtra("POSITION", 0)
                exoPlayer?.seekTo(position)

                // Update notification with current state
                updateNotification()
            }
            ACTION_NEXT -> {
                // Xử lý chuyển bài hát tiếp theo
                broadcastEvent("NEXT")
            }
            ACTION_PREVIOUS -> {
                // Xử lý quay lại bài hát trước
                broadcastEvent("PREVIOUS")
            }
        }
        return START_NOT_STICKY
    }


    private fun broadcastEvent(action: String) {
        val intent = Intent("MUSIC_EVENT")
        intent.putExtra("ACTION", action)
        sendBroadcast(intent)
    }

    private fun playSong(path: String) {
        // Giải phóng player cũ (nếu có)
        exoPlayer?.release()

        // Gửi sự kiện rằng đang tải bài hát
        broadcastEvent("LOADING")

        // Khởi tạo ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(path)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            this@MusicService.isPlaying = true
                            startPositionUpdates()
//                            Toast.makeText(this@MusicService, "Position: hello", Toast.LENGTH_SHORT).show()
                            updateNotification()
                            broadcastEvent("LOADED")
                        }
                        Player.STATE_ENDED -> {
                            broadcastEvent("COMPLETED")
                        }
                    }
                }
            })
        }
    }


    private fun pauseSong() {
        exoPlayer?.playWhenReady = false
        isPlaying = false
        updateNotification()
        broadcastEvent("PAUSED")
    }

    private fun resumeSong() {
        exoPlayer?.playWhenReady = true
        isPlaying = true
        updateNotification()
        broadcastEvent("RESUMED")
    }

    private var isUpdating = false

    private fun startPositionUpdates() {

        if (isUpdating) return
        isUpdating = true

        CoroutineScope(Dispatchers.IO).launch {
            while (isUpdating && exoPlayer != null) {
                try {
                    val position: Long
                    val duration: Long

                    withContext(Dispatchers.Main) { // Chuyển về Main Thread để lấy dữ liệu
                        position = exoPlayer?.currentPosition ?: 0L
                        duration = exoPlayer?.duration ?: 0L
                    }

                    val intent = Intent("POSITION_UPDATE").apply {
                        putExtra("POSITION", position)
                        putExtra("DURATION", duration)
                    }
                    sendBroadcast(intent)

                    delay(500)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error in position updates: ${e.message}")
                }
            }
        }

    }

    private fun stopPositionUpdates() {
        isUpdating = false
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Player"
            val descriptionText = "Music player controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Default bitmap if image loading fails
                var albumArt: Bitmap? = null

                // Load album art image
                currentSongImage?.let { imageUrl ->
                    albumArt = loadAlbumArt(imageUrl)
                }

                val notification = createNotification(albumArt)

                startForeground(NOTIFICATION_ID, notification)

                // If music is paused, remove foreground but keep notification visible
                if (!isPlaying) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        stopForeground(false)
                    }
                    // Update notification even when paused
                    NotificationManagerCompat.from(this@MusicService).notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error updating notification: ${e.message}")
            }
        }
    }

    private suspend fun loadAlbumArt(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(this@MusicService)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = Coil.imageLoader(this@MusicService).execute(request)
            return@withContext result.drawable?.toBitmap()
        } catch (e: Exception) {
            Log.e("MusicService", "Error loading album art: ${e.message}")
            null
        }
    }

    private fun createNotification(albumArt: Bitmap?): Notification {
        // Create an intent for launching the app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause action
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        // Previous and Next actions
        val previousAction = NotificationCompat.Action(
            R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Previous",
            createPendingIntent(ACTION_PREVIOUS)
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Next",
            createPendingIntent(ACTION_NEXT)
        )

        // Build notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mee_music)
            .setContentTitle(currentSongTitle ?: "Unknown title")
            .setContentText(currentSongArtist ?: "Unknown artist")
            .setLargeIcon(albumArt)
            .setContentIntent(pendingContentIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(packageName)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> resumeSong()
                ACTION_PAUSE -> pauseSong()
                ACTION_PREVIOUS -> broadcastEvent("PREVIOUS")
                ACTION_NEXT -> broadcastEvent("NEXT")
            }
        }
    }

    override fun onDestroy() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        unregisterReceiver(notificationActionReceiver)
        super.onDestroy()
    }

}