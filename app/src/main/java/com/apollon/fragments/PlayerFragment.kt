package com.apollon.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.apollon.MainActivity
import com.apollon.R
import com.squareup.picasso.Picasso
import com.squareup.otto.Subscribe
import com.apollon.classes.NewSongEvent


class PlayerFragment : Fragment(), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    lateinit var callback: Callback
    private var seekBarHandler = Handler()
    lateinit var albumArt: ImageView
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var seekBar: SeekBar
    lateinit var currentTime: TextView
    lateinit var duration: TextView
    lateinit var playButton: Button
    lateinit var loopButton: Button
    lateinit var randomButton: Button
    lateinit var favouriteButton: Button
    var songDuration = 0

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (activity as MainActivity).miniPlayer.visibility = View.GONE
        var mView = inflater.inflate(R.layout.player, container, false)
        title = mView.findViewById(R.id.song_title)
        artist = mView.findViewById(R.id.song_artist)
        albumArt = mView.findViewById(R.id.album_art)
        seekBar = mView.findViewById(R.id.seekbar_audio)
        currentTime = mView.findViewById(R.id.current_position)
        duration = mView.findViewById(R.id.duration)
        playButton = mView.findViewById(R.id.button_play)
        loopButton = mView.findViewById(R.id.button_repeat)
        randomButton = mView.findViewById(R.id.button_random)
        favouriteButton = mView.findViewById(R.id.button_favourite)

        mView.findViewById<Button>(R.id.button_previous).setOnClickListener(activity as MainActivity)
        mView.findViewById<Button>(R.id.button_next).setOnClickListener(activity as MainActivity)
        mView.findViewById<Button>(R.id.button_share).setOnClickListener(this)

        playButton.setOnClickListener(activity as MainActivity)
        loopButton.setOnClickListener(this)
        randomButton.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        favouriteButton.setOnClickListener(this)

        //Registers to service bus
        callback = Callback()
        (activity as MainActivity).mediaController.registerCallback(callback)

        return mView
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        seekBarHandler.removeCallbacksAndMessages(null)
        (activity as MainActivity).mediaController.unregisterCallback(callback)
        (activity as MainActivity).miniPlayer.visibility = View.VISIBLE
        super.onDestroyView()
    }

    private fun millisToString(millis: Int) : String{
        val seconds = millis/1000
        return "" + seconds/60 + ":" + String.format("%02d", seconds%60) // 2 digits precision - 0 for padding
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        currentTime.text = millisToString(songDuration * progress / 1000)
    }

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        (activity as MainActivity).mediaController.transportControls.seekTo(songDuration.toLong() * seekbar!!.progress / 1000)
    }

    private fun updateSeekBar() {
        val currentPosition = (activity as MainActivity).player.getCurrentPosition()
        seekBar.progress = currentPosition * 1000 / songDuration
        currentTime.text = millisToString(currentPosition)
    }

    private fun startSeekBarHandler() {
        seekBarHandler.removeCallbacksAndMessages(null)
        activity?.runOnUiThread(object : Runnable {
            override fun run() {
                updateSeekBar()
                seekBarHandler.postDelayed(this, 1000)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            /* R.id.button_play ->
                 if ((activity as MainActivity).mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
                     playButton.setBackgroundResource(R.drawable.pause_button_selector)
                     (activity as MainActivity).mediaController.transportControls.play()
                     startSeekBarHandler()
                 } else {
                         (activity as MainActivity).mediaController.transportControls.pause()
                         playButton.setBackgroundResource(R.drawable.play_button_selector)
                         seekBarHandler.removeCallbacksAndMessages(null)

                 }*/

            R.id.button_repeat ->
                when ((activity as MainActivity).mediaController.repeatMode) {
                    //Loops playlist
                    PlaybackStateCompat.REPEAT_MODE_NONE -> {
                        (activity as MainActivity).mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                    }
                    //Loops current song
                    PlaybackStateCompat.REPEAT_MODE_ALL -> {
                        (activity as MainActivity).mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                    }
                    //Doesn't loop
                    PlaybackStateCompat.REPEAT_MODE_ONE -> {
                        (activity as MainActivity).mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                    }
                }

            R.id.button_random ->
                if ((activity as MainActivity).mediaController.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                    (activity as MainActivity).mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                } else {
                    (activity as MainActivity).mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                }

            R.id.button_favourite ->
                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector)

            R.id.button_share -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message_start) + " ${artist.text} - ${title.text} " + getString(R.string.share_message_end))
                    type = "text/plain"
                }
                startActivity(sendIntent)
            }
        }
    }

    /*
    //Handles newSong event posted on bus by PlayerService
    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        if (event.song == null) {   //No songs to play
            playButton.setBackgroundResource(R.drawable.play_button_selector)
            seekBarHandler.removeCallbacksAndMessages(null)
        } else {    //New or same currentSong
            title.text = (activity as MainActivity).currentSong.title
            artist.text = (activity as MainActivity).currentSong.artist
            duration.text = (activity as MainActivity).currentSong.millisToString((activity as MainActivity).currentSong.duration)
            Picasso.get().load((activity as MainActivity).currentSong.img_url).into(albumArt)
        }

        //Starts currentSong if it was paused
        /*if ((activity as MainActivity).) {
            playButton.setBackgroundResource(R.drawable.pause_button_selector)
            startSeekBarHandler()
        } else {
            playButton.setBackgroundResource(R.drawable.play_button_selector)
            updateSeekBar()
        }*/

        //Updates loop button and variables

    }*/

    inner class Callback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            when (state?.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    playButton.setBackgroundResource(R.drawable.pause_button_selector)
                    startSeekBarHandler()
                }

                PlaybackStateCompat.STATE_PAUSED -> {
                    playButton.setBackgroundResource(R.drawable.play_button_selector)
                    seekBarHandler.removeCallbacksAndMessages(null)
                }
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onShuffleModeChanged(shuffleMode: Int) {
            super.onShuffleModeChanged(shuffleMode)
            when(shuffleMode){
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)

                PlaybackStateCompat.SHUFFLE_MODE_NONE -> randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            when(repeatMode){
                PlaybackStateCompat.REPEAT_MODE_ALL -> loopButton.setBackgroundResource(R.drawable.repeat_all_button_selector)

                PlaybackStateCompat.REPEAT_MODE_ONE -> loopButton.setBackgroundResource(R.drawable.repeat_this_button_selector)

                PlaybackStateCompat.REPEAT_MODE_NONE -> loopButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if(event == "PositionChanged" && (activity as MainActivity).mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
                    startSeekBarHandler()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata == null) {   //No songs to play
                playButton.setBackgroundResource(R.drawable.play_button_selector)
                seekBarHandler.removeCallbacksAndMessages(null)
            } else {    //New or same currentSong
                title.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                artist.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                songDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
                duration.text = millisToString(songDuration)
                Picasso.get().load(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)).into(albumArt)
            }

            if((activity as MainActivity).mediaController.playbackState?.state == PlaybackStateCompat.STATE_PLAYING){
                playButton.setBackgroundResource(R.drawable.pause_button_selector)
                startSeekBarHandler()
            }
            else{
                playButton.setBackgroundResource(R.drawable.play_button_selector)
                updateSeekBar()
            }

            //Updates loop button
            when((activity as MainActivity).mediaController.repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> {
                    loopButton.setBackgroundResource(R.drawable.repeat_this_button_selector)
                }
                PlaybackStateCompat.REPEAT_MODE_ALL -> {
                    loopButton.setBackgroundResource(R.drawable.repeat_all_button_selector)
                }
                else -> {
                    loopButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
                }
            }

            //Updates random button
            if ((activity as MainActivity).player.randomSelection) {
                randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)
            } else {
                randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
            }
        }
    }
}
