package com.apollon.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.*
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
        val search = mView.findViewById<SearchView>(R.id.search)
        val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String?): Boolean {
                (recyclerView.adapter as SongAdapter).filter.filter(s)
                return false
            }
        })
        when (playlist.img_url) {
            "genre" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.genre))
            "favourites" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.favourites))
            else -> Picasso.get().load(playlist.img_url).into(playlistThumbnail)
        }

        (activity as MainActivity).setSupportActionBar(playlistToolbar)
        playlistToolbar.title = playlist.title
        // Loads elements into the ArrayList
        addSongs(playlist) // TODO, assert or something else, improve costraints
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = SongAdapter(playlist.title, songs, requireContext(), this)
        return mView
    }

    // Adds songs to the empty ArrayList
    private fun addSongs(playlist: Playlist) {
        val uri = playlist.id
        songs.clear()
        val req = when(playlist){
            is Playlist.Album -> Server.getAlbum(uri)
            is Playlist.Custom -> Server.getPlaylist(uri)
            is Playlist.Favourites -> Server.getPlaylist(uri)
            else -> {assert(false); return}
        }
        if (req is ServerSongsResult.Future) {
            req.async.execute()
            while (req.get().size == 0 && req.error() != "No Tracks") {
                if (req.error() != "") {
                    Toast.makeText(context, req.error(), Toast.LENGTH_LONG).show()
                    return
                }
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