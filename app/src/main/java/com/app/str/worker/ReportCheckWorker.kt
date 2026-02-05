package com.app.str.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.str.data.api.AttendanceApiService
import com.app.str.data.api.DailyReportApiService
import com.app.str.data.api.HourlyReportApiService
import com.app.str.data.model.DailyReportRequest
import com.app.str.utils.AutoSubmitReportManager
import com.app.str.utils.BackgroundLocationHelper
import com.app.str.utils.NotificationHelper
import com.app.str.utils.WorkingHoursHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Background worker that checks hourly if user needs to submit report
 * Workflow:
 * 1. Worker starts automatically when app launches
 * 2. Runs every 15 minutes continuously
 * 3. Checks attendance status via API to see if user is checked in
 * 4. Validates working hours (9 AM - 9 PM) and check-in status
 * 5. If conditions met, calls hourly report API
 * 6. Shows notification if report submission needed
 * 7. Auto-submits report with "No workdone" if user doesn't submit within 1 hour
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReportCheckWorkerEntryPoint {
    fun hourlyReportApiService(): HourlyReportApiService
    fun attendanceApiService(): AttendanceApiService
    fun dailyReportApiService(): DailyReportApiService
}
@HiltWorker
class ReportCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val hourlyReportApiService: HourlyReportApiService,
    private val attendanceApiService: AttendanceApiService,
    private val dailyReportApiService: DailyReportApiService
) : CoroutineWorker(appContext, workerParams) {

    // Fallback constructor for standard WorkManager instantiation
    constructor(context: Context, workerParams: WorkerParameters) : this(
        appContext = context,
        workerParams = workerParams,
        hourlyReportApiService = EntryPointAccessors.fromApplication(
            context,
            ReportCheckWorkerEntryPoint::class.java
        ).hourlyReportApiService(),
        attendanceApiService = EntryPointAccessors.fromApplication(
            context,
            ReportCheckWorkerEntryPoint::class.java
        ).attendanceApiService(),
        dailyReportApiService = EntryPointAccessors.fromApplication(
            context,
            ReportCheckWorkerEntryPoint::class.java
        ).dailyReportApiService()
    )
    
    companion object {
        private const val TAG = "ReportCheckWorker"
        const val WORK_NAME = "hourly_report_check_work"
        private const val AUTO_SUBMIT_REASON = "Not Submit Report"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "=== ReportCheckWorker started ===")
        Log.d(TAG, "Current time: ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())}")
        
        // Create notification channel first to ensure it exists
        NotificationHelper.createNotificationChannel(applicationContext)
        
        return try {
            // Step 1: Check if auto-submit should be triggered first (PRIORITY CHECK)
            val shouldAutoSubmit = AutoSubmitReportManager.shouldAutoSubmit(applicationContext)
            Log.d(TAG, "Should auto-submit: $shouldAutoSubmit")
            
            if (shouldAutoSubmit) {
                Log.d(TAG, "=== AUTO-SUBMIT TRIGGERED ===")
                val autoSubmitResult = performAutoSubmit()
                if (autoSubmitResult) {
                    Log.d(TAG, "Auto-submit successful - showing notification")
                    NotificationHelper.showAutoSubmitNotification(applicationContext)
                    // Clear pending report after successful auto-submit
                    AutoSubmitReportManager.clearPendingReport(applicationContext)
                    return Result.success()
                } else {
                    Log.e(TAG, "Auto-submit failed - will retry on next run")
                }
            }
            
            // Step 2: Check attendance summary to verify if user is checked in
            val attendanceResponse = attendanceApiService.getAttendanceSummary()
            
            if (!attendanceResponse.isSuccessful) {
                Log.e(TAG, "Failed to fetch attendance summary: ${attendanceResponse.code()}")
                return Result.retry()
            }
            
            val attendanceSummary = attendanceResponse.body()
            val lastCheckInTime = attendanceSummary?.lastCheckInTime
            val lastCheckOutTime = attendanceSummary?.lastCheckOutTime
            
            Log.d(TAG, "Check-in: $lastCheckInTime, Check-out: $lastCheckOutTime")
            
            // Step 3: Verify working hours (9 AM - 9 PM) and check-in status
            val isWithinHours = WorkingHoursHelper.isWithinWorkingHours()
            val isCheckedIn = WorkingHoursHelper.isCheckedIn(lastCheckInTime, lastCheckOutTime)
            Log.d(TAG, "Working hours check: isWithinHours=$isWithinHours, isCheckedIn=$isCheckedIn")
            
            if (!WorkingHoursHelper.shouldRunHourlyCheck(lastCheckInTime, lastCheckOutTime)) {
                Log.d(TAG, "Outside working hours or user not checked in - skipping hourly check")
                return Result.success()
            }
            
            Log.d(TAG, "Within working hours and user is checked in - proceeding with hourly check")
            
            // Step 4: Call the hourly report API
            val response = hourlyReportApiService.checkHourlyReport()
            
            if (response.isSuccessful) {
                val message = response.body()?.message
                Log.d(TAG, "API Response: $message")
                
                // Check if the user needs to submit the report
                if (message != null && message.contains("need to submit", ignoreCase = true)) {
                    Log.d(TAG, "User needs to submit report - showing notification and recording reminder")
                    
                    // Record that reminder was shown (for auto-submit tracking)
                    AutoSubmitReportManager.recordReminderShown(applicationContext)
                    
                    try {
                        NotificationHelper.showReportReminderNotification(applicationContext)
                        Log.d(TAG, "Notification shown successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to show notification", e)
                    }
                } else {
                    Log.d(TAG, "Report already submitted - clearing pending report if any")
                    // Clear pending report since user has submitted
                    AutoSubmitReportManager.clearPendingReport(applicationContext)
                }
                
                Result.success()
            } else {
                Log.e(TAG, "API call failed with code: ${response.code()}")
                // Retry on API failure
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReportCheckWorker", e)
            // Retry on exception
            Result.retry()
        }
    }
    
    /**
     * Auto-submit report with:
     * - Current location (or default 0,0 if not available)
     * - work_done = "no"
     * - reason_not_done = "Not Submit Report"
     */
    private suspend fun performAutoSubmit(): Boolean {
        return try {
            Log.d(TAG, "=== Starting Auto-Submit Process ===")
            
            // Get pending report details
            val reportHour = AutoSubmitReportManager.getPendingReportHour(applicationContext)
            val reportDate = AutoSubmitReportManager.getPendingReportDate(applicationContext)
            
            Log.d(TAG, "Pending Report - Hour: $reportHour, Date: $reportDate")
            
            if (reportHour == -1 || reportDate.isEmpty()) {
                Log.e(TAG, "ERROR: No pending report found for auto-submit")
                return false
            }
            
            // Get current location (try up to 30 seconds)
            Log.d(TAG, "Getting current location...")
            val location = BackgroundLocationHelper.getCurrentLocation(applicationContext)
            
            val latitude: Double
            val longitude: Double
            
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                Log.d(TAG, "Location obtained: $latitude, $longitude")
            } else {
                // Use default location if not available
                latitude = 0.0
                longitude = 0.0
                Log.w(TAG, "Location not available, using default: 0.0, 0.0")
            }
            
            // Format location coordinates
            val formattedLatitude = String.format(Locale.US, "%.6f", latitude)
            val formattedLongitude = String.format(Locale.US, "%.6f", longitude)
            
            // Create auto-submit request
            val request = DailyReportRequest(
                reportDate = reportDate,
                reportHour = reportHour,
                locationLatitude = formattedLatitude,
                locationLongitude = formattedLongitude,
                workDone = "no",
                reasonNotDone = AUTO_SUBMIT_REASON,
                workTypes = null,
                details = emptyList()
            )
            
            Log.d(TAG, "=== Auto-Submit Request Details ===")
            Log.d(TAG, "Date: $reportDate")
            Log.d(TAG, "Hour: $reportHour")
            Log.d(TAG, "Location: $formattedLatitude, $formattedLongitude")
            Log.d(TAG, "Work Done: no")
            Log.d(TAG, "Reason: $AUTO_SUBMIT_REASON")
            
            Log.d(TAG, "Calling API to submit report...")
            
            // Submit the report
            val response = dailyReportApiService.submitDailyReport(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "=== AUTO-SUBMIT SUCCESSFUL ===")
                Log.d(TAG, "Response code: ${response.code()}")
                true
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "=== AUTO-SUBMIT FAILED ===")
                Log.e(TAG, "Response code: ${response.code()}")
                Log.e(TAG, "Error body: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== AUTO-SUBMIT EXCEPTION ===")
            Log.e(TAG, "Exception: ${e.message}", e)
            false
        }
    }
}
