package com.apollon.classes

class StreamingSong(val url:String, val duration: Int, id:String, title: String, artist: String, img_url:String) :
    Song(id, title, artist, img_url) {
    constructor(url: String, duration:Int, id: String, title: String, artist: String) :
            this(url, duration, id, title, artist,"https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
}