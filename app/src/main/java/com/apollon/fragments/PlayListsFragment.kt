package com.apollon.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.adapters.PlaylistAdapter
import com.apollon.classes.Playlist
import com.google.android.material.floatingactionbutton.FloatingActionButton


class PlayListsFragment : Fragment(), View.OnClickListener {

    override fun onClick(p0: View?) {
        (activity as MainActivity).miniPlayer.visibility = View.GONE
    }

    lateinit var mView: View
    //lateinit var newPlaylistButton: FloatingActionButton
    private val playlists : ArrayList<Playlist> = ArrayList()


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.playlists, container, false)
        val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)
       // newPlaylistButton = mView.findViewById(R.id.fab)
        //newPlaylistButton.setOnClickListener(this)
        // Loads elements into the ArrayList
        addPlaylists()
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(context, 1)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = PlaylistAdapter(playlists, requireContext())
        return mView
    }

    // Adds playlists to the empty ArrayList
    private fun addPlaylists() {
        playlists.clear()
        playlists.add(Playlist(0, getString(R.string.all), "https://wallpapercave.com/wp/hjSB3oT.jpg"))
        playlists.add(Playlist(0, getString(R.string.favourites), "https://shaunvislearningportfolio.files.wordpress.com/2014/11/record.jpeg"))
        playlists.add(Playlist(1, "Rock", "https://markmanson.net/wp-content/uploads/2018/04/on-rock-music-and-writing-cover.jpg"))
        playlists.add(Playlist(2, "Classical", "https://wallpaper-gallery.net/images/classical-music-wallpapers/classical-music-wallpapers-19.jpg"))
        playlists.add(Playlist(3, "Techno", "https://i.pinimg.com/originals/01/48/c5/0148c5ffb5de127dd9569b19dec288cb.jpg"))
        playlists.add(Playlist(4, "Reggae", "https://wallpapercave.com/wp/NOiaz6s.jpg"))
    }
}