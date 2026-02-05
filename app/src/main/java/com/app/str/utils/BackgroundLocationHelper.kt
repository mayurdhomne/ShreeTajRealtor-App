package com.app.str.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Background Location Helper for use in Workers and Services
 * This does not require an Activity context
 */
object BackgroundLocationHelper {
    
    private const val TAG = "BackgroundLocationHelper"
    private const val LOCATION_TIMEOUT_MS = 30000L // 30 seconds timeout
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    /**
     * Get current location from background context
     * Returns null if location cannot be obtained
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermissions(context)) {
            Log.e(TAG, "Location permissions not granted")
            return null
        }
        
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            try {
                getLocationInternal(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location: ${e.message}")
                null
            }
        }
    }
    
    private suspend fun getLocationInternal(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
            
            // Try to get last known location first
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    } else {
                        // Try to get current location
                        Log.d(TAG, "No last known location, trying current location")
                        getCurrentLocationUpdate(context, fusedLocationClient, continuation)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get last location: ${exception.message}")
                    // Try to get current location
                    getCurrentLocationUpdate(context, fusedLocationClient, continuation)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
    
    private fun getCurrentLocationUpdate(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        continuation: kotlinx.coroutines.CancellableContinuation<Location?>
    ) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        if (location != null) {
                            Log.d(TAG, "Got current location: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        } else {
                            Log.w(TAG, "Current location is null")
                            continuation.resume(null)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get current location: ${exception.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting current location: ${e.message}")
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Get default location coordinates (0, 0) when location cannot be obtained
     * This is used as a fallback for auto-submit
     */
    fun getDefaultLocation(): Pair<Double, Double> {
        return Pair(0.0, 0.0)
    }
}
