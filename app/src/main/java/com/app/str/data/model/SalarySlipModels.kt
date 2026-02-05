package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class SalarySlipResponse(
    @SerializedName("month") val month: Int,
    @SerializedName("year") val year: Int,
    @SerializedName("monthly_salary") val monthlySalary: Double,
    @SerializedName("working_days") val workingDays: Int,
    @SerializedName("daily_salary") val dailySalary: Double,
    @SerializedName("present_days") val presentDays: Int,
    @SerializedName("absent_days") val absentDays: Int,
    @SerializedName("allowed_leaves") val allowedLeaves: Int,
    @SerializedName("unpaid_absences") val unpaidAbsences: Int,
    @SerializedName("half_day_count") val halfDayCount: Double,
    @SerializedName("half_day_deduction") val halfDayDeduction: Double,
    @SerializedName("absence_deduction") val absenceDeduction: Double,
    @SerializedName("target_penalty") val targetPenalty: Double,
    @SerializedName("gross_salary") val grossSalary: Double,
    @SerializedName("total_deduction") val totalDeduction: Double,
    @SerializedName("net_salary") val netSalary: Double,
    @SerializedName("sales_sum") val salesSum: Double,
    @SerializedName("target_area") val targetArea: Double,
    @SerializedName("attendance_calendar") val attendanceCalendar: List<String>
)

data class SalarySlipRequest(
    val year: Int,
    val month: Int
)

// UI Model for spinner items
data class MonthYear(
    val month: Int,
    val year: Int,
    val displayName: String
) {
    companion object {
        fun generateMonthYearList(): List<MonthYear> {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            
            val list = mutableListOf<MonthYear>()
            
            // Add current year months up to current month
            for (month in 1..currentMonth) {
                list.add(MonthYear(
                    month = month,
                    year = currentYear,
                    displayName = "${monthNames[month - 1]} $currentYear"
                ))
            }
            
            // Add previous year months
            for (month in 1..12) {
                list.add(MonthYear(
                    month = month,
                    year = currentYear - 1,
                    displayName = "${monthNames[month - 1]} ${currentYear - 1}"
                ))
            }
            
            return list.reversed() // Show latest first
        }
    }
}