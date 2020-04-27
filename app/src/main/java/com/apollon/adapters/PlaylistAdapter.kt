@file:Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")

package com.apollon.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.apollon.*
import com.apollon.classes.Playlist
import com.apollon.fragments.PlayListsFragment
import com.apollon.fragments.SongsFragment
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

class PlaylistAdapter(var playlists: ArrayList<Playlist>, private val context: Context, private val fragment: PlayListsFragment) : RecyclerView.Adapter<PlaylistViewHolder>(), Filterable, TaskListener {

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
    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = filteredPlaylists[position]

        // sets the correct title
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

        // Loads in the correct image
        when (playlist.img_url) {
            "artist" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.artist))
            "album" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.album))
            "genre" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.genre))
            "favourites" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.favourites))
            "playlist" -> holder.thumbnail.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.playlist))
            else -> Picasso.get().load(playlist.img_url).into(holder.thumbnail)
        }

        // CardView listener
        val target = when (playlist) {
            is Playlist.Album -> SongsFragment.newInstance(playlist)
            is Playlist.Custom -> SongsFragment.newInstance(playlist)
            is Playlist.Favourites -> SongsFragment.newInstance(playlist)
            else -> PlayListsFragment.newInstance(playlist)
        }

        holder.itemView.setOnClickListener { (context as MainActivity).replaceFragment(target as Fragment) }

        // Custom playlist buttons
        if (playlist is Playlist.Custom) {
            val deleteButton = holder.itemView.findViewById<Button>(R.id.button_delete)
            val editButton = holder.itemView.findViewById<Button>(R.id.button_edit)
            deleteButton.visibility = View.VISIBLE
            editButton.visibility = View.VISIBLE
            // delete click listener
            deleteButton.setOnClickListener {
                // creates alert
                AlertDialog.Builder(context, R.style.AlertStyle)
                        .setTitle(context.getString(R.string.delete_title))
                        .setMessage(context.getString(R.string.delete_message) + " ${playlist.title}?")

                        .setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                            Server.removePlaylist(this, playlist.title)
                            dialog.dismiss()
                        }
                        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }

            // edit click listener
            editButton.setOnClickListener {
                val editView = LayoutInflater.from(context).inflate(R.layout.modify, null)
                val editText = editView.findViewById<EditText>(R.id.edit_title)
                editText.setText(playlist.title)
                // creates alert
                AlertDialog.Builder(context, R.style.AlertStyle)
                        .setView(editView)
                        .setTitle(context.getString(R.string.edit_title))
                        .setMessage(context.getString(R.string.edit_playlist_message))

                        .setPositiveButton(context.getString(R.string.edit)) { dialog, _ ->
                            Server.renamePlaylist(this, playlist.title, editText.text.toString())
                            dialog.dismiss()
                        }
                        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            }
        }
    }

    override fun onTaskCompleted(result: TaskResult) {
        if (result is TaskResult.OperationResult) {
            when (result.task) {
                "removePlaylist" ->
                    if (result.error == "")
                        (context as MainActivity).runOnUiThread {
                            Toast.makeText(context, context.getString(R.string.playlist_deleted, result.title), Toast.LENGTH_SHORT).show()
                            context.refreshFragment(fragment)
                        }
                    else
                        (context as MainActivity).runOnUiThread {
                            Toast.makeText(context, "${context.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                        }

                "renamePlaylist" ->
                    when {
                        result.error == "" ->
                            (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, context.getString(R.string.playlist_renamed, result.title), Toast.LENGTH_SHORT).show()
                                context.refreshFragment(fragment)
                            }

                        result.error.contains("same title") ->
                            (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, context.getString(R.string.playlist_rename_fail, result.title), Toast.LENGTH_SHORT).show()
                                context.refreshFragment(fragment)
                            }

                        else ->
                            (context as MainActivity).runOnUiThread {
                                Toast.makeText(context, "${context.getString(R.string.error)}: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                    }
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
                if (it.title.toLowerCase(Locale.ROOT).contains(s.toString().toLowerCase(Locale.ROOT)))
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
