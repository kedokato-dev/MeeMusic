
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
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Repository.SongRepository
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
    private val ACTION_CLOSE = "com.kedokato_dev.meemusic.ACTION_CLOSE"

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
            addAction(ACTION_CLOSE)
        }
        registerReceiver(notificationActionReceiver, intentFilter)
    }

    private var isLoopEnabled = false
    private var isShuffleEnabled = false

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
            "NEXT", ACTION_NEXT -> {
                // Xử lý chuyển bài hát tiếp theo
                playNextSong()
            }
            "PREVIOUS" ,ACTION_PREVIOUS -> {
                // Xử lý quay lại bài hát trước
                playPreviousSong()
            }
            ACTION_CLOSE -> closeService()

            "LOOP_ON" -> {
                isLoopEnabled = true
                broadcastEvent("LOOP_ON")
                // Setting the repeat mode on ExoPlayer
                exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE

            }
            "LOOP_OFF" -> {
                isLoopEnabled = false
                broadcastEvent("LOOP_OFF")
                // Turning off repeat mode
                exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF

            }
            "SHUFFLE_ON" -> {
                isShuffleEnabled = true
                broadcastEvent("SHUFFLE_ON")

            }
            "SHUFFLE_OFF" -> {
                isShuffleEnabled = false
                broadcastEvent("SHUFFLE_OFF")

            }
        }

        return START_NOT_STICKY
    }


    private fun playNextSong() {
        CoroutineScope(Dispatchers.IO).launch {
            val nextSong = if (isShuffleEnabled) {
                // Play a random song if shuffle is enabled
                SongRepository().playRandomSong()
            } else {
                // Play next song in sequence, or the same song if loop is enabled
                if (isLoopEnabled) {
                    // For single song loop, just restart the current song
                    currentSongId?.let { SongRepository().getSongs()?.find { song -> song.id == currentSongId } }
                } else {
                    // Normal next song behavior
                    SongRepository().getNextSong(currentSongId)
                }
            }

            nextSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.source)
                    Log.d("nextSong", "🎵 Bài hát tiếp theo: ${it.title}")
                    currentSongTitle = it.title
                    currentSongArtist = it.artist
                    currentSongImage = it.image
                    currentSongId = it.id

                    val intent = Intent("MUSIC_EVENT")
                    intent.putExtra("ACTION", "NEXT")
                    intent.putExtra("SONG_ID", it.id)
                    intent.putExtra("SONG_TITLE", it.title)
                    intent.putExtra("SONG_ARTIST", it.artist)
                    intent.putExtra("SONG_IMAGE", it.image)
                    intent.putExtra("SONG_SOURCE", it.source)
                    intent.putExtra("SONG_ALBUM", it.album)
                    sendBroadcast(intent)

                    updateNotification()
                }
            }
        }
    }

    private fun playPreviousSong() {

        CoroutineScope(Dispatchers.IO).launch {
            val previousSong = if (isShuffleEnabled) {
                // Play a random song if shuffle is enabled
                SongRepository().playRandomSong()
            } else {
                // Play next song in sequence, or the same song if loop is enabled
                if (isLoopEnabled) {
                    // For single song loop, just restart the current song
                    currentSongId?.let { SongRepository().getSongs()?.find { song -> song.id == currentSongId } }
                } else {
                    // Normal next song behavior
                    SongRepository().getPreviousSong(currentSongId)
                }
            }

            previousSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.source)
                    Log.d("previousSong", "🎵 Bài hát trước: ${it.title}")
                    currentSongTitle = it.title
                    currentSongArtist = it.artist
                    currentSongImage = it.image
                    currentSongId = it.id

                    val intent = Intent("MUSIC_EVENT")
                    intent.putExtra("ACTION", "PREVIOUS")
                    intent.putExtra("SONG_ID", it.id)
                    intent.putExtra("SONG_TITLE", it.title)
                    intent.putExtra("SONG_ARTIST", it.artist)
                    intent.putExtra("SONG_IMAGE", it.image)
                    intent.putExtra("SONG_SOURCE", it.source)
                    intent.putExtra("SONG_ALBUM", it.album)
                    sendBroadcast(intent)

                    updateNotification()
                }
            }
        }
    }

    private fun closeService() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        stopForeground(true)
        stopSelf()
    }


    private fun broadcastEvent(action: String) {
        val intent = Intent("MUSIC_EVENT")
        intent.putExtra("ACTION", action)
        // Include current state for loop and shuffle
        if (action == "LOOP_ON" || action == "LOOP_OFF" ||
            action == "SHUFFLE_ON" || action == "SHUFFLE_OFF") {
            intent.putExtra("LOOP_ENABLED", isLoopEnabled)
            intent.putExtra("SHUFFLE_ENABLED", isShuffleEnabled)
        }
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
                            if (isLoopEnabled) {
                                // If loop is enabled, replay the same song
                                exoPlayer?.seekTo(0)
                                exoPlayer?.playWhenReady = true
                            } else {
                                // Otherwise play next song (which will use shuffle if enabled)
                                playNextSong()
                            }
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

                var albumArt: Bitmap? = null

                // Load album art image
                currentSongImage?.let { imageUrl ->
                    albumArt = loadAlbumArt(imageUrl)
                }

                val notification = createNotification(albumArt)

                startForeground(NOTIFICATION_ID, notification)

                // If music is paused, remove foreground but keep notification visible
                if (!isPlaying) {
                    stopForeground(STOP_FOREGROUND_DETACH)

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
            // Add parameter to indicate coming from notification
            putExtra("FROM_NOTIFICATION", true)
            // Also add current song ID to ensure we open the right detail screen
            putExtra("SONG_ID", currentSongId)
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

        val closeAction = NotificationCompat.Action(
            R.drawable.close48,
            "Close",
            createPendingIntent(ACTION_CLOSE)
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
            .addAction(closeAction)
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
                ACTION_PREVIOUS -> playPreviousSong()
                ACTION_NEXT -> playNextSong()
                ACTION_CLOSE -> closeService()
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