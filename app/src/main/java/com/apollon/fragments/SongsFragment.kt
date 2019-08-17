package com.apollon.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.adapters.SongAdapter
import com.apollon.classes.Playlist
import com.apollon.classes.Song
import com.squareup.picasso.Picasso


class SongsFragment : Fragment() {

    lateinit var mView: View
    private val songs: ArrayList<Song> = ArrayList()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
            mView = inflater.inflate(R.layout.songs, container, false)
            val playlist = arguments?.getSerializable("playlist") as Playlist
            val playlistThumbnail = mView.findViewById<ImageView>(R.id.playlist_thumbnail)
            val playlistToolbar = mView.findViewById<Toolbar>(R.id.playlist_toolbar)
            val recyclerView = mView.findViewById<RecyclerView>(R.id.recycler_view)

            Picasso.get().load(playlist.img_url).into(playlistThumbnail)
            (activity as MainActivity).setSupportActionBar(playlistToolbar)
            playlistToolbar.title = playlist.title
            // Loads elements into the ArrayList
            addSongs()
        // Creates a Grid Layout Manager
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Access the RecyclerView Adapter and load the data into it
        recyclerView.adapter = SongAdapter(songs, requireContext())
        return mView
    }

    // Adds songs to the empty ArrayList
    private fun addSongs() {
        songs.clear()
        songs.add(Song(0, "Bohemian Rhapsody", "Queen", "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
        songs.add(Song(1, "Stairway to Heaven", "Led Zeppelin", "https://img.discogs.com/LUE7GmvTK-dCLpvZxVSYPY2L0s8=/fit-in/300x300/filters:strip_icc():format(jpeg):mode_rgb():quality(40)/discogs-images/R-7192711-1477636938-5046.jpeg.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"))
        songs.add(Song(2, "Imagine", "John Lennon", "https://img.cdandlp.com/2013/09/imgL/116189369.jpg","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"))
        songs.add(Song(3, "Smells Like Teen Spirit", "Nirvana", "https://s.mxmcdn.net/images-storage/albums/7/0/0/8/6/4/11468007_350_350.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"))
        songs.add(Song(4, "One", "Metallica", "https://upload.wikimedia.org/wikipedia/en/thumb/b/bd/Metallica_-_...And_Justice_for_All_cover.jpg/220px-Metallica_-_...And_Justice_for_All_cover.jpg","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"))
        songs.add(Song(5, "Hotel California", "Eagles", "https://upload.wikimedia.org/wikipedia/en/4/49/Hotelcalifornia.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"))
        songs.add(Song(6, "Comfortably Numb", "Pink Floyd", "https://cdnrockol-rockolcomsrl.netdna-ssl.com/oBpuq6jXfVAuU6VWXf_2Uu-qxJA=/702x526/smart/rockol-img/img/foto/upload/6af6a9a0d246464f976bef5193823322.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"))
        songs.add(Song(7, "Hey Jude", "The Beatles", "https://www.popsike.com/pix/20170507/322507905430.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"))
        songs.add(Song(8, "Sweet Child O'Mine", "Guns N' Roses", "https://img.discogs.com/MRplvxFAbLGEUWn9dAH3G-X-HiA=/fit-in/600x600/filters:strip_icc():format(jpeg):mode_rgb():quality(90)/discogs-images/R-6808907-1466725043-6016.jpeg.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"))
        songs.add(Song(9, "Lose Yourself", "Eminem", "https://www.significatocanzoni.it/wp-content/uploads/2013/01/lose_yourself.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"))
        songs.add(Song(10, "Bohemian Rhapsody", "Queen", "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
    }

    companion object {

        fun newInstance(playlist: Playlist): SongsFragment {
            val args = Bundle()
            args.putSerializable("playlist", playlist)
            val fragment = SongsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}