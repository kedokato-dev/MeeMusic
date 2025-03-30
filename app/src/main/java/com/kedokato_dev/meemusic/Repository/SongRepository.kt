package com.kedokato_dev.meemusic.Repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Global.putString
import android.util.Log
import com.kedokato_dev.meemusic.API.RetrofitClient
import com.kedokato_dev.meemusic.Models.Song

class SongRepository {
    suspend fun getSongs(): List<Song>? {
        return try {
            Log.d("SongRepository", "📢 Đang gọi API lấy danh sách bài hát...")
            val response= RetrofitClient.songApi.getSongs()
            Log.d("SongRepository", "✅ API trả về: ${response} bài hát")
            response.songs
        } catch (e: Exception) {
            Log.e("SongRepository", "❌ Lỗi khi gọi API: ${e.message}", e)
            null
        }
    }

    suspend fun getNextSong(currentSongId: String?): Song? {
        val songs = getSongs()
        if (songs.isNullOrEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex < songs.size - 1) {
            songs[currentIndex + 1]
        } else {
            songs.firstOrNull()
        }
    }

    suspend fun getPreviousSong(currentSongId: String?): Song? {
        val songs = getSongs()
        if (songs.isNullOrEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex > 0) {
            songs[currentIndex - 1]
        } else {
            songs.lastOrNull()
        }
    }

    suspend fun playRandomSong(): Song? {
        val songs = getSongs()
        if (songs.isNullOrEmpty()) return null

        return songs.random()
    }

    suspend fun getSongById(songId: String): Song? {
        return getSongs()?.find { it.id == songId }
    }

    suspend fun searchSongs(query: String): List<Song>? {
        if (query.isEmpty()) return emptyList()

        // Get all songs first
        val allSongs = getSongs() ?: return null

        // Filter songs by title or artist containing the query (case insensitive)
        return allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true)
        }
    }

    // Complete the saveFavoriteSong method and add other needed methods
    @SuppressLint("UseKtx")
    fun saveFavoriteSong(context: Context, songId: String) {
        val sharedPreferences = context.getSharedPreferences("favorite_songs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(songId, songId)
        editor.apply()
    }

    @SuppressLint("UseKtx")
    fun removeFromFavorites(context: Context, songId: String) {
        val sharedPreferences = context.getSharedPreferences("favorite_songs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(songId)
        editor.apply()
    }

    fun isFavoriteSong(context: Context, songId: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("favorite_songs", Context.MODE_PRIVATE)
        return sharedPreferences.contains(songId)
    }

    suspend fun getFavoriteSongs(context: Context): List<Song> {
        val sharedPreferences = context.getSharedPreferences("favorite_songs", Context.MODE_PRIVATE)
        val favoriteIds = sharedPreferences.all.keys
        val allSongs = getSongs() ?: return emptyList()

        return allSongs.filter { song ->
            favoriteIds.contains(song.id)
        }
    }




}
