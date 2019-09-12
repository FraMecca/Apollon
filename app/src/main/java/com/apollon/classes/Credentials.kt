package com.apollon.classes

import android.content.Context


object Credentials{
    var context: Context? = null
    fun init(ccontext: Context){
        context = ccontext
        val prefs = context!!.getSharedPreferences("Apollon", 0)
    }

    fun get(): Pair<String, String>{
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        return Pair(prefs.getString("user", ""), prefs.getString("password", ""))
    }
    fun getServer(): Pair<String, Pair<Int, Int>>{
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        return Pair(prefs.getString("server", ""), Pair(prefs.getInt("protocol", 1), prefs.getInt("port", 80)))
    }

    fun save(user: String, password: String, server:String, proto: Int, port:Int){
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        val editor = prefs.edit()
        editor.putString("user", user)
        editor.putInt("protocol", proto)
        editor.putString("password", password)
        editor.putString("server", server)
        editor.putInt("port", port)
        editor.commit()
    }

}

