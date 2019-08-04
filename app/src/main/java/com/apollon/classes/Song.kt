package com.apollon.classes

import java.io.Serializable

class Song(val id: Int, val title: String, val artist: String, val img_url: String, val audio_url: String, val duration: Int) : Serializable {

    constructor(id: Int, title: String, artist: String, audio_url: String, duration: Int) :
            this(id, title, artist,"https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png",audio_url, duration)

    constructor(id: Int, title: String, artist: String, img_url: String, audio_url: String) : this(id, title, artist, img_url, audio_url, 373000)

    fun millisToString(millis: Int) : String{
        val seconds = millis/1000
        return "" + seconds/60 + ":" + String.format("%02d", seconds%60) // 2 digits precision - 0 for padding
    }
}