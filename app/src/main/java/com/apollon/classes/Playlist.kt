package com.apollon.classes

import java.io.Serializable

sealed class Playlist( val id: String, val title: String, val img_url: String) : Serializable {

    class Begin(): Playlist("begin", "start screen", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
    class AllAlbums(): Playlist("AllAlbums", "Albums", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png") // TODO: put good images
    class AllArtists(): Playlist("AllArtists", "Artists", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
    class AllGenres(): Playlist("AllGenres", "Genres", "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")

    class Artist( id: String, title: String, img_url: String) : Playlist(id, title, img_url) {
        constructor(id: String, title: String) : this(id, title, "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
    }
    class Album( id: String, title: String, img_url: String) : Playlist(id, title, img_url) {
        constructor(id: String, title: String) : this(id, title, "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
    }
    class Genre( id: String, title: String, img_url: String) : Playlist(id, title, img_url) {
        constructor(id: String, title: String) : this(id, title, "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
    }
}
