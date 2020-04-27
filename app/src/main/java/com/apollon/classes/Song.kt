package com.apollon.classes

import java.io.Serializable

open class Song(val id: String, val title: String, val artist: String, var img_url: String) : Serializable {

    constructor(id: String, title: String, artist: String) :
            this(id, title, artist, "")

    override fun equals(other: Any?): Boolean {
        if (other !is Song)
            return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class PlaylistSong(id: String, title: String, artist: String, img_url: String) : Song(id, title, artist, img_url)
