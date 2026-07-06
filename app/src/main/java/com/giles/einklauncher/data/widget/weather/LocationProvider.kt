package com.giles.einklauncher.data.widget.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class DeviceLocation(val latitude: Double, val longitude: Double, val label: String)

/**
 * Resolves the device's coarse location for weather. Only used when the user has NOT set a
 * manual location. Requires ACCESS_COARSE_LOCATION; returns null when the permission is
 * absent or no fix is available.
 */
class LocationProvider(private val context: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    suspend fun current(): DeviceLocation? {
        if (!hasPermission()) return null
        return runCatching {
            val last = fused.lastLocation.await()
            val loc = last ?: fused.getCurrentLocation(
                CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build(),
                CancellationTokenSource().token,
            ).await()
            loc?.let {
                DeviceLocation(it.latitude, it.longitude, reverseGeocode(it.latitude, it.longitude))
            }
        }.getOrNull()
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                results?.firstOrNull()?.let { a ->
                    a.locality ?: a.subAdminArea ?: a.adminArea
                }
            }.getOrNull() ?: "Current location"
        }
}
