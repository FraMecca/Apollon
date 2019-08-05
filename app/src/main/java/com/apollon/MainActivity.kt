package com.apollon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import com.apollon.fragments.LoginFragment
import com.squareup.otto.Bus

class MainActivity : AppCompatActivity(){

    lateinit var player: PlayerService

    lateinit var bus: Bus

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            player = binder.service
            player.bus = bus
            Log.e("CREATE", binder.toString())
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //volumeControlStream = AudioManager.STREAM_MUSIC

        bus = Bus()

        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        /**** GUI ****/
        replaceFragment(LoginFragment(), false)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    fun replaceFragment(frag: Fragment, addToStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main, frag)
        //adds the transaction to a stack so it can be re-executed by pressing the back button
        if (addToStack)
            transaction.addToBackStack("ApollonStack")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
    }
}
