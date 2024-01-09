package com.puzzlebooth.main

import android.content.Context
import android.content.SharedPreferences
import com.puzzlebooth.server.utils.UdpBroadcastListener

object UdpBroadcastListener {

    var udpBroadcastListener: UdpBroadcastListener? = null

    fun start(context: Context, sharedPreferences: SharedPreferences) {
        if(udpBroadcastListener != null) {
            udpBroadcastListener = UdpBroadcastListener(context, sharedPreferences, 11791)
            udpBroadcastListener?.startListening()
        }
    }

    fun stop() {
        udpBroadcastListener?.stopListening()
    }
}