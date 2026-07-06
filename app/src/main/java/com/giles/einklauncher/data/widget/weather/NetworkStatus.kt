package com.giles.einklauncher.data.widget.weather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Lightweight connectivity check so the weather row can fall back gracefully offline. */
object NetworkStatus {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
