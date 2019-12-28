package com.apollon.classes

import java.io.Serializable

sealed class Playlist( val id: String, val title: String, val img_url: String, val elements: Int) : Serializable {

    class Begin : Playlist("begin", "start screen", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png", 0)
    class AllAlbums : Playlist("AllAlbums", "", "album", 0)
    class AllArtists : Playlist("AllArtists", "", "artist", 0)
    class AllGenres : Playlist("AllGenres", "", "genre", 0)
    class Favourites : Playlist("Favourites", "", "favourites", 0)

    class Artist( id: String, title: String, img_url: String) : Playlist(id, title, img_url, 0) {
        constructor(id: String, title: String) : this(id, title, "artist")
    }
    class Album( id: String, title: String, img_url: String) : Playlist(id, title, img_url, 0) {
        constructor(id: String, title: String) : this(id, title, "album")
    }
    class Genre( id: String, title: String, img_url: String) : Playlist(id, title, img_url, 0) {
        constructor(id: String, title: String) : this(id, title, "genre")
    }
}
