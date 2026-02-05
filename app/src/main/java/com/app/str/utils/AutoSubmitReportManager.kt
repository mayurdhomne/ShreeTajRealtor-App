package com.app.str.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Manages automatic report submission tracking.
 * If user doesn't submit report within 1 hour of the reminder,
 * the system will auto-submit with:
 * - Current location
 * - work_done = "no"
 * - reason_not_done = "Not Submit Report"
 */
object AutoSubmitReportManager {
    
    private const val TAG = "AutoSubmitReportManager"
    private const val PREF_NAME = "auto_submit_report_prefs"
    private const val KEY_PENDING_REPORT_HOUR = "pending_report_hour"
    private const val KEY_PENDING_REPORT_DATE = "pending_report_date"
    private const val KEY_REMINDER_TIMESTAMP = "reminder_timestamp"
    
    // Auto-submit after 1 hour (in milliseconds)
    private const val AUTO_SUBMIT_DELAY_MS = 60 * 60 * 1000L // 1 hour
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Called when reminder notification is shown.
     * Records the current hour and timestamp for tracking.
     */
    fun recordReminderShown(context: Context) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val timestamp = System.currentTimeMillis()
        
        val prefs = getPreferences(context)
        
        // Check if this is a new pending report (different hour/date)
        val existingHour = prefs.getInt(KEY_PENDING_REPORT_HOUR, -1)
        val existingDate = prefs.getString(KEY_PENDING_REPORT_DATE, "")
        
        // Only update if it's a different hour or date (new report period)
        if (existingHour != currentHour || existingDate != currentDate) {
            prefs.edit().apply {
                putInt(KEY_PENDING_REPORT_HOUR, currentHour)
                putString(KEY_PENDING_REPORT_DATE, currentDate)
                putLong(KEY_REMINDER_TIMESTAMP, timestamp)
                apply()
            }
            Log.d(TAG, "=== NEW PENDING REPORT RECORDED ===")
            Log.d(TAG, "Hour: $currentHour, Date: $currentDate")
            Log.d(TAG, "Timestamp: $timestamp")
            Log.d(TAG, "Auto-submit will trigger at: ${timestamp + AUTO_SUBMIT_DELAY_MS} (1 hour from now)")
        } else {
            Log.d(TAG, "Pending report already recorded for Hour: $currentHour, Date: $currentDate")
            Log.d(TAG, "Existing timestamp: ${prefs.getLong(KEY_REMINDER_TIMESTAMP, 0)}")
        }
    }
    
    /**
     * Check if auto-submit should be triggered.
     * Returns true if:
     * - There's a pending report recorded
     * - More than 1 hour has passed since the reminder was shown
     */
    fun shouldAutoSubmit(context: Context): Boolean {
        val prefs = getPreferences(context)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        val pendingHour = prefs.getInt(KEY_PENDING_REPORT_HOUR, -1)
        val pendingDate = prefs.getString(KEY_PENDING_REPORT_DATE, "")
        val reminderTimestamp = prefs.getLong(KEY_REMINDER_TIMESTAMP, 0)
        
        Log.d(TAG, "=== Checking Auto-Submit Condition ===")
        Log.d(TAG, "Pending: Hour=$pendingHour, Date=$pendingDate")
        Log.d(TAG, "Current: Hour=$currentHour, Date=$currentDate")
        Log.d(TAG, "Reminder Timestamp: $reminderTimestamp")
        
        // No pending report
        if (pendingHour == -1 || reminderTimestamp == 0L || pendingDate.isNullOrEmpty()) {
            Log.d(TAG, "Result: No pending report recorded")
            return false
        }
        
        // Calculate time since reminder
        val currentTime = System.currentTimeMillis()
        val timeSinceReminder = currentTime - reminderTimestamp
        val minutesSinceReminder = timeSinceReminder / 1000 / 60
        
        Log.d(TAG, "Time since reminder: $minutesSinceReminder minutes")
        Log.d(TAG, "Auto-submit threshold: ${AUTO_SUBMIT_DELAY_MS / 1000 / 60} minutes")
        
        // If more than 1 hour has passed since the reminder was shown
        if (timeSinceReminder >= AUTO_SUBMIT_DELAY_MS) {
            Log.d(TAG, "=== AUTO-SUBMIT CONDITION MET ===")
            Log.d(TAG, "$minutesSinceReminder minutes since reminder (threshold: ${AUTO_SUBMIT_DELAY_MS / 1000 / 60} min)")
            return true
        }
        
        val remainingMinutes = (AUTO_SUBMIT_DELAY_MS - timeSinceReminder) / 1000 / 60
        Log.d(TAG, "Result: Not yet time to auto-submit. $remainingMinutes minutes remaining.")
        return false
    }
    
    /**
     * Get the pending report hour for auto-submit
     */
    fun getPendingReportHour(context: Context): Int {
        val prefs = getPreferences(context)
        return prefs.getInt(KEY_PENDING_REPORT_HOUR, -1)
    }
    
    /**
     * Get the pending report date for auto-submit
     */
    fun getPendingReportDate(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_PENDING_REPORT_DATE, "") ?: ""
    }
    
    /**
     * Clear pending report after successful submission (manual or auto)
     */
    fun clearPendingReport(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            remove(KEY_PENDING_REPORT_HOUR)
            remove(KEY_PENDING_REPORT_DATE)
            remove(KEY_REMINDER_TIMESTAMP)
            apply()
        }
        Log.d(TAG, "Cleared pending report tracking")
    }
    
    /**
     * Check if there's a pending report that matches current hour
     * This is used to skip auto-submit if user already submitted manually
     */
    fun hasPendingReportForCurrentHour(context: Context): Boolean {
        val prefs = getPreferences(context)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        val pendingHour = prefs.getInt(KEY_PENDING_REPORT_HOUR, -1)
        val pendingDate = prefs.getString(KEY_PENDING_REPORT_DATE, "")
        
        return pendingHour == currentHour && pendingDate == currentDate
    }
    
    /**
     * DEBUG: Force set pending report for testing auto-submit
     * This sets a pending report with timestamp 1 hour ago so auto-submit triggers immediately
     * ONLY USE FOR TESTING
     */
    fun debugForceAutoSubmit(context: Context) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        // Set timestamp to 65 minutes ago (more than 1 hour)
        val pastTimestamp = System.currentTimeMillis() - (65 * 60 * 1000L)
        
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putInt(KEY_PENDING_REPORT_HOUR, currentHour)
            putString(KEY_PENDING_REPORT_DATE, currentDate)
            putLong(KEY_REMINDER_TIMESTAMP, pastTimestamp)
            apply()
        }
        
        Log.d(TAG, "=== DEBUG: Forced auto-submit setup ===")
        Log.d(TAG, "Hour: $currentHour, Date: $currentDate")
        Log.d(TAG, "Timestamp set to 65 minutes ago: $pastTimestamp")
        Log.d(TAG, "Auto-submit should trigger on next worker run")
    }
    
    /**
     * DEBUG: Get current pending report status for debugging
     */
    fun debugGetStatus(context: Context): String {
        val prefs = getPreferences(context)
        val pendingHour = prefs.getInt(KEY_PENDING_REPORT_HOUR, -1)
        val pendingDate = prefs.getString(KEY_PENDING_REPORT_DATE, "")
        val reminderTimestamp = prefs.getLong(KEY_REMINDER_TIMESTAMP, 0)
        
        val currentTime = System.currentTimeMillis()
        val timeSinceReminder = if (reminderTimestamp > 0) (currentTime - reminderTimestamp) / 1000 / 60 else -1
        
        return """
            Pending Hour: $pendingHour
            Pending Date: $pendingDate
            Reminder Timestamp: $reminderTimestamp
            Minutes Since Reminder: $timeSinceReminder
            Should Auto-Submit: ${timeSinceReminder >= 60}
        """.trimIndent()
    }
}
