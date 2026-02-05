package com.app.str.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermissions()) {
            continuation.resumeWithException(SecurityException("Location permissions not granted"))
            return@suspendCancellableCoroutine
        }

        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()
        
        // Try to get last known location first
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        // If no last known location, request current location
                        requestCurrentLocationUpdate(continuation)
                    }
                }
                .addOnFailureListener { exception ->
                    // If failed to get last location, request current location
                    requestCurrentLocationUpdate(continuation)
                }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
        
        continuation.invokeOnCancellation {
            // Cancel location updates if coroutine is cancelled
        }
    }
    
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String = suspendCancellableCoroutine { continuation ->
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        continuation.resume(formatAddress(addresses[0]))
                    } else {
                        continuation.resume("${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    continuation.resume(formatAddress(addresses[0]))
                } else {
                    continuation.resume("${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}")
                }
            }
        } catch (e: Exception) {
            continuation.resume("${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}")
        }
    }
    
    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()
        
        address.subLocality?.let { addressParts.add(it) }
        address.locality?.let { addressParts.add(it) }
        address.subAdminArea?.let { if (it != address.locality) addressParts.add(it) }
        address.adminArea?.let { addressParts.add(it) }
        
        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: "${String.format("%.4f", address.latitude)}, ${String.format("%.4f", address.longitude)}"
        }
    }
    
    private fun requestCurrentLocationUpdate(continuation: kotlinx.coroutines.CancellableContinuation<Location>) {
        try {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000L)
                .setMaxUpdateDelayMillis(15000L)
                .build()
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.resumeWithException(Exception("Unable to get current location"))
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double
) {
    fun toLocationString(): String {
        return "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"
    }
}