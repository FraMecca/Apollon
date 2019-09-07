package com.apollon.classes

import android.content.Context


object Credentials{
    var context: Context? = null
    fun init(ccontext: Context){
        context = ccontext
    }

    fun get(): Pair<String, String>{
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        return Pair(prefs.getString("user", ""), prefs.getString("password", ""))
    }
    fun getServer(): Pair<String, Int>{
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        return Pair(prefs.getString("ip", ""), prefs.getInt("port", 80))
    }

    fun save(user: String, password: String, server:String, port:Int){
        if (context==null)
            throw Exception("Invalid state: Credentials not initialized")

        val prefs = context!!.getSharedPreferences("Apollon", 0)
        val editor = prefs.edit()
        editor.putString("user", user)
        editor.putString("password", password)
        editor.putString("server", server)
        editor.putInt("password", port)
        editor.commit()
    }

}

