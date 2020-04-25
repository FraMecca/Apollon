package com.apollon

import android.os.AsyncTask
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.apollon.classes.*
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

private class AllAlbums(val listener: TaskListener) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()
        Log.e("HTTP", "request: all-by-album")

        when (val resp = makeRequest(hashMapOf("action" to "all-by-album"))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray

                for (i in 0 until values.length()) {
                    val album: JSONObject = values[i] as JSONObject
                    if ((album["img"] as String) != "")
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String, album["img"] as String, album["#nsongs"] as Int))
                    else
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String, album["#nsongs"] as Int))
                }
                val result = TaskResult.ServerPlaylistResult(ret)
                Server.allAlbums = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerPlaylistResult(error = resp.msg))
            }
        }
    }
}

private class AllArtists(val listener: TaskListener) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()

        when (val resp = makeRequest(hashMapOf("action" to "all-by-artist"))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray

                for (i in 0 until values.length()) {
                    val artist: JSONObject = values[i] as JSONObject

                    if ((artist["img"] as String) != "")
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["img"] as String, artist["#albums"] as Int))
                    else
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["#albums"] as Int))
                }

                val result = TaskResult.ServerPlaylistResult(ret)
                Server.allArtists = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerPlaylistResult(error = resp.msg))
            }
        }
    }
}

private class AllGenres(val listener: TaskListener) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()

        when (val resp = makeRequest(hashMapOf("action" to "all-by-genre"))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray

                for (i in 0 until values.length()) {
                    val genreName = (values[i] as JSONArray)[0].toString()
                    val items = (values[i] as JSONArray)[1].toString().toInt()
                    ret.add(Playlist.Genre(i.toString(), genreName, elements = items))
                }

                val result = TaskResult.ServerPlaylistResult(ret)
                Server.allGenres = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerPlaylistResult(error = resp.msg))
            }
        }
    }
}

private class AllPlaylists(val listener: TaskListener) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()

        when (val resp = makeRequest(hashMapOf("action" to "list-playlists"))) {
            is RequestResult.Ok -> {
                val j = resp.result
                var img = ""
                val values = j["result"] as JSONArray

                for (i in 0 until values.length()) {
                    val playlist: JSONObject = values[i] as JSONObject
                    if (playlist["title"] != "Favourites") {
                        //checks if the first song in the playlist has an image URL
                        if ((playlist["uris"] as JSONArray).length() > 0) {
                            var firstImg = ((playlist["uris"] as JSONArray)[0] as JSONObject)["img"].toString()
                            img = if (firstImg.isNotEmpty())
                                firstImg
                            else
                                "playlist"
                        }
                        ret.add(Playlist.Custom(playlist["title"] as String, playlist["title"] as String, img, playlist["#nsongs"] as Int))
                    }
                }

                val result = TaskResult.ServerPlaylistResult(ret)
                Server.allPlaylists = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerPlaylistResult(error = resp.msg))
            }
        }
    }
}

private class SingleGenre(val listener: TaskListener, val name: String) : AsyncTask<Void, Int, Unit>() {//NOT WORKING

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()

        when (val resp = makeRequest(hashMapOf("action" to "genre", "key" to name))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["artists"] as JSONArray

                for (i in 0 until values.length()) {
                    val artist: JSONObject = values[i] as JSONObject

                    if ((artist["img"] as String) != "")
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["img"] as String, artist["#albums"] as Int))
                    else
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["#albums"] as Int))
                }

                val result = TaskResult.ServerPlaylistResult(ret)
                Server.genres[name] = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerSongsResult(error = resp.msg))
            }
        }
        Log.e("HTTP", "Finished SingleGenre")
    }
}


private class SingleAlbum(val listener: TaskListener, val uri: String) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Song>()
        Log.e("HTTP", "request: single-album")

        when (val resp = makeRequest(hashMapOf("action" to "album", "key" to uri))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val songs = (j["album"] as JSONObject)["songs"] as JSONArray
                val artist = (j["album"] as JSONObject)["artist"] as String
                val img = (j["album"] as JSONObject)["img"] as String

                for (i in 0 until songs.length()) {
                    val jsong = songs[i] as JSONObject
                    val title: String = jsong["title"] as String

                    val uri = jsong["uri"] as String
                    if (img == "")
                        ret.add(Song(uri, title, artist))
                    else
                        ret.add(Song(uri, title, artist, img))
                }
                val result = TaskResult.ServerSongsResult(ret)
                Server.albums[uri] = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerSongsResult(error = resp.msg))
            }
        }
    }
}

private class SingleArtist(val listener: TaskListener, val name: String) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Playlist>()
        Log.e("HTTP", "request: single-artist")

        when (val resp = makeRequest(hashMapOf("action" to "artist", "key" to name))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val albums = (j["artist"] as JSONObject)["albums"] as JSONArray

                for (i in 0 until albums.length()) {
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

                val result = TaskResult.ServerPlaylistResult(ret)
                Server.artists[name] = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerPlaylistResult(error = resp.msg))
            }
        }
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

class SinglePlaylist(val listener: TaskListener, val title: String) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        val ret = ArrayList<Song>()
        Log.e("HTTP", "request: get-playlist")

        when (val resp = makeRequest(hashMapOf("action" to "get-playlist", "title" to title))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val songs = (j["result"] as JSONObject)["uris"] as JSONArray

                for (i in 0 until songs.length()) {
                    val jsong = songs[i] as JSONObject
                    val title: String = jsong["title"] as String
                    val artist: String = jsong["artist"] as String
                    val uri = jsong["uri"] as String
                    val img = jsong["img"] as String
                    ret.add(PlaylistSong(uri, title, artist, img))
                }

                val result = TaskResult.ServerSongsResult(ret)
                Server.playlists[title] = result
                listener.onTaskCompleted(result)
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.ServerSongsResult(error = resp.msg))
            }
        }
    }
}

class GetLyrics(val listener: TaskListener, val artist: String, val title: String) : AsyncTask<Void, Int, Unit>() {

    override fun doInBackground(vararg params: Void?) {
        lateinit var ret: List<String>
        Log.e("HTTP", "request: lyrics")

        when (val resp = makeRequest(hashMapOf("action" to "lyrics", "artist" to artist, "song" to title))) {
            is RequestResult.Ok -> {
                val j = resp.result
                val content = j["lyrics"] as String
                ret = content.split("\r\n")
                listener.onTaskCompleted(TaskResult.LyricsResult(ret))
            }
            is RequestResult.Error -> {
                listener.onTaskCompleted(TaskResult.LyricsResult(error = resp.msg))
            }
        }
        Log.e("HTTP", "Finished GetLyrics")
    }
}

class CreatePlaylist(val listener: TaskListener, var title: String) : AsyncTask<Void, Int, Unit>() {
    override fun doInBackground(vararg p0: Void?) {
        when (val resp = makeRequest(hashMapOf<String, Any>("action" to "new-playlist", "title" to title, "uris" to emptyList<String>()))) {
            is RequestResult.Ok -> {
                Server.resetPlaylists()
                listener.onTaskCompleted(TaskResult.OperationResult("createPlaylist", title))
            }
            is RequestResult.Error -> listener.onTaskCompleted(TaskResult.OperationResult("createPlaylist", title, resp.msg))
        }
        Log.e("HTTP", "Finished CreatePlaylist")
    }
}

class RemovePlaylist(val listener: TaskListener, var title: String) : AsyncTask<Void, Int, Unit>() {
    override fun doInBackground(vararg p0: Void?) {
        when (val resp = makeRequest(hashMapOf("action" to "remove-playlist", "title" to title))) {
            is RequestResult.Ok -> {
                Server.resetPlaylists()
                Server.dropPlaylist(title)
                listener.onTaskCompleted(TaskResult.OperationResult("removePlaylist", title))
            }
            is RequestResult.Error -> listener.onTaskCompleted(TaskResult.OperationResult("removePlaylist", title, resp.msg))

        }
        Log.e("HTTP", "Finished RemovePlaylist")
    }
}

class RenamePlaylist(val listener: TaskListener, var oldTitle: String, var newTitle: String) : AsyncTask<Void, Int, Unit>() {
    override fun doInBackground(vararg p0: Void?) {
        when (val resp = makeRequest(hashMapOf("action" to "rename-playlist", "src" to oldTitle, "dst" to newTitle))) {
            is RequestResult.Ok -> {
                Server.resetPlaylists()
                Server.dropPlaylist(oldTitle)
                listener.onTaskCompleted(TaskResult.OperationResult("renamePlaylist", oldTitle))
            }
            is RequestResult.Error -> listener.onTaskCompleted(TaskResult.OperationResult("renamePlaylist", newTitle, resp.msg))

        }
        Log.e("HTTP", "Finished RemovePlaylist")
    }
}

class AddSong(val listener: TaskListener, var title: String, var uri: String) : AsyncTask<Void, Int, Unit>() {
    override fun doInBackground(vararg p0: Void?) {
        when (val resp = makeRequest(hashMapOf("action" to "modify-playlist", "playlist-action" to "add", "title" to title, "uris" to listOf(uri)))) {
            is RequestResult.Ok -> {
                Server.resetPlaylists()
                Server.dropPlaylist(title)
                listener.onTaskCompleted(TaskResult.OperationResult("addSong", title))
            }
            is RequestResult.Error -> listener.onTaskCompleted(TaskResult.OperationResult("addSong", title, resp.msg))
        }
        Log.e("HTTP", "Finished AddSong")
    }
}

class RemoveSong(val listener: TaskListener, var title: String, var uri: String) : AsyncTask<Void, Int, Unit>() {
    override fun doInBackground(vararg p0: Void?) {
        when (val resp = makeRequest(hashMapOf("action" to "modify-playlist", "playlist-action" to "remove", "title" to title, "uris" to listOf(uri)))) {
            is RequestResult.Ok -> {
                Server.resetPlaylists()
                Server.dropPlaylist(title)
                listener.onTaskCompleted(TaskResult.OperationResult("removeSong", title))
            }
            is RequestResult.Error -> listener.onTaskCompleted(TaskResult.OperationResult("removeSong", title, resp.msg))
        }
        Log.e("HTTP", "Finished RemoveSong")
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

class DoLogin : AsyncTask<Void, Int, Boolean>() {
    var result: Boolean = false
    var done = false
    var msg = ""

    override fun doInBackground(vararg params: Void?): Boolean {
        Log.e("HTTP", "request: do-login")

        when (val resp = makeRequest(hashMapOf("action" to "challenge-login"))) {
            is RequestResult.Ok -> result = true
            is RequestResult.Error -> {
                result = false; msg = resp.msg
            }
        }
        done = true
        return result
    }
}

sealed class TaskResult {
    class ServerPlaylistResult(val result: ArrayList<Playlist>? = null, val error: String = "") : TaskResult()
    class ServerSongsResult(val result: ArrayList<Song>? = null, val error: String = "") : TaskResult()
    class LyricsResult(val result: List<String>? = null, val error: String = "") : TaskResult()
    class OperationResult(val task: String? = null, val title: String? = null, val error: String = "") : TaskResult()
}

object Server {
    val artists = HashMap<String, TaskResult.ServerPlaylistResult>()
    val albums = HashMap<String, TaskResult.ServerSongsResult>()
    val genres = HashMap<String, TaskResult.ServerPlaylistResult>()
    val playlists = HashMap<String, TaskResult.ServerSongsResult>()
    var allAlbums: TaskResult.ServerPlaylistResult? = null
    var allGenres: TaskResult.ServerPlaylistResult? = null
    var allArtists: TaskResult.ServerPlaylistResult? = null
    var allPlaylists: TaskResult.ServerPlaylistResult? = null
    var quality = "high"


    fun getArtist(listener: TaskListener, id: String) {
        if (artists.containsKey(id)) {
            listener.onTaskCompleted(artists[id] as TaskResult)
        } else {
            SingleArtist(listener, id).execute()
        }
    }

    fun getGenre(listener: TaskListener, id: String) {
        Log.e("singlegenre", id)
        if (genres.containsKey(id)) {
            listener.onTaskCompleted(genres[id] as TaskResult)
        } else {
            SingleGenre(listener, id).execute()
        }
    }

    fun getAlbum(listener: TaskListener, id: String) {
        Log.e("album_id", id)
        if (albums.containsKey(id)) {
            listener.onTaskCompleted(albums[id] as TaskResult)
        } else {
            SingleAlbum(listener, id).execute()
        }
    }


    fun getAlbums(listener: TaskListener) {
        if (allAlbums == null) {
            AllAlbums(listener).execute()
        } else {
            listener.onTaskCompleted(allAlbums!!)
        }
    }

    fun getArtists(listener: TaskListener) {
        if (allArtists == null) {
            AllArtists(listener).execute()
        } else {
            listener.onTaskCompleted(allArtists!!)
        }
    }

    fun getGenres(listener: TaskListener) {
        if (allGenres == null) {
            AllGenres(listener).execute()
        } else {
            listener.onTaskCompleted(allGenres!!)
        }
    }

    fun getLyrics(listener: TaskListener, artist: String, title: String) {
        GetLyrics(listener, artist, title).execute()
    }

    fun getPlaylists(listener: TaskListener) {
        if (allPlaylists == null) {
            AllPlaylists(listener).execute()
        } else {
            listener.onTaskCompleted(allPlaylists!!)
        }
    }

    fun getPlaylist(listener: TaskListener, title: String) {
        if (playlists.containsKey(title)) {
            listener.onTaskCompleted(playlists[title] as TaskResult)
        } else {
            SinglePlaylist(listener, title).execute()
        }
    }

    fun createPlaylist(listener: TaskListener, title: String) {
        CreatePlaylist(listener, title).execute()
    }

    fun removePlaylist(listener: TaskListener, title: String) {
        RemovePlaylist(listener, title).execute()
    }

    fun renamePlaylist(listener: TaskListener, oldTitle: String, newTitle: String) {
        RenamePlaylist(listener, oldTitle, newTitle).execute()
    }

    fun addSong(listener: TaskListener, title: String, uri: String) {
        AddSong(listener, title, uri).execute()
    }

    fun removeSong(listener: TaskListener, title: String, uri: String) {
        RemoveSong(listener, title, uri).execute()
    }

    fun resetPlaylists() {
        allPlaylists = null
    }

    fun dropPlaylist(title: String) {
        playlists.remove(title)
    }
}