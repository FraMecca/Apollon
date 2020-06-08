package com.apollon.classes

class StreamingSong(val url: String, val duration: Int, id: String, title: String, artist: String, img_url: String) :
    Song(id, title, artist, img_url, 0) {
    constructor(url: String, duration: Int, id: String, title: String, artist: String) :
            this(url, duration, id, title, artist, "")
}
