package com.kedokato_dev.meemusic.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import com.google.gson.Gson

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val source: String,
    val image: String,
    val duration: Int,
    val favorite: Boolean,
    val counter: Int,
    val replay: Int
) : Parcelable

fun Song.toJson(): String {
    return Gson().toJson(this)
}
