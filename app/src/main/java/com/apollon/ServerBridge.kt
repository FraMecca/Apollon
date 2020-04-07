package com.apollon

import android.os.AsyncTask
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.apollon.classes.*
import com.apollon.fragments.LoginFragment
import org.json.JSONArray
import org.json.JSONObject

sealed class RequestResult {
    class Ok(val result: JSONObject) : RequestResult()
    class Error(val msg: String) : RequestResult()
}

fun makeRequest(m: Map<String, Any>): RequestResult {
    val (user, pass) = Credentials.get()
    val params = hashMapOf<String, Any>("user" to user,
            "password" to pass)

    m.forEach { (k, v) -> params.put(k, v) }
    val data = JSONObject(params).toString()
    try {
        with(baseurl().openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            this.outputStream.write(data.toByteArray())
            this.outputStream.flush()
            this.outputStream.close()
            inputStream.bufferedReader().use {
                val llines = it.lines().toArray()
                assert(llines.count() == 1)
                val j = JSONObject(llines[0] as String)
                if (j["response"] == "error")
                    return RequestResult.Error(j["msg"] as String)
                else
                    return RequestResult.Ok(j)
            }
        }
    } catch (e: Exception) {
        return RequestResult.Error("Can't make the connection: " + e.message)
    }
}

fun baseurl(): URL {
    val (ip, snd) = Credentials.getServer()
    val (proto, port) = snd
    val split = ip.split('/')
    val hostname = split[0]
    val file = ip.substring(hostname.length, ip.length)

    val protoString: String = when (proto) {
        0 -> "http"
        1 -> "https"
        else -> {
            assert(false); "https"
        }
    }
    Log.e("GOTURL:", URL(protoString, hostname, port, file).toString())
    return URL(protoString, hostname, port, file)
}


abstract class ApollonAsync : AsyncTask<Void, Int, List<Playlist>>() {
    var result = ArrayList<Playlist>()
    var error = ""
}

abstract class SongAsync : AsyncTask<Void, Int, List<Song>>() {
    var result = ArrayList<Song>()
    var error = ""
}

abstract class OperationAsync : AsyncTask<Void, Int, String?>() {
    var result: String? = null
}

private class AllAlbums : ApollonAsync() {

    override fun doInBackground(vararg params: Void?): List<Playlist.Album>? {
        val ret = ArrayList<Playlist.Album>()
        Log.e("HTTP", "request: all-by-album")

        val resp = makeRequest(hashMapOf("action" to "all-by-album"))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString())

                for (i in 0..values.length() - 1) {
                    val album: JSONObject = values[i] as JSONObject
                    // TODO : uri, title + artist
                    Log.e("ALBUM", album.toString())
                    if ((album["img"] as String) != "")
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String, album["img"] as String, album["#nsongs"] as Int))
                    else
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String, album["#nsongs"] as Int))
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished all-albums")
        return if (resp is RequestResult.Ok) {
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            if (ret.isEmpty())
                error = "No Playlists"
            ret
        } else
            null
    }
}

private class AllArtists : ApollonAsync() {

    override fun doInBackground(vararg params: Void?): List<Playlist.Artist>? {
        val ret = ArrayList<Playlist.Artist>()
        Log.e("HTTP", "request: all-by-artist")

        val resp = makeRequest(hashMapOf("action" to "all-by-artist"))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString())

                for (i in 0..values.length() - 1) {
                    val artist: JSONObject = values[i] as JSONObject

                    // TODO : uri, title + artist
                    if ((artist["img"] as String) != "")
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["img"] as String, artist["#albums"] as Int))
                    else
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["#albums"] as Int))
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished all-artists")
        return if (resp is RequestResult.Ok) {
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            if (ret.isEmpty())
                error = "No Playlists"
            ret
        } else
            null
    }
}

private class AllGenres : ApollonAsync() {

    override fun doInBackground(vararg params: Void?): List<Playlist.Genre>? {
        val ret = ArrayList<Playlist.Genre>()
        Log.e("HTTP", "request: all-by-genre")

        val resp = makeRequest(hashMapOf("action" to "all-by-genre"))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString())

                for (i in 0..values.length() - 1) {
                    val genre: String = values[i] as String
                    ret.add(Playlist.Genre(i.toString(), genre)) // TODO: correct id
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished all-Genre")
        if (ret.size != 0) {
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            return ret
        } else
            return null
    }
}

private class AllPlaylists : ApollonAsync() {

    override fun doInBackground(vararg params: Void?): List<Playlist>? {
        val ret = ArrayList<Playlist.Custom>()
        Log.e("HTTP", "request: list-playlists")

        val resp = makeRequest(hashMapOf("action" to "list-playlists"))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                var img: String
                val values = j["result"] as JSONArray
                Log.e("HTTP", values.length().toString())

                for (i in 0..values.length() - 1) {
                    val playlist: JSONObject = values[i] as JSONObject
                    Log.e("HTTP", playlist.toString())
                    if (playlist["title"] != "Favourites") {
                        if ((playlist["uris"] as JSONArray).length() > 0)
                            img = ((playlist["uris"] as JSONArray)[0] as JSONObject)["img"] as String//TODO random?
                        else
                            img = "genre"//TODO correct name
                        ret.add(Playlist.Custom(playlist["title"] as String, playlist["title"] as String, img, playlist["#nsongs"] as Int))
                    }
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished all-playlists")

        return if (resp is RequestResult.Ok) {
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            if (ret.isEmpty())
                error = "No Playlists"
            ret
        } else
            null
    }
}

/*
private class SingleGenre(val name:String) : ApollonAsync(){

    override fun doInBackground(vararg params: Void?): ArrayList<Playlist.Artist>? {
        val ret = ArrayList<Playlist.Artist>()
        Log.e("HTTP", "request: single-genre")

        val resp = makeRequest(hashMapOf("action" to "genre", "key" to name))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val artists = (j["genre"] as JSONObject)["artists"] as JSONObject
               // val img = (j["artist"] as JSONObject)["img"] as String // TODO IMAGES
                val img = ""

                for (artist in artists.keys()){
                    if (img == "")
                        ret.add(Playlist.Artist(artist, artist))
                    else
                        ret.add(Playlist.Artist(artist, artist, img))
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished SingleArtist")
        if (ret.size != 0){
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}
*/

class SingleAlbum(val uri: String) : SongAsync() {

    override fun doInBackground(vararg params: Void?): ArrayList<Song>? {
        val ret = ArrayList<Song>()
        Log.e("HTTP", "request: single-album")

        val resp = makeRequest(hashMapOf("action" to "album", "key" to uri))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val songs = (j["album"] as JSONObject)["songs"] as JSONArray
                val artist = (j["album"] as JSONObject)["artist"] as String
                val img = (j["album"] as JSONObject)["img"] as String

                for (i in 0..songs.length() - 1) {
                    val jsong = songs[i] as JSONObject
                    val title: String = jsong["title"] as String

                    val uri = jsong["uri"] as String
                    if (img == "")
                        ret.add(Song(uri, title, artist))
                    else
                        ret.add(Song(uri, title, artist, img))
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished SingleAlbum")
        if (ret.size != 0) {
            result = ret
            return ret
        } else
            return null
    }
}

private class SingleArtist(val name: String) : ApollonAsync() {

    override fun doInBackground(vararg params: Void?): ArrayList<Playlist.Album>? {
        val ret = ArrayList<Playlist.Album>()
        Log.e("HTTP", "request: single-artist")

        val resp = makeRequest(hashMapOf("action" to "artist", "key" to name))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val albums = (j["artist"] as JSONObject)["albums"] as JSONArray
                //val artist = (j["artist"] as JSONObject)["name"] as String
                //val img = (j["artist"] as JSONObject)["img"] as String

                for (i in 0..albums.length() - 1) {
                    val jalbum = albums[i] as JSONObject
                    val title = jalbum["title"] as String
                    val img = jalbum["img"] as String
                    val uri = jalbum["uri"] as String
                    val nsongs = jalbum["#nsongs"] as Int
                    if (img == "")
                        ret.add(Playlist.Album(uri, title, nsongs))
                    else
                        ret.add(Playlist.Album(uri, title, img, nsongs))
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished SingleArtist")
        if (ret.size != 0) {
            @Suppress("UNCHECKED_CAST")
            result = ret as ArrayList<Playlist>
            return ret
        } else
            return null
    }
}

class SingleSong(val uri: String) : AsyncTask<Void, Int, StreamingSong>() {
    var result: StreamingSong? = null
    var error = ""

    override fun doInBackground(vararg params: Void?): StreamingSong? {
        Log.e("singleSong", uri)
        Log.e("HTTP", "request: single-song")

        val resp = makeRequest(hashMapOf("action" to "new-song", "quality" to Server.quality, "uri" to uri))
        result = when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val url = baseurl().toString().substring(0, baseurl().toString().length - 1) + (j["uri"] as String)
                Log.e("URL", url)
                val metadata = ((((j.get("metadata") as JSONObject).get("json") as JSONObject).get("media") as JSONObject)
                        .get("track") as JSONArray).get(0) as JSONObject
                val title = if (metadata.has("Title")) metadata["Title"] as String else metadata["Album"] as String
                val artist = metadata["Performer"] as String
                val fduration = metadata["Duration"] as String
                val duration = (fduration.toFloat() * 1000).toInt()
                val s = StreamingSong(url, duration, uri, title, artist)
                Log.e("HTTP", "new-song done")
                s
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        return result
    }
}

class SinglePlaylist(val title: String) : SongAsync() {

    override fun doInBackground(vararg params: Void?): ArrayList<Song>? {
        val ret = ArrayList<Song>()
        Log.e("HTTP", "request: get-playlist")

        val resp = makeRequest(hashMapOf("action" to "get-playlist", "title" to title))
        when (resp) {
            is RequestResult.Ok -> {
                val j = resp.result
                val songs = (j["result"] as JSONObject)["uris"] as JSONArray
                val title = title

                for (i in 0..songs.length() - 1) {
                    val jsong = songs[i] as JSONObject
                    val title: String = jsong["title"] as String
                    val artist: String = jsong["artist"] as String
                    val uri = jsong["uri"] as String
                    val img = jsong["img"] as String
                    ret.add(PlaylistSong(uri, title, artist, img))
                }
            }
            is RequestResult.Error -> {
                error = resp.msg; return null
            }
        }
        Log.e("HTTP", "Finished SinglePlaylist")
        return if (resp is RequestResult.Ok) {
            result = ret
            if (ret.isEmpty())
                error = "No Tracks"
            ret
        } else
            null
    }
}

class GetLyrics(val artist: String, val title: String) : AsyncTask<Void, Int, List<String>>() {
    var result: List<String>? = null
    var error: String = ""

    override fun doInBackground(vararg params: Void?): List<String>? {
        val ret: List<String>
        Log.e("HTTP", "request: lyrics")

        when (val resp = makeRequest(hashMapOf("action" to "lyrics", "artist" to artist, "song" to title))) {
            is RequestResult.Ok -> {
                val j = resp.result
                Log.e("JSON", j.toString())
                val content = j["lyrics"] as String
                ret = content.split("\r\n")
            }
            is RequestResult.Error -> {
                error = resp.msg; Log.e("JSON", resp.msg);result = listOf(""); return result
            }
        }
        Log.e("HTTP", "Finished GetLyrics")
        return if (ret.isNotEmpty()) {
            result = ret
            ret
        } else
            null
    }
}

class CreatePlaylist(var title: String) : OperationAsync() {
    override fun doInBackground(vararg p0: Void?): String? {
        when (val resp = makeRequest(hashMapOf<String, Any>("action" to "new-playlist", "title" to title, "uris" to emptyList<String>()))) {
            is RequestResult.Ok -> {
                Log.e("JSON", resp.result.toString())
                result = resp.result["response"] as String
                Server.resetPlaylists()
            }
            is RequestResult.Error -> {
                Log.e("JSON", resp.msg); result = resp.msg
            }
        }
        Log.e("HTTP", "Finished CreatePlaylist")
        return result
    }
}

class RemovePlaylist(var title: String) : OperationAsync() {
    override fun doInBackground(vararg p0: Void?): String? {
        when (val resp = makeRequest(hashMapOf("action" to "remove-playlist", "title" to title))) {
            is RequestResult.Ok -> {
                Log.e("JSON", resp.result.toString())
                result = resp.result["response"] as String
                Server.resetPlaylists()
                Server.dropPlaylist(title)
            }
            is RequestResult.Error -> {
                Log.e("JSON", resp.msg); result = resp.msg
            }
        }
        Log.e("HTTP", "Finished RemovePlaylist")
        return result
    }
}

class AddSong(var title: String, var uri: String) : OperationAsync() {
    override fun doInBackground(vararg p0: Void?): String? {
        when (val resp = makeRequest(hashMapOf("action" to "modify-playlist", "playlist-action" to "add", "title" to title, "uris" to listOf(uri)))) {
            is RequestResult.Ok -> {
                Log.e("JSON", resp.result.toString())
                result = resp.result["response"] as String
                Server.resetPlaylists()
                Server.dropPlaylist(title)
            }
            is RequestResult.Error -> {
                Log.e("JSON", resp.msg); result = resp.msg
            }
        }
        Log.e("HTTP", "Finished AddSong")
        return result
    }
}

class RemoveSong(var title: String, var uri: String) : OperationAsync() {
    override fun doInBackground(vararg p0: Void?): String? {
        when (val resp = makeRequest(hashMapOf("action" to "modify-playlist", "playlist-action" to "remove", "title" to title, "uris" to listOf(uri)))) {
            is RequestResult.Ok -> {
                Log.e("JSON", resp.result.toString())
                result = resp.result["response"] as String
                Server.resetPlaylists()
                Server.dropPlaylist(title)
            }
            is RequestResult.Error -> {
                Log.e("JSON", resp.msg); result = resp.msg
            }
        }
        Log.e("HTTP", "Finished RemoveSong")
        return result
    }
}

class FileExists(val uri: String) : AsyncTask<Void, Int, Boolean>() {
    var result: Boolean = false

    override fun doInBackground(vararg params: Void?): Boolean {
        Log.e("HTTP", "request: file-exists")

        with(baseurl().openConnection() as HttpURLConnection) {
            requestMethod = "HEAD"
            connect()
        }
        result = true
        return result
    }
}

class ConversionStatus(val uri: String): OperationAsync(){
    override fun doInBackground(vararg p0: Void?): String? {
        result = when (val resp = makeRequest(hashMapOf("action" to "conversion-status", "uri" to uri.substring(baseurl().toString().length - 1)))) {
            is RequestResult.Ok -> {
                Log.e("JSON", resp.result.toString())
                resp.result["result"] as String
            }
            is RequestResult.Error -> {
                Log.e("JSON", resp.msg); resp.msg
            }
        }
        Log.e("HTTP", "Finished conversionStatus")
        return result
    }
}

class DoLogin : AsyncTask<Void, Int, Boolean>() {
    var result: Boolean = false
    var done = false
    var msg = ""

    override fun doInBackground(vararg params: Void?): Boolean {
        Log.e("HTTP", "request: do-login")

        val resp = makeRequest(hashMapOf("action" to "challenge-login"))
        when (resp) {
            is RequestResult.Ok -> result = true
            is RequestResult.Error -> {
                result = false; msg = resp.msg
            }
        }
        done = true
        return result
    }
}

sealed class ServerPlaylistResult {
    abstract fun get(): ArrayList<Playlist>
    abstract fun error(): String

    class Future(val async: ApollonAsync) : ServerPlaylistResult() {
        override fun get(): ArrayList<Playlist> {
            return async.result
        }

        override fun error(): String {
            return async.error
        }
    }

    class Ready(val value: ApollonAsync) : ServerPlaylistResult() {
        override fun get(): ArrayList<Playlist> {
            return value.result
        }

        override fun error(): String {
            return value.error
        }
    }
}

sealed class ServerSongsResult {
    abstract fun get(): ArrayList<Song>
    abstract fun error(): String

    class Future(val async: SongAsync) : ServerSongsResult() {
        override fun get(): ArrayList<Song> {
            return async.result
        }

        override fun error(): String {
            return async.error
        }
    }

    class Ready(val value: SongAsync) : ServerSongsResult() {
        override fun get(): ArrayList<Song> {
            return value.result
        }

        override fun error(): String {
            return value.error
        }
    }
}

class LyricsResult(val async: GetLyrics) {
    fun get(): List<String>? {
        return async.result
    }

    fun error(): String {
        return async.error
    }
}

class OperationResult(val async: OperationAsync) {
    fun get(): String? {
        return async.result
    }
}

object Server {
    private val artists = HashMap<String, ApollonAsync>()
    private val albums = HashMap<String, SingleAlbum>()
    private val genres = HashMap<String, ApollonAsync>()
    private val playlists = HashMap<String, SinglePlaylist>()
    private var allAlbums: ApollonAsync? = null
    private var allGenres: ApollonAsync? = null
    private var allArtists: ApollonAsync? = null
    private var allPlaylists: ApollonAsync? = null
    var quality = "high" //TODO different quality


    fun getArtist(id: String): ServerPlaylistResult {
        if (artists.containsKey(id)) {
            val asyn = artists.get(id)!!
            return ServerPlaylistResult.Ready(asyn)
        } else {
            val asyn = SingleArtist(id)
            artists.put(id, asyn)
            return ServerPlaylistResult.Future(asyn)
        }
    }

    fun getGenre(id: String): ServerPlaylistResult {
        if (genres.containsKey(id)) {
            val asyn = genres.get(id)!!
            return ServerPlaylistResult.Ready(asyn)
        } else {
            val asyn = SingleArtist(id)
            genres.put(id, asyn)
            return ServerPlaylistResult.Future(asyn)
        }
    }

    fun getAlbum(id: String): ServerSongsResult {
        Log.e("album_id", id)
        return if (albums.containsKey(id)) {
            val asyn = albums.get(id)!!
            ServerSongsResult.Ready(asyn)
        } else {
            val asyn = SingleAlbum(id)
            albums.put(id, asyn)
            ServerSongsResult.Future(asyn)
        }
    }

    fun getAlbums(): ServerPlaylistResult {
        return if (allAlbums == null) {
            allAlbums = AllAlbums()
            ServerPlaylistResult.Future(allAlbums!!)
        } else {
            ServerPlaylistResult.Ready(allAlbums!!)
        }
    }

    fun getArtists(): ServerPlaylistResult {
        return if (allArtists == null) {
            allArtists = AllArtists()
            ServerPlaylistResult.Future(allArtists!!)
        } else {
            ServerPlaylistResult.Ready(allArtists!!)
        }
    }

    fun getGenres(): ServerPlaylistResult {
        return if (allGenres == null) {
            allGenres = AllGenres()
            ServerPlaylistResult.Future(allGenres!!)
        } else {
            ServerPlaylistResult.Ready(allGenres!!)
        }
    }

    fun getLyrics(artist: String, title: String): LyricsResult {
        val gl = GetLyrics(artist, title)
        gl.execute()
        return LyricsResult(gl)
    }

    fun getPlaylists(): ServerPlaylistResult {
        return if (allPlaylists == null) {
            allPlaylists = AllPlaylists()
            ServerPlaylistResult.Future(allPlaylists!!)
        } else {
            ServerPlaylistResult.Ready(allPlaylists!!)
        }
    }

    fun getPlaylist(title: String): ServerSongsResult {
        return if (playlists.containsKey(title)) {
            val asyn = playlists.get(title)!!
            ServerSongsResult.Ready(asyn)
        } else {
            val asyn = SinglePlaylist(title)
            playlists.put(title, asyn)
            ServerSongsResult.Future(asyn)
        }
    }

    fun createPlaylist(title: String): OperationResult {
        val asyn = CreatePlaylist(title)
        asyn.execute()
        return OperationResult(asyn)
    }

    fun removePlaylist(title: String): OperationResult {
        val asyn = RemovePlaylist(title)
        asyn.execute()
        return OperationResult(asyn)
    }

    fun addSong(title: String, uri: String): OperationResult {
        val asyn = AddSong(title, uri)
        asyn.execute()
        return OperationResult(asyn)
    }

    fun removeSong(title: String, uri: String): OperationResult {
        val asyn = RemoveSong(title, uri)
        asyn.execute()
        return OperationResult(asyn)
    }

    fun resetPlaylists() {
        allPlaylists = null
    }

    fun dropPlaylist(title: String) {
        playlists.remove(title)
    }

    fun getConversionStatus(uri: String): OperationResult {
        val asyn = ConversionStatus(uri)
        asyn.execute()
        return OperationResult(asyn)
    }
}

