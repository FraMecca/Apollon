package com.apollon.classes

import android.support.v4.media.MediaMetadataCompat
import java.io.Serializable

open class Song(val id: String, val title: String, val artist: String, val img_url: String) : Serializable {

    constructor(id: String, title: String, artist: String) :
            this(id, title, artist,"https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")


    fun millisToString(millis: Int) : String{
        val seconds = millis/1000
        return "" + seconds/60 + ":" + String.format("%02d", seconds%60) // 2 digits precision - 0 for padding
    }

    override fun equals(other: Any?): Boolean {
        if(other !is Song)
            return false
        return this.id == other.id
    }
}