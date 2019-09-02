package com.apollon.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.SingleAlbum
import com.apollon.adapters.SongAdapter
import com.apollon.classes.Playlist
import com.apollon.classes.Song
import com.squareup.picasso.Picasso


class SongsFragment : Fragment() {

    lateinit var mView: View
    private val songs: ArrayList<Song> = ArrayList()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
            mView = inflater.inflate(R.layout.songs, container, false)
            val playlist = arguments?.getSerializable("playlist") as Playlist
            Log.e("PLAYLIST", playlist.id)
            val playlistThumbnail = mView.findViewById<ImageView>(R.id.playlist_thumbnail)
            val playlistToolbar = mView.findViewById<Toolbar>(R.id.playlist_toolbar)
            val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)

            Picasso.get().load(playlist.img_url).into(playlistThumbnail)
            (activity as MainActivity).setSupportActionBar(playlistToolbar)
            playlistToolbar.title = playlist.title
            // Loads elements into the ArrayList
            addSongs(playlist.id)
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = SongAdapter(songs, requireContext())
        return mView
    }

    // Adds songs to the empty ArrayList
    private fun addSongs(uri: String) {
        songs.clear()
        val album = SingleAlbum(context!!, uri)
        album.execute()
        while(album.result.size == 0){} // TODO : animation for waiting
        album.result.forEach { songs.add(it) }

    }

    companion object {

        fun newInstance(playlist: Playlist): SongsFragment {
            val args = Bundle()
            args.putSerializable("playlist", playlist)
            val fragment = SongsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}