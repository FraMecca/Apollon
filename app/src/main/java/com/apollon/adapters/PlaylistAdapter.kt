package com.apollon.adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.classes.Playlist
import com.apollon.fragments.SongsFragment
import com.squareup.picasso.Picasso
import com.apollon.R
import com.apollon.Server
import com.apollon.fragments.PlayListsFragment



class PlaylistAdapter(val playlists: ArrayList<Playlist>, val context: Context, val fragment: PlayListsFragment) : RecyclerView.Adapter<PlaylistViewHolder>(), Filterable {

    private val filter = PlaylistFilter()
    private var filteredPlaylists = playlists

    // Gets the number of playlists in the list
    override fun getItemCount(): Int {
        return filteredPlaylists.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(LayoutInflater.from(context).inflate(R.layout.playlist_card, parent, false))
    }

    // Binds each playlist in the ArrayList to a view
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = filteredPlaylists[position]

        //sets the correct title
        when (playlist) {
            is Playlist.AllArtists -> holder.title.text = context.getString(R.string.artists)
            is Playlist.AllAlbums -> holder.title.text = context.getString(R.string.albums)
            is Playlist.AllGenres -> holder.title.text = context.getString(R.string.genres)
            is Playlist.AllPlaylists -> holder.title.text = context.getString(R.string.playlists)
            is Playlist.Favourites -> holder.title.text = context.getString(R.string.Favourites)
            else -> {
                holder.title.text = playlist.title
                holder.title.isSelected = true
                holder.elements.text = String.format(context.getString(R.string.elements), playlist.elements)
            }
        }

        //Loads in the correct image
        when (playlist.img_url) {
            "artist" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.artist))
            "album" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.album))
            "genre" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.genre))
            "favourites" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.favourites))
            else -> Picasso.get().load(playlist.img_url).into(holder.thumbnail)
        }

        //CardView listener
        val target = when (playlist) {
            is Playlist.Album -> SongsFragment.newInstance(playlist)
            is Playlist.Custom -> SongsFragment.newInstance(playlist)
            else -> PlayListsFragment.newInstance(playlist)
        }

        holder.itemView.setOnClickListener { (context as MainActivity).replaceFragment(target) }

        //Custom playlist buttons
        if(playlist is Playlist.Custom) {
            val deleteButton = holder.itemView.findViewById<Button>(R.id.button_delete)
            val editButton = holder.itemView.findViewById<Button>(R.id.button_edit)
            deleteButton.visibility = View.VISIBLE
            editButton.visibility = View.VISIBLE
            //delete click listener
            deleteButton.setOnClickListener {
                //creates alert
                AlertDialog.Builder(context, R.style.AlertStyle)
                        .setTitle(context.getString(R.string.delete_title))
                        .setMessage(context.getString(R.string.delete_message) + " ${playlist.title}?")

                        .setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                            val res = Server.removePlaylist(playlist.title)
                            while (res.get() == null) {
                            }
                            if (res.get() == "ok") {
                                Toast.makeText(context, context.getString(R.string.playlist_deleted, playlist.title), Toast.LENGTH_SHORT).show()
                                (context as MainActivity).refreshFragment(fragment)
                            } else
                                Toast.makeText(context, "${context.getString(R.string.error)}: ${res.get()}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }

            // edit click listener
            editButton.setOnClickListener{
                val editView = LayoutInflater.from(context).inflate(R.layout.modify, null)
                val editText = editView.findViewById<EditText>(R.id.edit_title)
                editText.setText(playlist.title)
                //creates alert
                AlertDialog.Builder(context, R.style.AlertStyle)
                        .setView(editView)
                        .setTitle(context.getString(R.string.edit_title))
                        .setMessage(context.getString(R.string.edit_playlist_message))

                        .setPositiveButton(context.getString(R.string.edit)) { dialog, _ ->
                            Toast.makeText(context, "newname: ${editText.text}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                            Toast.makeText(context, "u r safe: ${playlist.id}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class PlaylistFilter : Filter() {
        override fun performFiltering(s: CharSequence?): FilterResults {
            val res = FilterResults()
            if (s.isNullOrEmpty())
                res.values = playlists
            else {
                val resList = ArrayList<Playlist>()
                playlists.forEach {
                    if (it.title.toLowerCase().contains(s.toString().toLowerCase()))
                        resList.add(it)
                }
                res.values = resList
            }
            return res
        }

        override fun publishResults(s: CharSequence?, r: FilterResults?) {
            filteredPlaylists = r?.values as ArrayList<Playlist>
            notifyDataSetChanged()
        }
    }
}

class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
    val elements = view.findViewById(R.id.elements) as TextView
}