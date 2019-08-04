package com.apollon.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
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

    private val REPEAT_NOT = 0
    private val REPEAT_ALL = 1
    private val REPEAT_SONG = 2

    lateinit var mView: View
    lateinit var albumArt: ImageView
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var seekBar: SeekBar
    lateinit var currentTime: TextView
    lateinit var duration: TextView
    lateinit var song: Song
    lateinit var playButton: Button
    lateinit var repeatButton: Button
    private var seekBarHandler = Handler()
    private var isPlaying = true
    private var repeat = REPEAT_NOT

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mView = inflater.inflate(R.layout.player, container, false)
        song = arguments?.getSerializable("song") as Song
        title = mView.findViewById(R.id.song_title)
        artist = mView.findViewById(R.id.song_artist)
        albumArt = mView.findViewById(R.id.album_art)
        seekBar = mView.findViewById(R.id.seekbar_audio)
        currentTime = mView.findViewById(R.id.current_position)
        duration = mView.findViewById(R.id.duration)
        playButton = mView.findViewById(R.id.button_play)
        repeatButton = mView.findViewById(R.id.button_repeat)
        mView.findViewById<View>(R.id.button_previous).setOnClickListener(this)
        mView.findViewById<View>(R.id.button_next).setOnClickListener(this)

        playButton.setOnClickListener(this)
        repeatButton.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        title.text = song.title
        artist.text = song.artist
        duration.text = song.millisToString(song.duration)
        Picasso.get().load(song.img_url).into(albumArt)

        //Registers to service bus
        (activity as MainActivity).bus.register(this)

        (activity as MainActivity).play(song.audio_url)
        updateSeekBar()
        return mView
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        seekBarHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        currentTime.text = song.millisToString(song.duration * progress / 1000)
    }

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        (activity as MainActivity).seekTo(song.duration * seekbar!!.progress / 1000)
        if(isPlaying)
            updateSeekBar()
    }

    private fun updateSeekBar() {
        activity?.runOnUiThread(object : Runnable {
            override fun run() {
                val currentPosition = (activity as MainActivity).getCurrentPosition()
                seekBar.progress = currentPosition * 1000 / song.duration
                currentTime.text = song.millisToString(currentPosition)
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
                    (activity as MainActivity).play(song.audio_url)
                    updateSeekBar()
                } else {
                    isPlaying = false
                    playButton.setBackgroundResource(R.drawable.play_button_selector)
                    seekBarHandler.removeCallbacksAndMessages(null)
                    (activity as MainActivity).pause()
                }
            R.id.button_repeat ->
                when(repeat){
                    REPEAT_NOT -> {
                        repeat = REPEAT_ALL
                        repeatButton.setBackgroundResource(R.drawable.repeat_all_button_selector)
                        (activity as MainActivity).loop(true)
                    }
                    REPEAT_ALL -> {
                        repeat = REPEAT_SONG
                        repeatButton.setBackgroundResource(R.drawable.repeat_this_button_selector)
                        (activity as MainActivity).loop(true)
                    }
                    REPEAT_SONG -> {
                        repeat = REPEAT_NOT
                        repeatButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
                        (activity as MainActivity).loop(false)
                    }
                }
        }
    }

    @Subscribe
    fun answerAvailable(event: NewSongEvent) {
        isPlaying = false
        playButton.setBackgroundResource(R.drawable.play_button_selector)
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun newInstance(song: Song): PlayerFragment {
            val args = Bundle()
            args.putSerializable("song", song)
            val fragment = PlayerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}