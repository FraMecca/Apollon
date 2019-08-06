package com.apollon

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.apollon.classes.NewSongEvent
import com.apollon.classes.Song
import com.squareup.otto.Bus
import java.io.IOException
import kotlin.random.Random


class PlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {


    // Binder given to clients
    private val binder = LocalBinder()

    private var mediaPlayer: MediaPlayer? = null

    lateinit var bus: Bus

    private var loopPlaylist = false

    var randomSelection = false

    var playlist = ArrayList<Song>()

    var songIndex = 0

    private var stopped = false

    //The system calls this method when an activity, requests the service be started
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("PlayerService", "onStartCommand")
        initMediaPlayer()
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    override fun onCompletion(mp: MediaPlayer) {
        //Invoked when playback of a media source has completed.
        if (songIndex == playlist.size - 1 && !loopPlaylist && !randomSelection) {
            bus.post(NewSongEvent(null))
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            stopped = true
        } else {
            nextMedia()
        }
    }

    //Handle errors
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked when there has been an error during an asynchronous operation
        when (what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED $extra")
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN $extra")
        }
        return true
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked to communicate some info.
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        //Invoked when the media source is ready for playback.
        mediaPlayer?.start()
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        //Invoked indicating the completion of a seek operation.
    }

    override fun onAudioFocusChange(focusChange: Int) {
        //Invoked when the audio focus of the system is updated.
    }

    fun initMedia(playlist: ArrayList<Song>, songIndex: Int) {
        if (mediaPlayer == null)
            initMediaPlayer()
        if (this.playlist != playlist || this.songIndex != songIndex || stopped) {
            stopped = false
            this.playlist = playlist
            this.songIndex = songIndex
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            try {
                mediaPlayer?.setDataSource(playlist[songIndex].audio_url)
                mediaPlayer?.prepareAsync()
                bus.post(NewSongEvent(playlist[songIndex]))
            } catch (ex: IOException) {
                Toast.makeText(applicationContext, getString(R.string.unsupported_format), Toast.LENGTH_SHORT)
            }
        } else {
            bus.post(NewSongEvent(playlist[songIndex]))
        }
    }

    fun initMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
        //Set up MediaPlayer event listeners
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnSeekCompleteListener(this)
        mediaPlayer?.setOnInfoListener(this)
        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
    }

    fun playMedia() {
        if (mediaPlayer == null) {
            initMedia(playlist, songIndex)
        } else if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pauseMedia() {
        // Equality check (==) handles null case too
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun previousMedia() {
        //If mediaPlayer is null nothing happens
        when {
            mediaPlayer?.currentPosition ?: 1001 > 1000 -> mediaPlayer?.seekTo(0)
            randomSelection -> {
                var ran = songIndex
                while (ran == songIndex) ran = Random.nextInt(0, playlist.size - 1)
                initMedia(playlist, ran)
            }
            loopPlaylist -> initMedia(playlist, (playlist.size + songIndex - 1) % playlist.size) //Kotlin module returns -1 instead of (size - 1)
            else -> initMedia(playlist, maxOf(songIndex - 1, 0))
        }
    }

    fun nextMedia() {
        when {
            randomSelection -> {
                var ran = songIndex
                while (ran == songIndex) ran = Random.nextInt(0, playlist.size - 1)
                initMedia(playlist, ran)
            }
            loopPlaylist -> initMedia(playlist, (songIndex + 1) % playlist.size)
            else -> initMedia(playlist, minOf(songIndex + 1, playlist.size - 1))
        }
    }

    fun loopSong() {
        mediaPlayer?.isLooping = true
    }

    fun loopPlaylist(loop: Boolean) {
        mediaPlayer?.isLooping = false
        loopPlaylist = loop
    }

    fun isLoopingSong(): Boolean {
        return mediaPlayer?.isLooping ?: false
    }

    fun isLoopingPlaylist(): Boolean {
        return loopPlaylist
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }
}