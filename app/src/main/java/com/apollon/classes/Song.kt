package com.apollon.classes

import java.io.Serializable

open class Song(val id: String, val title: String, val artist: String, var img_url: String) : Serializable {

    constructor(id: String, title: String, artist: String) :
            this(id, title, artist,"")


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

class PlaylistSong(id: String, title: String, artist:String, img_url: String) : Song(id, title, artist, img_url)