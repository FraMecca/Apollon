@file:Suppress("UNCHECKED_CAST")

package com.apollon.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.apollon.*
import com.apollon.classes.PlaylistSong
import com.apollon.classes.Song
import com.apollon.fragments.PlayerFragment
import com.squareup.picasso.Picasso
import java.util.*
import com.apollon.fragments.SongsFragment


class SongAdapter(val playlistTitle: String, var songs: ArrayList<Song>, val context: Context, val fragment: SongsFragment) : RecyclerView.Adapter<SongViewHolder>(), TaskListener, Filterable {

    private val filter = SongFilter()
    private var filteredSongs = songs
    lateinit var selectedView: View
    lateinit var selectedSong: Song

    // Gets the number of songs in the list
    override fun getItemCount(): Int {
        return filteredSongs.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(LayoutInflater.from(context).inflate(R.layout.song_card, parent, false))
    }

    // Binds each `(activity as MainActivity).currentSong` in the ArrayList to a view
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = filteredSongs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist
        holder.title.isSelected = true
        holder.artist.isSelected = true

        //Thumbnail download
        if (song.img_url.isNotEmpty())
            Picasso.get().load(song.img_url).into(holder.thumbnail)

        //Menu listener
        holder.menu.setOnClickListener { showPopUpMenu(it, song) }

        //CardView listener
        holder.itemView.setOnClickListener {
            (context as MainActivity).replaceFragment(PlayerFragment())
            context.player.initMedia(songs, position)
        }
    }

    private fun showPopUpMenu(view: View, song: Song) {
        val popupMenu = PopupMenu(context, view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.song_menu, popupMenu.menu)
        if (song is PlaylistSong)
            popupMenu.menu.findItem(R.id.action_remove).isVisible = true
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add_favourite -> Server.addSong(this, "Favourites", song.id)

                R.id.action_add_playlist -> {
                    selectedView = view
                    selectedSong = song
                    Server.getPlaylists(this)
                }
                R.id.action_remove -> {
                    Server.removeSong(this, playlistTitle, song.id)
                }
            }
            true
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    override fun onTaskCompleted(result: TaskResult) {
        when (result) {
            is TaskResult.ServerPlaylistResult -> {
                if (result.error == "") {
                    val menu = PopupMenu(context, selectedView)
                    result.result?.forEach {
                        menu.menu.add(it.title)
                    }
                    menu.setOnMenuItemClickListener { item ->
                        Server.addSong(this, item.title.toString(), selectedSong.id)
                        true
                    }
                    (context as Activity).runOnUiThread {
                        menu.show()
                    }
                } else
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "${context.getString(R.string.error)}: ${result.error}", Toast.LENGTH_LONG).show()
                    }
            }

            is TaskResult.OperationResult -> {
                when (result.task) {
                    "addSong" -> {
                        when {
                            result.error == "" ->
                                if (result.title == "Favourites") {
                                    (context as MainActivity).runOnUiThread {
                                        Toast.makeText(context, context.getString(R.string.song_added, context.getString(R.string.Favourites)), Toast.LENGTH_SHORT).show()
                                    }
                                } else (context as MainActivity).runOnUiThread {
                                    Toast.makeText(context, context.getString(R.string.song_added, result.title), Toast.LENGTH_SHORT).show()
                                }

                            result.error.contains("already in the playlist") -> {
                                (context as MainActivity).runOnUiThread {
                                    Toast.makeText(context, context.getString(R.string.already_in, result.title), Toast.LENGTH_SHORT).show()
                                }
                            }

                            else -> (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, "${context.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    "removeSong" -> {
                        if (result.error == "")
                            (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, context.getString(R.string.song_removed, result.title), Toast.LENGTH_SHORT).show()
                                Server.getPlaylist(fragment, playlistTitle)
                            }
                        else
                            (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, "${context.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }

    inner class SongFilter : Filter() {
        override fun performFiltering(s: CharSequence?): FilterResults {
            val res = FilterResults()
            if (s.isNullOrEmpty())
                res.values = songs
            else {
                val resList = ArrayList<Song>()
                val query = s.toString().toLowerCase(Locale.ROOT)
                songs.forEach {
                    if (it.title.toLowerCase(Locale.ROOT).contains(query) or it.artist.toLowerCase(Locale.ROOT).contains(query))
                        resList.add(it)
                }
                res.values = resList
            }
            return res
        }

        override fun publishResults(s: CharSequence?, r: FilterResults?) {
            filteredSongs = r?.values as ArrayList<Song>
            notifyDataSetChanged()
        }
    }
}

class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val artist = view.findViewById(R.id.artist) as TextView
    val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
    val menu = view.findViewById(R.id.menu) as ImageView
}