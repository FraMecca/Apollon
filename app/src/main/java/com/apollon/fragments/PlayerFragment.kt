package com.apollon.fragments

import android.content.Intent
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
    lateinit var playButton: Button
    lateinit var previousButton: Button
    lateinit var nextButton: Button
    lateinit var loopButton: Button
    lateinit var randomButton: Button
    lateinit var favouriteButton: Button
    private var seekBarHandler = Handler()
    private var loopType = NOT
    private var randomSelection = false

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
        previousButton = mView.findViewById(R.id.button_previous)
        nextButton = mView.findViewById(R.id.button_next)
        loopButton = mView.findViewById(R.id.button_repeat)
        randomButton = mView.findViewById(R.id.button_random)
        favouriteButton = mView.findViewById(R.id.button_favourite)

        mView.findViewById<Button>(R.id.button_previous).setOnClickListener(this)
        mView.findViewById<Button>(R.id.button_next).setOnClickListener(this)
        mView.findViewById<Button>(R.id.button_share).setOnClickListener(this)

        playButton.setOnClickListener(this)
        previousButton.setOnClickListener(this)
        nextButton.setOnClickListener(this)
        loopButton.setOnClickListener(this)
        randomButton.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        favouriteButton.setOnClickListener(this)

        //Registers to service bus
        (activity as MainActivity).bus.register(this)

        return mView
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        seekBarHandler.removeCallbacksAndMessages(null)
        (activity as MainActivity).bus.unregister(this)
        (activity as MainActivity).miniPlayer.visibility = View.VISIBLE
        super.onDestroyView()
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        currentTime.text = (activity as MainActivity).currentSong.millisToString((activity as MainActivity).currentSong.duration * progress / 1000)
    }

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        (activity as MainActivity).player.seekTo((activity as MainActivity).currentSong.duration * seekbar!!.progress / 1000)
        if ((activity as MainActivity).isPlaying)
            startSeekBarHandler()
    }

    private fun updateSeekBar() {
        val currentPosition = (activity as MainActivity).player.getCurrentPosition()
        seekBar.progress = currentPosition * 1000 / (activity as MainActivity).currentSong.duration
        currentTime.text = (activity as MainActivity).currentSong.millisToString(currentPosition)
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
                if (!(activity as MainActivity).isPlaying) {
                    (activity as MainActivity).setIsPlaying(true)
                    playButton.setBackgroundResource(R.drawable.pause_button_selector)
                    (activity as MainActivity).player.playMedia()
                    startSeekBarHandler()
                } else {
                    if ((activity as MainActivity).player.pauseMedia()) {
                        (activity as MainActivity).setIsPlaying(false)
                        playButton.setBackgroundResource(R.drawable.play_button_selector)
                        seekBarHandler.removeCallbacksAndMessages(null)
                    }
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
                    //Loops `(activity as MainActivity).currentSong`
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
            R.id.button_favourite ->
                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector)

            R.id.button_share -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message_start) + " ${(activity as MainActivity).currentSong.artist} - ${(activity as MainActivity).currentSong.title} " + getString(R.string.share_message_end))
                    type = "text/plain"
                }
                startActivity(sendIntent)
            }
        }
    }

    //Handles newSong event posted on bus by PlayerService
    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        if (event.song == null) {   //No songs to play
            (activity as MainActivity).setIsPlaying(false)
            playButton.setBackgroundResource(R.drawable.play_button_selector)
            seekBarHandler.removeCallbacksAndMessages(null)
        } else {    //New or same currentSong
            title.text = (activity as MainActivity).currentSong.title
            artist.text = (activity as MainActivity).currentSong.artist
            duration.text = (activity as MainActivity).currentSong.millisToString((activity as MainActivity).currentSong.duration)
            Picasso.get().load((activity as MainActivity).currentSong.img_url).into(albumArt)
        }

        //Starts currentSong if it was paused
        if ((activity as MainActivity).isPlaying) {
            playButton.setBackgroundResource(R.drawable.pause_button_selector)
            startSeekBarHandler()
        } else {
            playButton.setBackgroundResource(R.drawable.play_button_selector)
            updateSeekBar()
        }

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
