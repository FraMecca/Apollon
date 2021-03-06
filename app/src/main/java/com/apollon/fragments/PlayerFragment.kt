@file:Suppress("PrivatePropertyName")

package com.apollon.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.apollon.*
import kotlin.math.abs

class PlayerFragment : Fragment(), TaskListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private lateinit var callback: Callback
    private var seekBarHandler = Handler()
    lateinit var albumArt: ImageView
    lateinit var title: TextView
    lateinit var artist: TextView
    lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    lateinit var duration: TextView
    lateinit var playButton: Button
    lateinit var loopButton: Button
    lateinit var randomButton: Button
    private lateinit var favouriteButton: Button
    private lateinit var qualityButton: Button
    private lateinit var gestureDetector: GestureDetector
    private var isFavourite: Boolean = false
    var songDuration = 0
    var songUri = ""

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (activity as MainActivity).miniPlayer.visibility = View.GONE

        // Initialize gesture detection
        gestureDetector = GestureDetector((activity as MainActivity).applicationContext, GListener())

        val mView = inflater.inflate(R.layout.player, container, false)
        title = mView.findViewById(R.id.song_title)
        artist = mView.findViewById(R.id.song_artist)
        title.isSelected = true
        artist.isSelected = true
        albumArt = mView.findViewById(R.id.album_art)
        seekBar = mView.findViewById(R.id.seekbar_audio)
        currentTime = mView.findViewById(R.id.current_position)
        duration = mView.findViewById(R.id.duration)
        playButton = mView.findViewById(R.id.button_play)
        loopButton = mView.findViewById(R.id.button_repeat)
        randomButton = mView.findViewById(R.id.button_random)
        favouriteButton = mView.findViewById(R.id.button_favourite)
        qualityButton = mView.findViewById(R.id.button_quality)
        // quality button look
        when (Server.quality) {
            "high" ->
                if (qualityButton.tag == "inverted")
                    qualityButton.setBackgroundResource(R.drawable.hq_button_selector_inverted)
                else

                    qualityButton.setBackgroundResource(R.drawable.hq_button_selector)
            "medium" ->
                if (qualityButton.tag == "inverted")
                    qualityButton.setBackgroundResource(R.drawable.mq_button_selector_inverted)
                else

                    qualityButton.setBackgroundResource(R.drawable.mq_button_selector)
            "low" ->
                if (qualityButton.tag == "inverted")
                    qualityButton.setBackgroundResource(R.drawable.lq_button_selector_inverted)
                else

                    qualityButton.setBackgroundResource(R.drawable.lq_button_selector)
        }

        mView.findViewById<Button>(R.id.button_previous).setOnClickListener(activity as MainActivity)
        mView.findViewById<Button>(R.id.button_next).setOnClickListener(activity as MainActivity)
        mView.findViewById<Button>(R.id.button_share).setOnClickListener(this)
        mView.findViewById<Button>(R.id.button_lyrics).setOnClickListener(this)

        playButton.setOnClickListener(activity as MainActivity)
        loopButton.setOnClickListener(this)
        randomButton.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        favouriteButton.setOnClickListener(this)
        qualityButton.setOnClickListener(this)

        albumArt.setOnTouchListener { _, e -> gestureDetector.onTouchEvent(e) }

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

    private fun millisToString(millis: Int): String {
        val seconds = millis / 1000
        return "" + seconds / 60 + ":" + String.format("%02d", seconds % 60) // 2 digits precision - 0 for padding
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        currentTime.text = millisToString(songDuration * progress / 1000)
    }

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        seekBarHandler.removeCallbacksAndMessages(null)
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        (activity as MainActivity).mediaController.transportControls.seekTo(songDuration.toLong() * seekbar!!.progress / 1000)
        seekBar.isEnabled = false
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

            R.id.button_repeat ->
                when ((activity as MainActivity).mediaController.repeatMode) {
                    // Loops playlist
                    PlaybackStateCompat.REPEAT_MODE_NONE -> {
                        (activity as MainActivity).mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                    }
                    // Loops current song
                    PlaybackStateCompat.REPEAT_MODE_ALL -> {
                        (activity as MainActivity).mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                    }
                    // Doesn't loop
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

            R.id.button_favourite -> {
                favouriteOps()
            }

            R.id.button_share -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message_start) + " ${artist.text} - ${title.text} " + getString(R.string.share_message_end))
                    type = "text/plain"
                }
                startActivity(sendIntent)
            }

            R.id.button_lyrics -> Server.getLyrics(this, artist.text.toString(), title.text.toString())

            R.id.button_quality -> {
                val popupMenu = PopupMenu(context, v)
                val inflater = popupMenu.menuInflater
                inflater.inflate(R.menu.quality_menu, popupMenu.menu)
                popupMenu.show()
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.low_quality -> {
                            if (Server.quality != "low") {
                                Server.quality = "low"
                                (activity as MainActivity).player.changeQuality(seekBar.progress.toLong() * songDuration / 1000)
                                if (qualityButton.tag == "inverted")
                                    qualityButton.setBackgroundResource(R.drawable.lq_button_selector_inverted)
                                else
                                    qualityButton.setBackgroundResource(R.drawable.lq_button_selector)
                            }
                        }
                        R.id.medium_quality -> {
                            if (Server.quality != "medium") {
                                Server.quality = "medium"
                                (activity as MainActivity).player.changeQuality(seekBar.progress.toLong() * songDuration / 1000)
                                if (qualityButton.tag == "inverted")
                                    qualityButton.setBackgroundResource(R.drawable.mq_button_selector_inverted)
                                else
                                    qualityButton.setBackgroundResource(R.drawable.mq_button_selector)
                            }
                        }
                        R.id.high_quality -> {
                            if (Server.quality != "high") {
                                Server.quality = "high"
                                (activity as MainActivity).player.changeQuality(seekBar.progress.toLong() * songDuration / 1000)
                                if (qualityButton.tag == "inverted")
                                    qualityButton.setBackgroundResource(R.drawable.hq_button_selector_inverted)
                                else
                                    qualityButton.setBackgroundResource(R.drawable.hq_button_selector)
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    fun favouriteOps() {
        val songId = (activity as MainActivity).mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        if (isFavourite) {
            Server.removeSong(this, "Favourites", songId)
        } else {
            Server.addSong(this, "Favourites", songId)
        }
    }

    override fun onTaskCompleted(result: TaskResult) {
        when (result) {
            is TaskResult.ServerSongsResult -> {
                if (result.error == "") {
                    isFavourite = result.result!!.any { it.id == songUri }
                    activity?.runOnUiThread {
                        if (isFavourite)
                            if (favouriteButton.tag == "inverted") {
                                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector_inverted)
                            } else
                                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector)
                        else if (favouriteButton.tag == "inverted")
                                favouriteButton.setBackgroundResource(R.drawable.favourite_not_button_selector_inverted)
                            else
                                favouriteButton.setBackgroundResource(R.drawable.favourite_not_button_selector)
                    }
                } else
                    activity?.runOnUiThread {
                        Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                    }
            }

            is TaskResult.LyricsResult -> {
                if (result.error == "") {
                    var st = result.result!!.joinToString(separator = "\n")
                    if (st == "")
                        st = resources.getString(R.string.lyrics)
                    activity?.runOnUiThread {
                        AlertDialog.Builder(context, R.style.AlertStyle)
                                .setTitle(title.text)
                                .setMessage(st)
                                .setNegativeButton(context?.getString(R.string.close)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                    }
                }
            }

            is TaskResult.OperationResult -> {

                when (result.task) {
                    "removeSong" -> {
                        if (result.error == "") {
                            if (favouriteButton.tag == "inverted")
                                favouriteButton.setBackgroundResource(R.drawable.favourite_not_button_selector_inverted)
                            else
                                favouriteButton.setBackgroundResource(R.drawable.favourite_not_button_selector)
                            isFavourite = false
                            activity?.runOnUiThread {
                                Toast.makeText(context, context!!.getString(R.string.song_removed, context!!.getString(R.string.favourites)), Toast.LENGTH_SHORT).show()
                            }
                        } else
                            activity?.runOnUiThread {
                                Toast.makeText(context, "${context!!.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    "addSong" -> {
                        if (result.error == "") {
                            if (favouriteButton.tag == "inverted")
                                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector_inverted)
                            else
                                favouriteButton.setBackgroundResource(R.drawable.favourite_button_selector)
                            isFavourite = true
                            activity?.runOnUiThread {
                                Toast.makeText(context, context!!.getString(R.string.song_added, context!!.getString(R.string.favourites)), Toast.LENGTH_SHORT).show()
                            }
                        } else activity?.runOnUiThread {
                            Toast.makeText(context, "${context!!.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    inner class Callback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            when (state?.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    seekBar.isEnabled = true
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
            when (shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)

                PlaybackStateCompat.SHUFFLE_MODE_NONE -> randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ALL -> loopButton.setBackgroundResource(R.drawable.repeat_all_button_selector)

                PlaybackStateCompat.REPEAT_MODE_ONE -> loopButton.setBackgroundResource(R.drawable.repeat_this_button_selector)

                PlaybackStateCompat.REPEAT_MODE_NONE -> loopButton.setBackgroundResource(R.drawable.repeat_not_button_selector)
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                "PositionChanged" -> {
                    seekBar.isEnabled = true
                    if ((activity as MainActivity).mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
                        startSeekBarHandler()
                }
                "Buffered" -> seekBar.secondaryProgress = extras!!.getInt("percent") * 10
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata != null) { // New or same currentSong
                title.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                artist.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                songDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
                duration.text = millisToString(songDuration)
                albumArt.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                songUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                Server.getPlaylist(this@PlayerFragment, "Favourites")
            }

            if ((activity as MainActivity).mediaController.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                playButton.setBackgroundResource(R.drawable.pause_button_selector)
                startSeekBarHandler()
            } else {
                playButton.setBackgroundResource(R.drawable.play_button_selector)
                updateSeekBar()
            }

            // Updates loop button
            when ((activity as MainActivity).mediaController.repeatMode) {
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

            // Updates random button
            if ((activity as MainActivity).mediaController.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                randomButton.setBackgroundResource(R.drawable.shuffle_button_selector)
            } else {
                randomButton.setBackgroundResource(R.drawable.shuffle_not_button_selector)
            }
        }
    }

    inner class GListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_MAX_OFF_PATH = 250 // How much you can derail from a straight line when swiping
        private val SWIPE_MIN_DISTANCE = 120 // How long must the swipe be
        private val SWIPE_MIN_VELOCITY = 120 // How quick must the swipe be

        override fun onShowPress(p0: MotionEvent?) {}

        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
            return true
        }

        override fun onDown(p0: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velX: Float, velY: Float): Boolean {
            // Check if user made a swipe-like motion
            if (abs(e1!!.y - e2!!.y) <= SWIPE_MAX_OFF_PATH && abs(velX) > SWIPE_MIN_VELOCITY && abs(e1.x - e2.x) >= SWIPE_MIN_DISTANCE) {
                // Right to left swipe
                if (e1.x >= e2.x) {
                    (activity as MainActivity).mediaController.transportControls.skipToNext()
                }
                // Left to right swipe
                else {
                    (activity as MainActivity).mediaController.transportControls.skipToPrevious()
                }
                return true
            }
            return false
        }

        override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
            return true
        }

        override fun onLongPress(p0: MotionEvent?) {}

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            favouriteOps()
            return true
        }
    }
}
