package com.apollon.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.classes.Song
import com.squareup.picasso.Picasso
import com.squareup.otto.Subscribe
import com.apollon.classes.NewSongEvent


class PlayerFragment : Fragment(), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private val NOT = 0
    private val PLAYLIST = 1
    private val SONG = 2

    lateinit var albumArt: ImageView
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var seekBar: SeekBar
    lateinit var currentTime: TextView
    lateinit var duration: TextView
    lateinit var song: Song
    lateinit var playButton: Button
    lateinit var previousButton: Button
    lateinit var nextButton: Button
    lateinit var loopButton: Button
    lateinit var randomButton: Button
    private var seekBarHandler = Handler()
    private var isPlaying = true
    private var loopType = NOT
    private var randomSelection = false

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        var mView = inflater.inflate(R.layout.player, container, false)
        title = mView.findViewById(R.id.song_title)
        artist = mView.findViewById(R.id.song_artist)
        albumArt = mView.findViewById(R.id.album_art)
        seekBar = mView.findViewById(R.id.seekbar_audio)
        currentTime = mView.findViewById(R.id.current_position)
        duration = mView.findViewById(R.id.duration)
        playButton = mView.findViewById(R.id.button_play)
        previousButton = mView.findViewById(R.id.button_previous)
        nextButton = mView.findViewById(R.id.button_next)
        loopButton = mView.findViewById(R.id.button_repeat)
        randomButton = mView.findViewById(R.id.button_random)
        mView.findViewById<View>(R.id.button_previous).setOnClickListener(this)
        mView.findViewById<View>(R.id.button_next).setOnClickListener(this)

        playButton.setOnClickListener(this)
        previousButton.setOnClickListener(this)
        nextButton.setOnClickListener(this)
        loopButton.setOnClickListener(this)
        randomButton.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)

        //Registers to service bus
        (activity as MainActivity).bus.register(this)

        return mView
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        seekBarHandler.removeCallbacksAndMessages(null)
        (activity as MainActivity).bus.unregister(this)
        super.onDestroyView()
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        currentTime.text = song.millisToString(song.duration * progress / 1000)
    }

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        (activity as MainActivity).player.seekTo(song.duration * seekbar!!.progress / 1000)
        if (isPlaying)
            startSeekBarHandler()
    }

    private fun updateSeekBar(){
        val currentPosition = (activity as MainActivity).player.getCurrentPosition()
        seekBar.progress = currentPosition * 1000 / song.duration
        currentTime.text = song.millisToString(currentPosition)
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
            R.id.button_play ->
                if (!isPlaying) {
                    isPlaying = true
                    playButton.setBackgroundResource(R.drawable.pause_button_selector)
                    (activity as MainActivity).player.playMedia()
                    startSeekBarHandler()
                } else {
                    isPlaying = false
                    playButton.setBackgroundResource(R.drawable.play_button_selector)
                    seekBarHandler.removeCallbacksAndMessages(null)
                    (activity as MainActivity).player.pauseMedia()
                }

            R.id.button_previous ->
                (activity as MainActivity).player.previousMedia()

            R.id.button_next ->
                (activity as MainActivity).player.nextMedia()

            R.id.button_repeat ->
                when (loopType) {
                    //Loops playlist
                    NOT -> {
                        loopType = PLAYLIST
                        loopButton.setBackgroundResource(R.drawable.repeat_all_button_selector)
                        (activity as MainActivity).player.loopPlaylist(true)
                    }
                    //Loops song
                    PLAYLIST -> {
                        loopType = SONG
                        loopButton.setBackgroundResource(R.drawable.repeat_this_button_selector)
                        (activity as MainActivity).player.loopSong()
                    }
                    //Doesn't loop
                    SONG -> {
                        loopType = NOT
                        loopButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
                        (activity as MainActivity).player.loopPlaylist(false)
                    }
                }

            R.id.button_random ->
                if (randomSelection) {
                    randomSelection = false
                    randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
                    (activity as MainActivity).player.randomSelection = false
                } else {
                    randomSelection = true
                    randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)
                    (activity as MainActivity).player.randomSelection = true
                }
        }
    }

    //Handles newSong event posted on bus by PlayerService
    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        if (event.song == null) {   //No songs to play
            isPlaying = false
            playButton.setBackgroundResource(R.drawable.play_button_selector)
            seekBarHandler.removeCallbacksAndMessages(null)
        } else {    //New or same song
            song = event.song
            title.text = song.title
            artist.text = song.artist
            duration.text = song.millisToString(song.duration)
            Picasso.get().load(song.img_url).into(albumArt)

            //Starts song if it was paused
            if((activity as MainActivity).player.isPaused()) {
                isPlaying = false
                playButton.setBackgroundResource(R.drawable.play_button_selector)
                updateSeekBar()
            }

            startSeekBarHandler()

            //Updates loop button and variables
            when {
                (activity as MainActivity).player.isLoopingSong() -> {
                    loopType = SONG
                    loopButton.setBackgroundResource(R.drawable.repeat_this_button_selector)
                }
                (activity as MainActivity).player.isLoopingPlaylist() -> {
                    loopType = PLAYLIST
                    loopButton.setBackgroundResource(R.drawable.repeat_all_button_selector)
                }
                else -> {
                    loopType = NOT
                    loopButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
                }
            }

            //Updates random button and variables
            if ((activity as MainActivity).player.randomSelection) {
                randomSelection = true
                randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)
            } else {
                randomSelection = false
                randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
            }
        }
    }
}