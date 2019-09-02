package com.apollon

import android.os.AsyncTask
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.apollon.classes.Playlist
import org.json.JSONArray
import org.json.JSONObject

val url = URL("https://francescomecca.eu/apollon/")

class AllAlbums(val context: Context) : AsyncTask<Void, Integer, List<Playlist>>(){
    val m = hashMapOf("user" to "mario",
        "password" to "rossi",
        "action" to "all-by-album")

    val ret = ArrayList<Playlist>()

    override fun doInBackground(vararg params: Void?): List<Playlist>? {
        with(url.openConnection() as HttpURLConnection){
            requestMethod = "POST"
            val data = JSONObject(m).toString()
            this.outputStream.write(data.toByteArray())
            this.outputStream.flush()
            this.outputStream.close()

            Log.e("HTTP", "req")
            inputStream.bufferedReader().use {
                val llines = it.lines().toArray()
                val len = llines.count()
                Log.e("HTTP", "lines: " + len.toString())

                assert(len == 1.toInt())
                val j: JSONObject = JSONObject(llines[0] as String)

                if (j["response"] == "error")
                    return null

                val values = j["values"] as JSONArray
                Log.e("HTTP", values.length().toString() )

                for (i in 0 .. values.length()-1){
                    val album : JSONObject = values[i] as JSONObject
                    // TODO : uri, title + artist
                    if((album["img"] as String) != "")
                        ret.add(Playlist(i, album["title"] as String, album["img"] as String))
                    else
                        ret.add(Playlist(i, album["title"] as String))
                    Log.e("HTTP", album.toString())

                }
            }
        }
        if (ret.size != 0)
            return ret
        else
            return null
    }

}