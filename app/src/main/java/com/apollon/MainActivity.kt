package com.apollon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.apollon.fragments.LoginFragment
import java.text.FieldPosition

class MainActivity : AppCompatActivity(){

    val fm = supportFragmentManager

    lateinit var player: PlayerService


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            player = binder.service
            Log.e("CREATE", binder.toString())
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        Log.e("Service", "start")
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        /**** GUI ****/
        replaceFragment(LoginFragment(), false)
    }

    fun replaceFragment(frag: Fragment, addToStack: Boolean = true) {
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.main, frag)
        //adds the transaction to a stack so it can be re-executed by pressing the back button
        if (addToStack)
            transaction.addToBackStack("ApollonStack")
        transaction.commit()
        fm.executePendingTransactions()
    }

    fun initSong(audioUrl: String){
        player.initMediaPlayer(audioUrl)
    }

    fun play(){
        player.playMedia()
    }

    fun pause(){
        player.pauseMedia()
    }

    fun loop(loop: Boolean){
        player.loopMedia(loop)
    }

    fun seekTo(position: Int){
        player.seekTo(position)
    }

    fun getCurrentPosition(): Int{
        return player.getCurrentPosition()
    }
}
