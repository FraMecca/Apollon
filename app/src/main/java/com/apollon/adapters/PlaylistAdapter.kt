package com.apollon.adapters

import android.app.AlertDialog
import android.content.Context
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
        holder.itemView.setOnClickListener { (context as MainActivity).replaceFragment(SongsFragment.newInstance(playlist)) }

        //delete click listener
        holder.itemView.findViewById<Button>(R.id.button_delete).setOnClickListener {
            //creates alert
            AlertDialog.Builder(context, R.style.AlertStyle)
                    .setTitle(context.getString(R.string.delete_title))
                    .setMessage(context.getString(R.string.delete_message) + " ${playlist.title}?")

                    .setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                        Toast.makeText(context, "u r ded: ${playlist.id}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                        Toast.makeText(context, "u r safe: ${playlist.id}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .create()
                    .show()
        }

        // edit click listener
        holder.itemView.findViewById<Button>(R.id.button_edit).setOnClickListener {
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

class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
}