package com.apollon.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.apollon.MainActivity
import com.apollon.R


class LoginFragment : Fragment() {

    lateinit var mView: View
    lateinit var loginButton: Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.login, container, false)
        loginButton = mView.findViewById(R.id.login_btn)
        loginButton.setOnClickListener {
            (activity as MainActivity).replaceFragment(PlayListsFragment(), false)
        }
        return mView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


}