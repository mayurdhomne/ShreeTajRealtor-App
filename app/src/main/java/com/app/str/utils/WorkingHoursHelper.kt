package com.app.str.utils

import android.util.Log
import java.util.Calendar

object WorkingHoursHelper {
    
    private const val WORK_START_HOUR = 9  // 9:00 AM
    private const val WORK_END_HOUR = 21   // 9:00 PM
    
    /**
     * Check if current time is within working hours (9:00 AM - 9:00 PM)
     * @return true if current time is within working hours, false otherwise
     */
    fun isWithinWorkingHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in WORK_START_HOUR until WORK_END_HOUR
    }
    
    /**
     * Check if user has checked in today
     * @param lastCheckInTime last check-in time from API (format: "HH:MM:SS" or null)
     * @param lastCheckOutTime last check-out time from API (format: "HH:MM:SS" or null)
     * @return true if user is currently checked in (has check-in but no check-out)
     */
    fun isCheckedIn(lastCheckInTime: String?, lastCheckOutTime: String?): Boolean {
        // If no check-in time, user hasn't checked in
        if (lastCheckInTime == null) {
            return false
        }
        
        // If check-in exists but no check-out, user is checked in
        if (lastCheckOutTime == null) {
            return true
        }
        
        // If both exist, user has already checked out
        return false
    }
    
    /**
     * Check if WorkManager should run
     * @param lastCheckInTime last check-in time from API
     * @param lastCheckOutTime last check-out time from API
     * @return true if both conditions are met: within working hours AND user is checked in
     */
    fun shouldRunHourlyCheck(lastCheckInTime: String?, lastCheckOutTime: String?): Boolean {
        val withinHours = isWithinWorkingHours()
        val checkedIn = isCheckedIn(lastCheckInTime, lastCheckOutTime)
        return withinHours && checkedIn
    }
}
