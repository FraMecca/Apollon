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
    class Error(val msg: String) : RequestResult ()
}

fun makeRequest(m: Map<String, String>): RequestResult{
   val (user, pass) = Credentials.get()
   val params = hashMapOf("user" to "mario",
        "password" to "rossi")

    m.forEach { (k, v) -> params.put(k, v)}
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
    }catch(e:Exception){
        return RequestResult.Error("Can't make the connection: "+e.message)
    }
}

fun baseurl():URL{
    val (ip, snd) = Credentials.getServer()
    val (proto, port) = snd
    val split = ip.split('/')
    val hostname = split[0]
    val file = ip.substring(hostname.length,ip.length)

    val protoString:String = when(proto) {
        0 -> "http"
        1 -> "https"
        else -> {assert(false); "https"}
    }
    Log.e("GOTURL:", URL(protoString , hostname,port, file).toString())
    return URL(protoString , hostname,port, file)
}


abstract class ApollonAsync: AsyncTask<Void, Int, List<Playlist>>() {
    var result = ArrayList<Playlist>()
    var error = ""
}

private class AllAlbums(val context: Context) : ApollonAsync(){

    override fun doInBackground(vararg params: Void?): List<Playlist.Album>? {
        val ret = ArrayList<Playlist.Album>()
        Log.e("HTTP", "request: all-by-album")

        val resp = makeRequest(hashMapOf("action" to "all-by-album"))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString() )

                for (i in 0 .. values.length()-1){
                    val album : JSONObject = values[i] as JSONObject
                    // TODO : uri, title + artist
                    if((album["img"] as String) != "")
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String, album["img"] as String))
                    else
                        ret.add(Playlist.Album(album["uri"] as String, album["title"] as String))
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished all-albums")
        if (ret.size != 0) {
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}

private class AllArtists(val context: Context) : ApollonAsync(){

    override fun doInBackground(vararg params: Void?): List<Playlist.Artist>? {
        val ret = ArrayList<Playlist.Artist>()
        Log.e("HTTP", "request: all-by-artist")

        val resp = makeRequest(hashMapOf("action" to "all-by-artist"))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString() )

                for (i in 0 .. values.length()-1){
                    val artist : JSONObject = values[i] as JSONObject
                    // TODO : uri, title + artist
                    if((artist["img"] as String) != "")
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String, artist["img"] as String))
                    else
                        ret.add(Playlist.Artist(artist["name"] as String, artist["name"] as String))
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished all-artists")
        if (ret.size != 0) {
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}

private class AllGenres(val context: Context) : ApollonAsync(){

    override fun doInBackground(vararg params: Void?): List<Playlist.Genre>? {
        val ret = ArrayList<Playlist.Genre>()
        Log.e("HTTP", "request: all-by-genre")

        val resp = makeRequest(hashMapOf("action" to "all-by-genre"))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString() )

                for (i in 0 .. values.length()-1){
                    val genre : String  = values[i] as String
                    ret.add(Playlist.Genre(i.toString(), genre)) // TODO: correct id
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished all-Genre")
        if (ret.size != 0) {
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}
private class SingleGenre(val context: Context, val name:String) : ApollonAsync(){

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
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}


class SingleAlbum(val context: Context, val uri:String) : AsyncTask<Void, Int, List<Song>>(){
    var result = ArrayList<Song>()
    var error = ""

    override fun doInBackground(vararg params: Void?): ArrayList<Song>? {
        val ret = ArrayList<Song>()
        Log.e("HTTP", "request: single-album")

        val resp = makeRequest(hashMapOf("action" to "album", "key" to uri))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val songs = (j["album"] as JSONObject)["songs"] as JSONArray
                val artist = (j["album"] as JSONObject)["artist"] as String
                val img = (j["album"] as JSONObject)["img"] as String

                for (i in 0 .. songs.length()-1){
                    val jsong = songs[i] as JSONObject
                    val title : String  = jsong["title"] as String

                    val uri = jsong["uri"] as String
                    if (img == "")
                        ret.add(Song(uri, title, artist))
                    else
                        ret.add(Song(uri, title, artist, img))
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished SingleAlbum")
        if (ret.size != 0){
            result = ret
            return ret
        }
        else
            return null
    }
}

private class SingleArtist(val context: Context, val name:String) : ApollonAsync(){

    override fun doInBackground(vararg params: Void?): ArrayList<Playlist.Album>? {
        val ret = ArrayList<Playlist.Album>()
        Log.e("HTTP", "request: single-artist")

        val resp = makeRequest(hashMapOf("action" to "artist", "key" to name))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val albums = (j["artist"] as JSONObject)["albums"] as JSONArray
                val artist = (j["artist"] as JSONObject)["name"] as String
                val img = (j["artist"] as JSONObject)["img"] as String

                for (i in 0 .. albums.length()-1){
                    val jalbum = albums[i] as JSONObject
                    val title : String  = jalbum["title"] as String

                    val uri = jalbum["uri"] as String
                    if (img == "")
                        ret.add(Playlist.Album(uri, title))
                    else
                        ret.add(Playlist.Album(uri, title, img))
                }
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        Log.e("HTTP", "Finished SingleArtist")
        if (ret.size != 0){
            result = ret as ArrayList<Playlist>
            return ret
        }
        else
            return null
    }
}

class SingleSong(val context: Context, val uri:String) : AsyncTask<Void, Int, StreamingSong>(){
    var result : StreamingSong? = null
    var error = ""

    override fun doInBackground(vararg params: Void?): StreamingSong? {
        Log.e("HTTP", "request: single-song")

        val resp = makeRequest(hashMapOf("action" to "new-song", "quality" to "high", "uri" to uri))
        result = when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val url = baseurl().toString().substring(0, baseurl().toString().length-1) +(j["uri"] as String)
                Log.e("URL", url)
                val metadata = ((((j.get("metadata") as JSONObject).get("json") as JSONObject).get("media") as JSONObject)
                    .get("track") as JSONArray).get(0) as JSONObject
                val title = metadata["Title"] as String
                val artist = metadata["Performer"] as String
                val album = metadata["Album"] as String
                val fduration = metadata["Duration"] as String
                val duration = (fduration.toFloat() * 1000).toInt()
                val s = StreamingSong(url, duration, uri, title, artist)
                Log.e("HTTP", "new-song done")
                s
            }
            is RequestResult.Error -> { error = resp.msg; return null}
        }
        return result
    }

    override fun onPostExecute(result: StreamingSong?) {
        super.onPostExecute(result)
    }
}

class FileExists(val context: Context, val uri:String) : AsyncTask<Void, Int, Boolean>(){
    var result : Boolean = false

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

class DoLogin(val context: Context) : AsyncTask<Void, Int, Boolean>(){
    var result : Boolean = false
    var done = false
    var msg = ""

    override fun doInBackground(vararg params: Void?): Boolean {
        Log.e("HTTP", "request: do-login")

        val resp = makeRequest(hashMapOf("action" to "challenge-login"))
        when(resp) {
            is RequestResult.Ok -> result = true
            is RequestResult.Error -> {result = false; msg = resp.msg}
        }
        done = true
        return result
    }
}

sealed class ServerPlaylistResult(){
    abstract fun get(): ArrayList<Playlist>
    abstract fun error(): String

    class Future(val async:ApollonAsync): ServerPlaylistResult(){
        override fun get(): ArrayList<Playlist> {return async.result}
        override fun error(): String { return async.error }
    }
    class Ready(val value: ApollonAsync): ServerPlaylistResult(){
        override fun get(): ArrayList<Playlist> {return value.result}
        override fun error(): String { return value.error }
    }
}

sealed class ServerSongsResult(){
    abstract fun get(): ArrayList<Song>
    abstract fun error(): String

    class Future(val async:SingleAlbum): ServerSongsResult(){
        override fun get(): ArrayList<Song> {return async.result}
        override fun error(): String { return async.error }
    }
    class Ready(val value: SingleAlbum): ServerSongsResult(){
        override fun get(): ArrayList<Song> {return value.result}
        override fun error(): String { return value.error }
    }
}

object Server {
    private val artists = HashMap<String, ApollonAsync>()
    private val albums = HashMap<String, SingleAlbum>()
    private val genres = HashMap<String, ApollonAsync>()
    private var allAlbums: ApollonAsync? = null
    private var allGenres: ApollonAsync? = null
    private var allArtists: ApollonAsync? = null


    fun getArtist(context: Context, id: String): ServerPlaylistResult{
        if (artists.containsKey(id)) {
            val asyn = artists.get(id)!!
            return ServerPlaylistResult.Ready(asyn)
        } else {
            val asyn = SingleArtist(context, id)
            artists.put(id, asyn)
            return ServerPlaylistResult.Future(asyn)
        }
    }
    fun getGenre(context: Context, id: String): ServerPlaylistResult{
        if (genres.containsKey(id)) {
            val asyn = genres.get(id)!!
            return ServerPlaylistResult.Ready(asyn)
        } else {
            val asyn = SingleArtist(context, id)
            genres.put(id, asyn)
            return ServerPlaylistResult.Future(asyn)
        }
    }

    fun getAlbum(context: Context, id: String): ServerSongsResult{
        if (albums.containsKey(id)) {
            val asyn = albums.get(id)!!
            return ServerSongsResult.Ready(asyn)
        } else {
            val asyn = SingleAlbum(context, id)
            albums.put(id, asyn)
            return ServerSongsResult.Future(asyn)
        }
    }

    fun getAlbums(context: Context): ServerPlaylistResult{
        if(allAlbums == null) {
            allAlbums = AllAlbums(context)
            return ServerPlaylistResult.Future(allAlbums!!)
        } else {
            return ServerPlaylistResult.Ready(allAlbums!!)
        }
    }
    fun getArtists(context: Context): ServerPlaylistResult{
        if(allArtists == null) {
            allArtists = AllArtists(context)
            return ServerPlaylistResult.Future(allArtists!!)
        } else {
            return ServerPlaylistResult.Ready(allArtists!!)
        }
    }
    fun getGenres(context: Context): ServerPlaylistResult{
        if(allGenres == null) {
            allGenres = AllGenres(context)
            return ServerPlaylistResult.Future(allGenres!!)
        } else {
            return ServerPlaylistResult.Ready(allGenres!!)
        }
    }
}