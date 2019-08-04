package com.apollon.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.classes.Playlist
import com.apollon.fragments.SongsFragment
import com.squareup.picasso.Picasso
import java.util.*

class PlaylistAdapter(val playlists: ArrayList<Playlist>, val context: Context) : RecyclerView.Adapter<PlaylistViewHolder>() {
    // Gets the number of playlists in the list
    override fun getItemCount(): Int {
        return playlists.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(LayoutInflater.from(context).inflate(R.layout.playlist_card, parent, false))
    }

    // Binds each playlist in the ArrayList to a view
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.title.text = playlist.title

        //Thumbnail download
        Picasso.get().load(playlist.img_url).into(holder.thumbnail)

        //CardView listener
        holder.itemView.setOnClickListener {
            (context as MainActivity).replaceFragment(SongsFragment.newInstance(playlist))
        }
    }
}

class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
}