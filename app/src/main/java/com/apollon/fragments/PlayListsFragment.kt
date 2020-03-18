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


class PlayListsFragment : Fragment(), View.OnClickListener {

    private val playlists: ArrayList<Playlist> = ArrayList()
    lateinit var playlist: Playlist
    lateinit var loading: GifImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.playlists, container, false)
        val search = mView.findViewById<SearchView>(R.id.search)
        val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)
        playlist =  if(this.arguments != null && this.arguments!!.containsKey("playlist"))
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
        if(playlist is Playlist.AllPlaylists){
            val addButton = mView.findViewById<Button>(R.id.new_playlist_button)
            addButton.setOnClickListener(this)
            addButton.visibility = View.VISIBLE
        }

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
        playlists.clear()


        val action = when(playlist){
            is Playlist.Begin -> {
                playlists.add(Playlist.AllArtists())
                playlists.add(Playlist.AllAlbums())
                playlists.add(Playlist.AllGenres())
                playlists.add(Playlist.AllPlaylists())
                playlists.add(Playlist.Favourites())
                return // break
            }
            is Playlist.Artist -> Server.getArtist(playlist.id)
            is Playlist.Album -> {assert(false); return} // this should have been forwarded to SongsFragments
            is Playlist.Genre -> Server.getGenre(playlist.id)
            is Playlist.AllAlbums -> Server.getAlbums()
            is Playlist.AllArtists -> Server.getArtists()
            is Playlist.AllGenres -> Server.getGenres()
            is Playlist.AllPlaylists -> Server.getPlaylists()
            is Playlist.Favourites -> {assert(false); return}
            is Playlist.Custom -> {assert(false); return} // this should have been forwarded to SongsFragments
        }
        if(action is ServerPlaylistResult.Future) {
            action.async.execute()
            Log.e("Playlist", "server.execute")
            //Loading gif starts
            //loading.visibility = View.VISIBLE

            while(action.get().size == 0){
                if(action.error() != "") {
                    Toast.makeText(context, action.error(), Toast.LENGTH_LONG).show()
                    loading.visibility = View.GONE
                    return
                }
            }
            //Loading gif ends
            //loading.visibility = View.GONE
        }

        action.get().forEach {
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