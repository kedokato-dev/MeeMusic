package com.kedokato_dev.meemusic.Repository

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
}
