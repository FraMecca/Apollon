package com.apollon

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.apollon.classes.Song
import java.io.IOException
import kotlin.random.Random
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.icu.util.ULocale
import android.media.*
import android.os.AsyncTask
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apollon.classes.StreamingSong
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList

class PlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    // Binder given to clients
    private val CHANNEL_ID = "101010"

    private val PAUSE_ACTION = "PAUSE"

    private val PLAY_ACTION = "PLAY"

    private val PREVIOUS_ACTION = "PREVIOUS"

    private val NEXT_ACTION = "NEXT"

    private val binder = LocalBinder()

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var mediaController: MediaControllerCompat

    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private lateinit var metaDataBuilder: MediaMetadataCompat.Builder

    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var audioManager: AudioManager

    private lateinit var focusRequest: AudioFocusRequest

    private val handler = Handler()

    private var loopPlaylist = false

    private var randomSelection = false

    private var playlist = ArrayList<Song>()

    private var songIndex = 0

    private var ready = false

    override fun onCreate() {
        super.onCreate()

        stateBuilder = PlaybackStateCompat.Builder()

        //Media session
        mediaSession = MediaSessionCompat(baseContext, "MediaSession").apply {

            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(Callback())
        }

        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)

        mediaController = mediaSession.controller

        metaDataBuilder = MediaMetadataCompat.Builder()

        notificationManager = NotificationManagerCompat.from(this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(FocusChangeListener(), handler)
            build()
        }

        mediaController.registerCallback(NotificationCallback())

        //Notification channel
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.player_notifications_channel), NotificationManager.IMPORTANCE_LOW)
        // Register the channel with the system
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    //The system calls this method when an activity, requests the service be started
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("PlayerService", this.toString())
        when (intent.action) {
            PLAY_ACTION -> mediaSession.controller.transportControls.play()
            PAUSE_ACTION -> mediaSession.controller.transportControls.pause()
            NEXT_ACTION -> mediaSession.controller.transportControls.skipToNext()
            PREVIOUS_ACTION -> mediaSession.controller.transportControls.skipToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf()
        //allow rebind
        return false
    }

    override fun onDestroy() {
        Log.e("DESTROY", "destroyed")
        notificationManager.cancelAll()
        mediaSession.release()
        mediaPlayer?.release()
        super.onDestroy()
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
            mediaController.transportControls.stop()
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
        ready = true
        mediaController.transportControls.play()
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
        if (this.playlist != playlist || this.songIndex != songIndex || !ready) {
            ready = false
            this.playlist = playlist
            this.songIndex = songIndex
            if (mediaPlayer?.isLooping == true)
                mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            mediaPlayer?.reset()

            val s = SingleSong(playlist[songIndex].id)
            s.execute()
            while(true) {
                try {
                    s.get(50, TimeUnit.MILLISECONDS)
                    break
                } catch (e: TimeoutException){}
            }
            val str = s.get()
            try {
                val uu:String = str.url
                val f = FileExists(uu)
                f.execute()
                while(f.result == false) {} // TODO ANIMATION


                mediaPlayer?.setDataSource(uu)
                mediaPlayer?.prepareAsync()
                mediaSession.setMetadata(songToMetaData(str))
            } catch (ex: IOException) {
                Toast.makeText(applicationContext, getString(R.string.unsupported_format), Toast.LENGTH_SHORT)
                    .show()
            }
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

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getSessionController(): MediaControllerCompat {
        return mediaSession.controller
    }

    /*
    fun echoCurrentSong() {
        mediaSession.setMetadata(songToMetaData(playlist[songIndex]))
    }
   */

    private fun sendNotification() {

        val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1, 2))
            setSmallIcon(R.drawable.icon)
            setContentTitle(playlist[songIndex].title)
            setContentText(playlist[songIndex].artist)
           // setLargeIcon(mediaSession.controller.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Adds transport controls
            addAction(createAction(PREVIOUS_ACTION, R.drawable.back_noti, getString(R.string.previous)))

            if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PAUSED)
                addAction(createAction(PLAY_ACTION, R.drawable.play_noti, getString(R.string.play)))
            else
                addAction(createAction(PAUSE_ACTION, R.drawable.pause_noti, getString(R.string.pause)))

            addAction(createAction(NEXT_ACTION, R.drawable.forward_noti, getString(R.string.next)))

            //creates the same kind of intent android creates to start the application so that the activity is resumed instead of recreated
            val notificationIntent = Intent(applicationContext, MainActivity::class.java)
            notificationIntent.action = Intent.ACTION_MAIN
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setContentIntent(PendingIntent.getActivity(applicationContext, 2, notificationIntent, 0))
            //prevents notification from being cancelled
            setOngoing(true)
        }
        notificationManager.notify(1, builder.build())
    }

    private fun createAction(action: String, drawable: Int, title: String): NotificationCompat.Action {
        val i = Intent(applicationContext, PlayerService::class.java)
        i.action = action
        val pi = PendingIntent.getService(applicationContext, 1, i, 0)
        return NotificationCompat.Action.Builder(drawable, title, pi).build()
    }

    private fun songToMetaData(song: StreamingSong): MediaMetadataCompat {
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.img_url)
        metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration.toLong())

        val cursong = playlist[songIndex]
        val url = cursong.img_url.replace("http://", "https://")
        Log.e("Picasso", url)
        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                Log.e("Picasso", e?.message)
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                // loaded bitmap is here (bitmap)
                metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

        })
        return metaDataBuilder.build()
    }

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    inner class Callback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            audioManager.requestAudioFocus(focusRequest)
            handler.removeCallbacksAndMessages(null)
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
                handler.postDelayed({mediaController.transportControls.stop(); Log.e("PAUSE", "stopped")}, 30000)
            }
        }

        override fun onStop() {
            super.onStop()
            mediaPlayer?.release()
            mediaPlayer = null
            ready = false
            mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0F).build())
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            nextMedia()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            //If mediaPlayer is null nothing happens
            when {
                mediaPlayer?.currentPosition ?: 4001 > 4000 -> mediaPlayer?.seekTo(0)

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

        override fun onSeekTo(pos: Long) {
            if (ready) {
                super.onSeekTo(pos)
                mediaPlayer?.seekTo(pos.toInt())
                mediaSession.sendSessionEvent("PositionChanged", null)
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
            when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ALL -> {
                    mediaPlayer?.isLooping = false
                    loopPlaylist = true
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                }

                PlaybackStateCompat.REPEAT_MODE_ONE -> {
                    loopPlaylist = false
                    mediaPlayer?.isLooping = true
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                }

                PlaybackStateCompat.REPEAT_MODE_NONE -> {
                    mediaPlayer?.isLooping = false
                    loopPlaylist = false
                    mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                }
            }
        }
    }

    inner class NotificationCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            sendNotification()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            sendNotification()
        }
    }

    inner class FocusChangeListener : AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS -> mediaController.transportControls.pause()

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mediaController.transportControls.pause()

                AudioManager.AUDIOFOCUS_GAIN -> mediaController.transportControls.play()
            }
        }
    }
}