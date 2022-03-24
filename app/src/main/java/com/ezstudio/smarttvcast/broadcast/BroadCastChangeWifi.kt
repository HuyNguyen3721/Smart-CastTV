package com.ezstudio.smarttvcast.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager

class BroadCastChangeWifi : BroadcastReceiver() {
    var listenerStateChange: (() -> Unit)? = null
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                listenerStateChange?.invoke()
            }
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                listenerStateChange?.invoke()
            }
        }
    }
}