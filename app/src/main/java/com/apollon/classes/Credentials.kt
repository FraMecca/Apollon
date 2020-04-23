@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.apollon.classes

import android.content.Context


object Credentials{
    lateinit var server: String
    lateinit var user: String
    lateinit var password: String
    var proto: Int = -1
    var port: Int = -1

    fun init(context: Context){
        val prefs = context.getSharedPreferences("Apollon", 0)
        user = prefs.getString("user", "")
        password = prefs.getString("password", "")
        server = prefs.getString("server", "")
        proto = prefs.getInt("protocol", 1)
        port = prefs.getInt("port", 80)
    }

    fun get(): Pair<String, String>{
        return Pair(user, password)
    }
    fun getServer(): Pair<String, Pair<Int, Int>>{
        return Pair(server, Pair(proto, port))
    }

    fun save(context:Context, user: String, password: String, server:String, proto: Int, port:Int){

        val prefs = context.getSharedPreferences("Apollon", 0)
        val editor = prefs.edit()
        editor.putString("user", user)
        editor.putInt("protocol", proto)
        editor.putString("password", password)
        editor.putString("server", server)
        editor.putInt("port", port)
        editor.apply()
        init(context) // reinstate variables
    }

}