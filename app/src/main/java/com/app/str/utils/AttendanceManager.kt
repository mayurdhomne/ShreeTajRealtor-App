package com.app.str.utils

import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.app.str.R
import com.app.str.data.api.AttendanceApiService
import com.app.str.data.model.CheckInRequest
import com.app.str.data.model.CheckOutRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceManager @Inject constructor(
    private val attendanceApiService: AttendanceApiService,
    private val authManager: AuthManager
) {
    
    private var currentDialog: Dialog? = null
    
    fun showCheckInConfirmation(activity: Activity, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // Quick authentication check before showing dialog
        GlobalScope.launch(Dispatchers.IO) {
            val hasTokens = authManager.hasStoredTokens()
            withContext(Dispatchers.Main) {
                if (!hasTokens) {
                    onError("Your session has expired. Please login again to continue.")
                    return@withContext
                }
                
                val locationHelper = LocationHelper(activity)
                
                if (!locationHelper.hasLocationPermissions()) {
                    locationHelper.requestLocationPermissions(activity)
                    onError("Location permissions are required for check-in")
                    return@withContext
                }
                
                showAttendanceDialog(
                    activity = activity,
                    isCheckIn = true,
                    locationHelper = locationHelper,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }
    
    fun showCheckOutConfirmation(activity: Activity, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // Quick authentication check before showing dialog
        GlobalScope.launch(Dispatchers.IO) {
            val hasTokens = authManager.hasStoredTokens()
            withContext(Dispatchers.Main) {
                if (!hasTokens) {
                    onError("Your session has expired. Please login again to continue.")
                    return@withContext
                }
                
                val locationHelper = LocationHelper(activity)
                
                if (!locationHelper.hasLocationPermissions()) {
                    locationHelper.requestLocationPermissions(activity)
                    onError("Location permissions are required for check-out")
                    return@withContext
                }
                
                showAttendanceDialog(
                    activity = activity,
                    isCheckIn = false,
                    locationHelper = locationHelper,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }
    
    private fun showAttendanceDialog(
        activity: Activity,
        isCheckIn: Boolean,
        locationHelper: LocationHelper,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dialog = Dialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_attendance_confirmation, null)
        
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        
        // Setup dialog views
        val ivIcon = view.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        
        // Configure dialog based on action type
        if (isCheckIn) {
            ivIcon.setImageResource(R.drawable.ic_checkin)
            tvTitle.text = "Confirm Check In"
            tvMessage.text = "Are you sure you want to check in now?"
            btnConfirm.text = "Check In"
        } else {
            ivIcon.setImageResource(R.drawable.ic_checkout)
            tvTitle.text = "Confirm Check Out"
            tvMessage.text = "Are you sure you want to check out now?"
            btnConfirm.text = "Check Out"
        }
        
        // Set up button click listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            btnConfirm.isEnabled = false
            btnConfirm.text = if (isCheckIn) "Checking In..." else "Checking Out..."
            
            // Get current location and perform action
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val location = locationHelper.getCurrentLocation()
                    val locationData = LocationData(location.latitude, location.longitude)
                    
                    performAttendanceAction(
                        isCheckIn = isCheckIn,
                        locationData = locationData,
                        context = activity,
                        onSuccess = { message ->
                            GlobalScope.launch(Dispatchers.Main) {
                                dialog.dismiss()
                                onSuccess(message)
                            }
                        },
                        onError = { error ->
                            GlobalScope.launch(Dispatchers.Main) {
                                dialog.dismiss()
                                onError(error)
                            }
                        }
                    )
                } catch (e: Exception) {
                    GlobalScope.launch(Dispatchers.Main) {
                        dialog.dismiss()
                        onError("Failed to get location: ${e.message}")
                    }
                }
            }
        }
        
        currentDialog = dialog
        dialog.show()
    }
    
    private suspend fun performAttendanceAction(
        isCheckIn: Boolean,
        locationData: LocationData,
        context: Activity,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Check authentication
            val isValidSession = authManager.hasStoredTokens()
            
            if (!isValidSession) {
                withContext(Dispatchers.Main) {
                    onError("Your session has expired. Please login again to continue.")
                }
                return
            }
            val response = withContext(Dispatchers.IO) {
                if (isCheckIn) {
                    val request = CheckInRequest(
                        latitude = locationData.latitude.toString(),
                        longitude = locationData.longitude.toString()
                    )
                    attendanceApiService.checkIn(request)
                } else {
                    val request = CheckOutRequest(
                        latitude = locationData.latitude.toString(),
                        longitude = locationData.longitude.toString()
                    )
                    attendanceApiService.checkOut(request)
                }
            }
            
            if (response.isSuccessful) {
                val attendanceResponse = response.body()
                val message = attendanceResponse?.message ?: 
                    if (isCheckIn) "Successfully checked in!" else "Successfully checked out!"
                
                // Check notification permission on first check-in
                if (isCheckIn) {
                    if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                        NotificationPermissionHelper.requestNotificationPermission(context)
                    }
                }
                
                onSuccess(message)
            } else {
                // Try to parse error message from response body
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val gson = com.google.gson.Gson()
                        val errorResponse = gson.fromJson(errorBody, com.app.str.data.model.ErrorResponse::class.java)
                        errorResponse.message ?: errorResponse.detail ?: getGenericErrorMessage(response.code(), isCheckIn)
                    } else {
                        getGenericErrorMessage(response.code(), isCheckIn)
                    }
                } catch (e: Exception) {
                    getGenericErrorMessage(response.code(), isCheckIn)
                }
                onError(errorMessage)
            }
        } catch (e: Exception) {
            onError("Network error: ${e.message}")
        }
    }
    
    fun handleLocationPermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        return when (requestCode) {
            LocationHelper.LOCATION_PERMISSION_REQUEST_CODE -> {
                grantResults.isNotEmpty() && 
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            }
            else -> false
        }
    }
    
    fun dismissCurrentDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }
    
    private fun getGenericErrorMessage(statusCode: Int, isCheckIn: Boolean): String {
        return when (statusCode) {
            401 -> "Authentication expired. Please login again."
            400 -> "Invalid request. Please try again."
            500 -> "Server error. Please try again later."
            else -> "Failed to ${if (isCheckIn) "check in" else "check out"}. Please try again."
        }
    }
}