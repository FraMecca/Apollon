package com.apollon.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
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

class SongsFragment : Fragment(), TaskListener {

    private lateinit var mView: View
    private val songs: ArrayList<Song> = ArrayList()
    lateinit var playlist: Playlist
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.songs, container, false)
        playlist = arguments?.getSerializable("playlist") as Playlist
        val playlistThumbnail = mView.findViewById<ImageView>(R.id.playlist_thumbnail)
        val playlistToolbar = mView.findViewById<Toolbar>(R.id.playlist_toolbar)
        val search = mView.findViewById<SearchView>(R.id.search)
        recyclerView = mView.findViewById(R.id.recycler_view)

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
            "all" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.all))
            "genre" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.genre))
            "favourites" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.favourites))
            "playlist" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.playlist))
            "artist" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.artist))
            "album" -> playlistThumbnail.setImageBitmap(BitmapFactory.decodeResource(context?.resources, R.drawable.album))
            else -> Picasso.get().load(playlist.img_url).into(playlistThumbnail)
        }

        (activity as MainActivity).setSupportActionBar(playlistToolbar)
        if (playlist is Playlist.AllSongs)
            playlistToolbar.title = context?.getString(R.string.all)
        else
            playlistToolbar.title = playlist.title
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = SongAdapter(playlist.title, songs, requireContext(), this)
        // Loads elements into the ArrayList
        addSongs(playlist)
        return mView
    }

    // Adds songs to the empty ArrayList
    private fun addSongs(playlist: Playlist) {
        val uri = playlist.id
        songs.clear()
        when (playlist) {
            is Playlist.AllSongs -> Server.getSongs(this)
            is Playlist.Album -> Server.getAlbum(this, uri)
            is Playlist.Custom -> Server.getPlaylist(this, uri)
            is Playlist.Favourites -> Server.getPlaylist(this, uri)
            else -> {
                assert(false); return
            }
        }
    }

    override fun onTaskCompleted(result: TaskResult) {
        if (result is TaskResult.ServerSongsResult) {
            if (result.error == "") {
                songs.clear()
                result.result?.forEach { songs.add(it) }
                // Access the RecyclerView Adapter and load the data into it
                activity?.runOnUiThread {
                    (recyclerView.adapter as SongAdapter).songs = songs

                    (recyclerView.adapter as SongAdapter).notifyDataSetChanged()
                }
            } else
                activity?.runOnUiThread {
                    Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                }
        }
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
