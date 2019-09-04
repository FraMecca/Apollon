package com.apollon.fragments

import android.app.AlertDialog
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
import android.widget.Toast
import com.apollon.*


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
        val playlist: Playlist = if(this.arguments != null && this.arguments!!.containsKey("playlist"))
                this.arguments!!.get("playlist") as Playlist
        else
            Playlist.Begin()
        playlists.clear()


        val action = when(playlist){
            is Playlist.Begin -> {
                playlists.add(Playlist.AllArtists())
                playlists.add(Playlist.AllAlbums())
                playlists.add(Playlist.AllGenres())
                return // break
            }
            is Playlist.Artist -> SingleArtist(context!!, playlist.id)
            is Playlist.Album -> {assert(false); AllAlbums(context!!)} // this should have been forwarded to SongsFragments
            is Playlist.Genre -> SingleGenre(context!!, playlist.id)
            is Playlist.AllAlbums -> AllAlbums(context!!)
            is Playlist.AllArtists -> AllArtists(context!!)
            is Playlist.AllGenres -> AllGenres(context!!)
        }
        action.execute()
        Log.e("Playlist", "server.execute")

        while(action.result.size == 0){
            // Log.e("Playlist", "Waiting") TODO: animation
        }
        action.result.forEach {
            playlists.add(it)
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