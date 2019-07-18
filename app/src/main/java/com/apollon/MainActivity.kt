package com.apollon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ButtonBarLayout

class MainActivity : AppCompatActivity() {

    val fm = supportFragmentManager

    lateinit var button : Button
    lateinit var player: MediaPlayerService


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.LocalBinder
            player = binder.service
            Log.e("CREATE", binder.toString())
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, MediaPlayerService::class.java)
        val song = Song("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "canzone di merda")
        intent.putExtra("song", song.uri )
        startService(intent)
        Log.e("Service", "start")
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)


        /**** GUI ****/
        button = findViewById(R.id.button2)
        button.text = song.title
        button.setOnClickListener { v: View ->

            val transaction = fm.beginTransaction()
            val frag = ApollonFragment()
            transaction.replace(R.id.main, frag)
            transaction.commit()
            fm.executePendingTransactions()
            frag.updateTextView()
        }


    }
}
