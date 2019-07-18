package com.apollon

import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ButtonBarLayout

class MainActivity : AppCompatActivity() {

    val fm = supportFragmentManager

    lateinit var button : Button

    var i = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeControlStream = AudioManager.STREAM_MUSIC

        button = findViewById(R.id.button2)
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
