package com.apollon

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.apollon.classes.NewSongEvent
import com.apollon.classes.Song
import com.squareup.otto.Bus
import java.io.IOException
import kotlin.random.Random
import android.app.NotificationManager
import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.squareup.picasso.Picasso


class PlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {


    // Binder given to clients
    private val CHANNEL_ID = "101010"

    private val binder = LocalBinder()

    private var mediaPlayer: MediaPlayer? = null

    private var stopped = false

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    lateinit var bus: Bus

    private var loopPlaylist = false

    var randomSelection = false

    var playlist = ArrayList<Song>()

    var songIndex = 0

    override fun onCreate() {
        super.onCreate()

        //Media session
        mediaSession = MediaSessionCompat(baseContext, "MediaSession").apply {

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackStateCompat.Builder()
            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(Callback())

        }

        //Notification channel
        val name = "Player Notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)

        // Register the channel with the system
        val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    //The system calls this method when an activity, requests the service be started
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("PlayerService", "onStartCommand")
        initMediaPlayer()
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()
        //mediaPlayer?.stop()
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
        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition().toLong(), 1f).build())
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
            //mediaPlayer?.stop()
            if (mediaPlayer?.isLooping == true)
                mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            mediaPlayer?.reset()
            try {
                mediaPlayer?.setDataSource(playlist[songIndex].audio_url)
                mediaPlayer?.prepareAsync()
                bus.post(NewSongEvent(playlist[songIndex]))
                sendNotification()
            } catch (ex: IOException) {
                Toast.makeText(applicationContext, getString(R.string.unsupported_format), Toast.LENGTH_SHORT)
            }
        } else {
            bus.post(NewSongEvent(playlist[songIndex]))
        }
    }

    private fun initMediaPlayer() {
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

    private fun nextMedia() {
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

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getSessionController(): MediaControllerCompat {
        return mediaSession.controller
    }

    fun echoCurrentSong() {
        bus.post(NewSongEvent(playlist[songIndex]))
    }

    fun sendNotification() {
        //Get an instance of NotificationManager//
        val view = RemoteViews(packageName, R.layout.notification)
        Log.e("NOTI", playlist[songIndex].title)

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContent(view)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        //Picasso.get().load(playlist[songIndex].img_url).into(view.findViewById<ImageView>(R.id.album_art))
        view.setTextViewText(R.id.song_title, playlist[songIndex].title)
        view.setTextViewText(R.id.song_artist, playlist[songIndex].artist)
        /*view.findViewById<Button>(R.id.back_button)
        view.findViewById<Button>(R.id.play_button)
        view.findViewById<Button>(R.id.next_button)*/
        // Gets an instance of the NotificationManager service//

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(10, builder.build())
        }

    }

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    inner class Callback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            if (mediaPlayer == null) {
                initMedia(playlist, songIndex)
            } else if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition().toLong(), 1F).build())
            }
        }

        override fun onPause() {
            super.onPause()
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition().toLong(), 0F).build())
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            nextMedia()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            //If mediaPlayer is null nothing happens
            when {
                mediaPlayer?.currentPosition ?: 1001 > 1000 -> mediaPlayer?.seekTo(0)

                randomSelection -> {
                    var ran = songIndex
                    while (ran == songIndex) ran = Random.nextInt(0, playlist.size - 1)
                    initMedia(playlist, ran)
                }

                loopPlaylist -> initMedia(playlist, (playlist.size + songIndex - 1) % playlist.size) //Kotlin module returns -1 instead of (size - 1)

                else -> {
                    initMedia(playlist, maxOf(songIndex - 1, 0))
                }
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)
            when (shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> {
                    randomSelection = true
                    mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                }

                PlaybackStateCompat.SHUFFLE_MODE_NONE -> {
                    randomSelection = false
                    mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                }
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            when(repeatMode){
                PlaybackStateCompat.REPEAT_MODE_ALL ->{
                    mediaPlayer?.isLooping = false
                    loopPlaylist = true
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                }

                PlaybackStateCompat.REPEAT_MODE_ONE ->{
                    loopPlaylist = false
                    mediaPlayer?.isLooping = true
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                }

                PlaybackStateCompat.REPEAT_MODE_NONE ->{
                    mediaPlayer?.isLooping = false
                    loopPlaylist = false
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                }
            }
        }
    }
}