package com.apollon

import android.os.AsyncTask
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.apollon.classes.Playlist
import com.apollon.classes.Song
import com.apollon.classes.StreamingSong
import org.json.JSONArray
import org.json.JSONObject

sealed class RequestResult {
    class Ok(val result: JSONObject) : RequestResult()
    class Error() : RequestResult ()
}

fun makeRequest(m: Map<String, String>): RequestResult{
    val params = hashMapOf("user" to "mario",
        "password" to "rossi")

    m.forEach {k,v -> params.put(k, v)}
    val data = JSONObject(params).toString()
    with(baseurl.openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        this.outputStream.write(data.toByteArray())
        this.outputStream.flush()
        this.outputStream.close()
        inputStream.bufferedReader().use {
            val llines = it.lines().toArray()
            assert(llines.count() == 1.toInt())
            val j: JSONObject = JSONObject(llines[0] as String)
            if (j["response"] == "error")
                return RequestResult.Error()
            else
                return RequestResult.Ok(j)
        }
    }

}
val baseurl = URL("https://francescomecca.eu/apollon/")

class AllAlbums(val context: Context) : AsyncTask<Void, Int, List<Playlist>>(){
    var result = ArrayList<Playlist>()

    override fun doInBackground(vararg params: Void?): List<Playlist>? {
        val ret = ArrayList<Playlist>()
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
                        ret.add(Playlist(album["uri"] as String, album["title"] as String, album["img"] as String))
                    else
                        ret.add(Playlist(album["uri"] as String, album["title"] as String))
                }
            }
            is RequestResult.Error -> return null
        }
        Log.e("HTTP", "Finished all-albums")
        if (ret.size != 0) {
            result = ret
            return ret
        }
        else
            return null
    }
}

class AllGenre(val context: Context) : AsyncTask<Void, Int, List<Playlist>>(){
    var result = ArrayList<Playlist>()

    override fun doInBackground(vararg params: Void?): List<Playlist>? {
        val ret = ArrayList<Playlist>()
        Log.e("HTTP", "request: all-by-genre")

        val resp = makeRequest(hashMapOf("action" to "all-by-genre"))
        when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString() )

                for (i in 0 .. values.length()-1){
                    val genre : String  = values[i] as String
                    ret.add(Playlist(i.toString(), genre)) // TODO: correct id
                }
            }
            is RequestResult.Error -> return null
        }
        Log.e("HTTP", "Finished all-Genre")
        if (ret.size != 0) {
            result = ret
            return ret
        }
        else
            return null
    }
}

class SingleAlbum(val context: Context, val uri:String) : AsyncTask<Void, Int, List<Song>>(){
    var result = ArrayList<Song>()

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
            is RequestResult.Error -> return null
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

class SingleSong(val context: Context, val uri:String) : AsyncTask<Void, Int, StreamingSong>(){
    var result : StreamingSong? = null

    override fun doInBackground(vararg params: Void?): StreamingSong? {
        Log.e("HTTP", "request: single-song")

        val resp = makeRequest(hashMapOf("action" to "new-song", "quality" to "high", "uri" to uri))
        result = when(resp){
            is RequestResult.Ok -> {
                val j = resp.result
                val url = baseurl.toString().substring(0, baseurl.toString().length-1) +(j["uri"] as String)
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
            is RequestResult.Error -> null
        }
        return result
    }

    override fun onPostExecute(result: StreamingSong?) {
        super.onPostExecute(result)
    }
}
