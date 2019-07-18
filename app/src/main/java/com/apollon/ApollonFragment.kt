package com.apollon

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ApollonFragment : Fragment(){

    lateinit var mView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment, container, false)
        return mView
    }

    fun updateTextView() {
        mView.findViewById<TextView>(R.id.text1)?.text = "prova"
    }


}