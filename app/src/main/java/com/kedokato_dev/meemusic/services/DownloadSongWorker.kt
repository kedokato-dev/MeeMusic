package com.kedokato_dev.meemusic.services

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadSongWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val songUrl = inputData.getString("song_url") ?: return Result.failure()
        val songTitle = inputData.getString("song_title") ?: "unknown_song"
        val songId = inputData.getString("song_id") ?: "0"

        return try {
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MeeMusic")
            downloadsDir.mkdirs() // Tạo thư mục nếu chưa có

            val fileName = "${songTitle.replace(" ", "_")}_$songId.mp3"
            val file = File(downloadsDir, fileName)

            downloadFile(songUrl, file)

            // Gửi Broadcast để thông báo tải xong
            val intent = Intent("MUSIC_EVENT")
            intent.putExtra("ACTION", "DOWNLOAD_COMPLETE")
            intent.putExtra("SONG_ID", songId)
            intent.putExtra("FILE_PATH", file.absolutePath)
            applicationContext.sendBroadcast(intent)

            Log.d("DownloadSongWorker", "Tải xong: ${file.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadSongWorker", "Lỗi tải bài hát: ${e.message}")
            Result.retry() // Thử lại nếu có lỗi
        }
    }

    private fun downloadFile(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()

        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            outputStream.write(buffer, 0, len)
        }

        outputStream.close()
        inputStream.close()
    }
}
