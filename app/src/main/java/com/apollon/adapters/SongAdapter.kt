package com.apollon.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.classes.Song
import com.apollon.fragments.PlayerFragment
import com.squareup.picasso.Picasso
import java.util.*

class SongAdapter(val songs : ArrayList<Song>, val context: Context) : RecyclerView.Adapter<SongViewHolder>(){
    // Gets the number of songs in the list
    override fun getItemCount(): Int {
        return songs.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(LayoutInflater.from(context).inflate(R.layout.song_card, parent, false))
    }

    // Binds each `(activity as MainActivity).currentSong` in the ArrayList to a view
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist

        //Thumbnail download
        Picasso.get().load(song.img_url).into(holder.thumbnail)

        //Menu listener
        holder.menu.setOnClickListener{showPopUpMenu(it, song.id)}

        //CardView listener
        holder.itemView.setOnClickListener{
            (context as MainActivity).replaceFragment(PlayerFragment())
            context.player.initMedia(songs, position)
        }
    }

    private fun showPopUpMenu(view : View, songId : Int){
        val popupMenu = PopupMenu(context, view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.song_menu, popupMenu.menu)
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.action_add_favourite -> {
                    Toast.makeText(context, "Song $songId added to favourites", Toast.LENGTH_SHORT).show()
                }
                R.id.action_play_next -> {
                    Toast.makeText(context, "Song $songId added to queue", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }
}

class SongViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val artist = view.findViewById(R.id.artist) as TextView
    val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
    val menu = view.findViewById(R.id.menu) as ImageView
}
