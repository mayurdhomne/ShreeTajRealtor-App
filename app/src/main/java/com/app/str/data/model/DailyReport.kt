package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class DailyReportRequest(
    @SerializedName("report_date")
    val reportDate: String,
    
    @SerializedName("report_hour")
    val reportHour: Int,
    
    @SerializedName("location_latitude")
    val locationLatitude: String?,
    
    @SerializedName("location_longitude")
    val locationLongitude: String?,
    
    @SerializedName("work_done")
    val workDone: String, // "yes" or "no"
    
    @SerializedName("reason_not_done")
    val reasonNotDone: String?,
    
    @SerializedName("work_types")
    val workTypes: List<Int>?,
    
    @SerializedName("details")
    val details: List<WorkDetail>
)

data class WorkDetail(
    @SerializedName("work_type")
    val workType: Int?,
    
    @SerializedName("project")
    val project: Int?,
    
    @SerializedName("customer_name")
    val customerName: String?,
    
    @SerializedName("mobile_number")
    val mobileNumber: String?,
    
    @SerializedName("plot_number")
    val plotNumber: String?,
    
    @SerializedName("customer_response")
    val customerResponse: String?, // "interested", "not_interested", "not_sure"
    
    @SerializedName("reason_not_interested")
    val reasonNotInterested: String?,
    
    @SerializedName("other_reason")
    val otherReason: String?,
    
    @SerializedName("site_visit_done")
    val siteVisitDone: Boolean?,
    
    @SerializedName("meeting_done")
    val meetingDone: Boolean?,
    
    @SerializedName("booking_done")
    val bookingDone: Boolean?,
    
    @SerializedName("next_followup_date")
    val nextFollowupDate: String?,
    
    @SerializedName("area")
    val area: Double?,
    
    @SerializedName("rate")
    val rate: Int?,
    
    @SerializedName("total_value")
    val totalValue: Long?,
    
    @SerializedName("tcm")
    val tcm: String?,
    
    @SerializedName("value_per_sqft")
    val valuePerSqft: Int?,
    
    @SerializedName("feedback")
    val feedback: String?
)

data class DailyReportResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("success")
    val success: Boolean
)