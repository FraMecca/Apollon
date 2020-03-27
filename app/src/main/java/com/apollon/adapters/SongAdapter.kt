package com.apollon.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.classes.PlaylistSong
import com.apollon.classes.Song
import com.apollon.fragments.PlayerFragment
import com.squareup.picasso.Picasso
import java.util.*
import com.apollon.R
import com.apollon.Server
import com.apollon.ServerPlaylistResult
import com.apollon.fragments.SongsFragment


class SongAdapter(val playlistTitle: String, val songs: ArrayList<Song>, val context: Context, val fragment: SongsFragment) : RecyclerView.Adapter<SongViewHolder>(), Filterable {

    private val filter = SongFilter()
    private var filteredSongs = songs

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

        popupMenu.setOnMenuItemClickListener { it ->
            when (it.itemId) {
                R.id.action_add_favourite -> {
                    Toast.makeText(context, "Song ${song.id} added to favourites", Toast.LENGTH_SHORT).show()
                }
                R.id.action_add_playlist -> {
                    val action = Server.getPlaylists()
                    if (action is ServerPlaylistResult.Future) {
                        action.async.execute()

                        while (action.get().size == 0 && action.error() != "No Playlists") {
                            if (action.error() != "") {
                                Toast.makeText(context, action.error(), Toast.LENGTH_LONG).show()
                                break
                            }
                        }
                    }
                    val menu = PopupMenu(context, view)
                    action.get().forEach {
                        menu.menu.add(it.title)
                    }
                    menu.setOnMenuItemClickListener { item ->
                        val res = Server.addSong(item.title.toString(), song.id)
                        while (res.get() == null) {
                        }
                        when {
                            res.get() == "ok" -> Toast.makeText(context, context.getString(R.string.song_added, song.title, item.title), Toast.LENGTH_SHORT).show()
                            res.get().toString().contains("already in the playlist") -> Toast.makeText(context, context.getString(R.string.already_in, song.title, item.title), Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(context, "${context.getString(R.string.error)}: ${res.get()}", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    menu.show()
                }
                R.id.action_remove -> {
                    val res = Server.removeSong(playlistTitle, song.id)
                    while (res.get() == null) {
                    }
                    when {
                        res.get() == "ok" -> {
                            Toast.makeText(context, context.getString(R.string.song_removed, song.title, playlistTitle), Toast.LENGTH_SHORT).show()
                            (context as MainActivity).refreshFragment(fragment)
                        }
                        else -> Toast.makeText(context, "${context.getString(R.string.error)}: ${res.get()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class SongFilter : Filter() {
        override fun performFiltering(s: CharSequence?): FilterResults {
            val res = FilterResults()
            if (s.isNullOrEmpty())
                res.values = songs
            else {
                val resList = ArrayList<Song>()
                val query = s.toString().toLowerCase()
                songs.forEach {
                    if (it.title.toLowerCase().contains(query) or it.artist.toLowerCase().contains(query))
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
