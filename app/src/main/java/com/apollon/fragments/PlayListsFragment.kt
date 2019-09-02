package com.apollon.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.R
import com.apollon.adapters.PlaylistAdapter
import com.apollon.classes.Playlist
import android.widget.EditText
import android.widget.Toast
import com.apollon.AllAlbums


class PlayListsFragment : Fragment(), View.OnClickListener {

    private val playlists: ArrayList<Playlist> = ArrayList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.playlists, container, false)
        val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)
        mView.findViewById<Button>(R.id.new_playlist_button).setOnClickListener(this)

        // Loads elements into the ArrayList
        addPlaylists()
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(context, 1)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = PlaylistAdapter(playlists, requireContext())
        return mView
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.new_playlist_button -> {
                val editView = LayoutInflater.from(context).inflate(R.layout.modify, null)
                //creates alert
                AlertDialog.Builder(context, R.style.AlertStyle)
                        .setTitle(getString(R.string.new_playlist))
                        .setMessage(getString(R.string.edit_playlist_message))
                        .setView(editView)

                        .setPositiveButton(getString(R.string.create)) { dialog, _ ->
                            Toast.makeText(context, "long live playlist: ${editView.findViewById<EditText>(R.id.edit_title).text}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            Toast.makeText(context, "too bad", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }

        }
    }

    // Adds playlists to the empty ArrayList
    private fun addPlaylists() {
        val server = AllAlbums(context!!)
        server.execute()
        playlists.clear()

        playlists.add(Playlist(0, getString(R.string.all), "https://wallpapercave.com/wp/hjSB3oT.jpg"))
        playlists.add(Playlist(1, getString(R.string.favourites), "https://shaunvislearningportfolio.files.wordpress.com/2014/11/record.jpeg"))
        playlists.add(Playlist(2, "Rock", "https://markmanson.net/wp-content/uploads/2018/04/on-rock-music-and-writing-cover.jpg"))
        playlists.add(Playlist(3, "Classical", "https://wallpaper-gallery.net/images/classical-music-wallpapers/classical-music-wallpapers-19.jpg"))
        playlists.add(Playlist(4, "Techno", "https://i.pinimg.com/originals/01/48/c5/0148c5ffb5de127dd9569b19dec288cb.jpg"))
        playlists.add(Playlist(5, "Reggae", "https://wallpapercave.com/wp/NOiaz6s.jpg"))
        playlists.add(Playlist(6, "Prova", "https://img.discogs.com/LUE7GmvTK-dCLpvZxVSYPY2L0s8=/fit-in/300x300/filters:strip_icc():format(jpeg):mode_rgb():quality(40)/discogs-images/R-7192711-1477636938-5046.jpeg.jpg"))
    }
}