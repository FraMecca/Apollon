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
import com.apollon.*
import com.apollon.adapters.SongAdapter
import com.apollon.classes.Playlist
import com.apollon.classes.Song
import com.squareup.picasso.Picasso
import kotlin.reflect.typeOf


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
            addSongs(playlist as Playlist.Album) // TODO, assert or something else, improve costraints
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = SongAdapter(songs, requireContext())
        return mView
    }

    // Adds songs to the empty ArrayList
    private fun addSongs(playlist: Playlist.Album) {
        val uri = playlist.id
        songs.clear()
        val req = Server.getAlbum(context!!, uri)
        if (req is ServerSongsResult.Future) {
            req.async.execute()
            while (req.get().size == 0) {
            } // TODO : animation for waiting
        }
        req.get().forEach { songs.add(it) }

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