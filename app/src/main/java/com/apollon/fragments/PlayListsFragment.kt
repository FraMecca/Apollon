package com.apollon.fragments

import android.app.AlertDialog
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.adapters.PlaylistAdapter
import com.apollon.classes.Playlist
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import com.apollon.*
import pl.droidsonroids.gif.GifImageView


class PlayListsFragment : Fragment(), TaskListener, View.OnClickListener {

    private val playlists: ArrayList<Playlist> = ArrayList()
    lateinit var playlist: Playlist
    lateinit var loading: GifImageView
    lateinit var recyclerView: RecyclerView
    lateinit var selectedPlaylist: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.e("PlaylistFragment", "OnCreateView")
        val mView = inflater.inflate(R.layout.playlists, container, false)
        val search = mView.findViewById<SearchView>(R.id.search)
        recyclerView = mView.findViewById(R.id.recycler_view)
        playlist = if (this.arguments != null && this.arguments!!.containsKey("playlist"))
            this.arguments!!.get("playlist") as Playlist
        else
            Playlist.Begin()

        loading = mView.findViewById(R.id.loading)

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String?): Boolean {
                (recyclerView.adapter as PlaylistAdapter).filter.filter(s)
                return false
            }
        })
        if (playlist is Playlist.AllPlaylists) {
            val addButton = mView.findViewById<Button>(R.id.new_playlist_button)
            addButton.setOnClickListener(this)
            addButton.visibility = View.VISIBLE
        }
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(context, 1)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = PlaylistAdapter(playlists, requireContext(), this)

        // Loads elements into the ArrayList
        addPlaylists()

        return mView
    }
    //New playlist button
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
                            selectedPlaylist = editView.findViewById<EditText>(R.id.edit_title).text.toString()
                            Server.createPlaylist(this, selectedPlaylist)
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }
        }
    }

    // Adds playlists to the empty ArrayList
    private fun addPlaylists() {
        playlists.clear()


        when (playlist) {
            is Playlist.Begin -> {
                playlists.add(Playlist.AllArtists())
                playlists.add(Playlist.AllAlbums())
                playlists.add(Playlist.AllGenres())
                playlists.add(Playlist.AllPlaylists())
                playlists.add(Playlist.Favourites())
                return // break
            }
            is Playlist.Artist -> Server.getArtist(this, playlist.id)
            is Playlist.Album -> {
                assert(false); return
            }
            is Playlist.Genre -> Server.getGenre(this, playlist.title)
            is Playlist.AllAlbums -> Server.getAlbums(this)
            is Playlist.AllArtists -> Server.getArtists(this)
            is Playlist.AllGenres -> Server.getGenres(this)
            is Playlist.AllPlaylists -> Server.getPlaylists(this)
            is Playlist.Favourites -> {
                assert(false); return
            }
            is Playlist.Custom -> {
                assert(false); return
            } // this should have been forwarded to SongsFragments
        }
    }

    override fun onTaskCompleted(result: TaskResult) {
        when (result) {
            is TaskResult.ServerPlaylistResult -> {
                if (result.error == "") {
                    playlists.clear()
                    result.result?.forEach { playlists.add(it) }
                    // Access the RecyclerView Adapter and load the data into it
                    activity?.runOnUiThread {
                        (recyclerView.adapter as PlaylistAdapter).playlists = playlists
                        (recyclerView.adapter as PlaylistAdapter).notifyDataSetChanged()
                    }
                } else
                    activity?.runOnUiThread {
                        Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                    }
            }

            is TaskResult.OperationResult -> {
                when (result.task) {
                    "createPlaylist" -> {
                        when{
                            result.error == "" -> {
                                activity?.runOnUiThread {
                                    Toast.makeText(context, "Playlist $selectedPlaylist created", Toast.LENGTH_SHORT).show()
                                }
                                Server.getPlaylists(this)
                            }
                            result.error.contains("There is a playlist with the same title and user already") -> {
                                activity?.runOnUiThread {
                                    Toast.makeText(context, context!!.getString(R.string.already_exists, selectedPlaylist), Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> activity?.runOnUiThread {
                                Toast.makeText(context, "Error: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        fun newInstance(playlist: Playlist): PlayListsFragment {
            val args = Bundle()
            args.putSerializable("playlist", playlist)
            val fragment = PlayListsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}