package com.apollon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apollon.classes.NewSongEvent
import com.apollon.classes.Song
import com.apollon.fragments.LoginFragment
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnClickListener {

    lateinit var player: PlayerService
    lateinit var miniPlayer: View
    lateinit var albumArt: ImageView
    lateinit var playButton: Button
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var currentSong: Song
    var isPlaying = false

    lateinit var bus: Bus

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            player = binder.service
            player.bus = bus
            Log.e("CREATE", binder.toString())
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //volumeControlStream = AudioManager.STREAM_MUSIC

        bus = Bus()
        bus.register(this)

        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        /**** GUI ****/
        miniPlayer = findViewById(R.id.mini_player)
        albumArt = findViewById(R.id.album_art)
        title = findViewById(R.id.song_title)
        artist = findViewById(R.id.song_artist)
        playButton = findViewById(R.id.play_button)
        playButton.setOnClickListener(this)
        findViewById<Button>(R.id.back_button).setOnClickListener(this)
        findViewById<Button>(R.id.next_button).setOnClickListener(this)
        replaceFragment(LoginFragment(), false)

    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        bus.unregister(this)
        super.onDestroy()
    }

    fun replaceFragment(frag: Fragment, addToStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main, frag)
        //adds the transaction to a stack so it can be re-executed by pressing the back button
        if (addToStack)
            transaction.addToBackStack("ApollonStack")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
    }

    fun setIsPlaying(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (isPlaying)
            playButton.setBackgroundResource(R.drawable.pause_button_selector)
        else
            playButton.setBackgroundResource(R.drawable.play_button_selector)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.play_button ->
                if (!isPlaying) {
                    setIsPlaying(true)
                    player.playMedia()
                } else {
                    setIsPlaying(false)
                    player.pauseMedia()
                }

            R.id.back_button ->
                player.previousMedia()

            R.id.next_button ->
                player.nextMedia()
        }
    }

    //Handles newSong event posted on bus by PlayerService
    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        if (event.song == null) {   //No songs to play
            setIsPlaying(false)
        } else {    //New or same currentSong
            if (!::currentSong.isInitialized || currentSong != event.song) {
                setIsPlaying(true)
                currentSong = event.song
                title.text = currentSong.title
                artist.text = currentSong.artist
                Picasso.get().load(currentSong.img_url).into(albumArt)
            }
        }
    }
}
