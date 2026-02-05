package com.app.str.data.model

import com.google.gson.annotations.SerializedName

/**
 * Main attendance summary response from API
 */
data class AttendanceSummary(
    @SerializedName("month")
    val month: Int,
    
    @SerializedName("year")
    val year: Int,
    
    @SerializedName("total_days_in_month")
    val totalDaysInMonth: Int,
    
    @SerializedName("summary")
    val summary: AttendanceSummaryDetails,
    
    @SerializedName("today")
    val today: TodayAttendance,
    
    @SerializedName("attendance_calendar")
    val attendanceCalendar: List<AttendanceCalendarDay>
) {
    // Backward compatibility properties
    val totalPresentDays: Int
        get() = summary.presentDays.toInt()
    
    val totalAbsentDays: Int
        get() = summary.absentDays.toInt()
    
    val lastCheckInTime: String?
        get() = today.checkInTime
    
    val lastCheckOutTime: String?
        get() = today.checkOutTime
    
    val lastDate: String?
        get() = today.date
}

/**
 * Summary details with attendance counts
 */
data class AttendanceSummaryDetails(
    @SerializedName("present_days")
    val presentDays: Double = 0.0,
    
    @SerializedName("half_days")
    val halfDays: Double = 0.0,
    
    @SerializedName("absent_days")
    val absentDays: Double = 0.0,
    
    @SerializedName("paid_attendance")
    val paidAttendance: Double = 0.0,
    
    @SerializedName("future_days")
    val futureDays: Int = 0
)

/**
 * Today's attendance details
 */
data class TodayAttendance(
    @SerializedName("date")
    val date: String? = null,
    
    @SerializedName("check_in_time")
    val checkInTime: String? = null,
    
    @SerializedName("check_out_time")
    val checkOutTime: String? = null,
    
    @SerializedName("status")
    val status: String? = null
)

/**
 * Individual calendar day attendance
 */
data class AttendanceCalendarDay(
    @SerializedName("date")
    val date: String,
    
    @SerializedName("day_number")
    val dayNumber: Int,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("status_name")
    val statusName: String,
    
    @SerializedName("check_in_time")
    val checkInTime: String? = null,
    
    @SerializedName("check_out_time")
    val checkOutTime: String? = null,
    
    @SerializedName("is_half_day")
    val isHalfDay: Boolean = false
) {
    companion object {
        const val STATUS_PRESENT = "P"
        const val STATUS_ABSENT = "A"
        const val STATUS_HALF_DAY = "H"
        const val STATUS_FUTURE = "-"
    }
}