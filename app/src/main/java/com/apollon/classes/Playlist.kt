package com.apollon.classes

import java.io.Serializable

sealed class Playlist( val id: String, val title: String, val img_url: String, val elements: Int) : Serializable {

    class Begin : Playlist("begin", "start screen", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png", 0)
    class AllAlbums : Playlist("AllAlbums", "", "album", 0)
    class AllArtists: Playlist("AllArtists", "", "artist",0)
    class AllGenres : Playlist("AllGenres", "", "genre", 0)
    class AllPlaylists : Playlist("AllPlaylists", "", "genre", 0)
    class Favourites : Playlist("Favourites", "Favourites", "favourites", 0)

    class Artist( id: String, title: String, img_url: String, elements: Int) : Playlist(id, title, img_url, elements) {
        constructor(id: String, title: String, elements: Int) : this(id, title, "artist", elements)
    }
    class Album( id: String, title: String, img_url: String, elements: Int) : Playlist(id, title, img_url, elements) {
        constructor(id: String, title: String, elements: Int) : this(id, title, "album", elements)
    }
    class Genre( id: String, title: String, img_url: String) : Playlist(id, title, img_url, 0) {
        constructor(id: String, title: String) : this(id, title, "genre")
    }
    class Custom( id: String, title: String, elements: Int) : Playlist(id, title, "genre", elements)
}
