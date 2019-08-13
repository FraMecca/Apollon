package com.apollon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.apollon.classes.NewSongEvent
import com.apollon.classes.Song
import com.apollon.fragments.LoginFragment
import com.apollon.fragments.PlayerFragment
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), OnClickListener {

    lateinit var player: PlayerService
    lateinit var mediaController: MediaControllerCompat
    lateinit var callback: Callback
    lateinit var miniPlayer: View
    lateinit var albumArt: ImageView
    lateinit var playButton: Button
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var currentSong: Song

    lateinit var bus: Bus

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            player = binder.service
            player.bus = bus
            mediaController = player.getSessionController()
            callback = Callback()
            mediaController.registerCallback(callback)


            Log.e("MediaController", mediaController.toString())
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
        playButton = findViewById(R.id.button_play)
        miniPlayer.setOnClickListener(this)
        playButton.setOnClickListener(this)
        findViewById<Button>(R.id.button_previous).setOnClickListener(this)
        findViewById<Button>(R.id.button_next).setOnClickListener(this)
        replaceFragment(LoginFragment(), false)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        bus.unregister(this)
        mediaController.unregisterCallback(callback)
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_play ->
                if (mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING)
                    mediaController.transportControls.play()
                else
                    mediaController.transportControls.pause()

            R.id.button_previous ->
                mediaController.transportControls.skipToPrevious()

            R.id.button_next ->
                mediaController.transportControls.skipToNext()

            R.id.mini_player -> {
                replaceFragment(PlayerFragment())
                player.echoCurrentSong()
            }
        }
    }

    //Handles newSong event posted on bus by PlayerService
    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        if (event.song == null) {   //No songs to play
            //setIsPlaying(false)
        } else {    //New or same currentSong
            if (!::currentSong.isInitialized || currentSong != event.song) {
                // setIsPlaying(true)
                currentSong = event.song
                title.text = currentSong.title
                artist.text = currentSong.artist
                Picasso.get().load(currentSong.img_url).into(albumArt)
            }
        }
    }

    inner class Callback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            if (state?.state == PlaybackStateCompat.STATE_PLAYING)
                playButton.setBackgroundResource(R.drawable.pause_button_selector)
            else if (state?.state == PlaybackStateCompat.STATE_PAUSED)
                playButton.setBackgroundResource(R.drawable.play_button_selector)

        }
    }
}
