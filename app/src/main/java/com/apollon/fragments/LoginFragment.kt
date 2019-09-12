package com.apollon.fragments


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.apollon.DoLogin
import com.apollon.MainActivity
import com.apollon.R
import com.apollon.classes.Credentials


class LoginFragment : Fragment() {

    lateinit var mView: View
    lateinit var loginButton: Button

    @SuppressLint("ResourceType")
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Credentials.init(context!!)
        mView = inflater.inflate(R.layout.login, container, false)

        val (user, password) = Credentials.get()
        val userField: EditText = mView.findViewById(R.id.input_username)
        val passField: EditText = mView.findViewById(R.id.input_password)
        userField.text?.clear()
        userField.text?.insert(0, user)
        passField.text?.clear()
        passField.text?.insert(0, password)

        val (server, snd) = Credentials.getServer()
        val (proto, port) = snd
        val serverField: EditText = mView.findViewById(R.id.input_ip)
        val protocolField: Spinner = mView.findViewById(R.id.protocol)
        val portField: EditText = mView.findViewById(R.id.input_port)
        serverField.text?.clear()
        serverField.text?.insert(0, server)
        portField.text?.clear()
        portField.text?.insert(0, port.toString())
        protocolField.setSelection(proto)

        loginButton = mView.findViewById(R.id.login_btn)
        loginButton.setOnClickListener {
            val newUser = userField.text.toString()
            val newPass = passField.text.toString()
            val newPort = portField.text.toString().toInt()
            val newServer = serverField.text.toString()
            val newProto = protocolField.selectedItemId.toInt()
            Credentials.save(newUser, newPass, newServer, newProto, newPort)
            val l = DoLogin(context!!)
            l.execute()
            while (l.done == false){ // TODO animation
                Log.e("Trying login", l.done.toString())
            }
            if(l.result == false){
                Toast.makeText(context!!, l.msg, Toast.LENGTH_LONG).show()
                Log.e("Login","Invalid Login")
            } else {
                (activity as MainActivity).replaceFragment(PlayListsFragment(), false)
            }
        }
        return mView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}
