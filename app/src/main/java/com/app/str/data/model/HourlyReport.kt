package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class HourlyReportResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("work_types")
    val workTypes: List<WorkTypeItem>,
    @SerializedName("details")
    val details: List<HourlyReportDetail>,
    @SerializedName("report_date")
    val reportDate: String,
    @SerializedName("report_hour")
    val reportHour: Int,
    @SerializedName("location_latitude")
    val locationLatitude: String,
    @SerializedName("location_longitude")
    val locationLongitude: String,
    @SerializedName("work_done")
    val workDone: String,
    @SerializedName("reason_not_done")
    val reasonNotDone: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("user")
    val userId: Int
)

data class WorkTypeItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class HourlyReportDetail(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("work_type")
    val workType: Int,
    @SerializedName("project")
    val project: Int,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("plot_number")
    val plotNumber: String?,
    @SerializedName("customer_response")
    val customerResponse: String?,
    @SerializedName("reason_not_interested")
    val reasonNotInterested: String?,
    @SerializedName("other_reason")
    val otherReason: String?,
    @SerializedName("site_visit_done")
    val siteVisitDone: Boolean,
    @SerializedName("meeting_done")
    val meetingDone: Boolean,
    @SerializedName("booking_done")
    val bookingDone: Boolean,
    @SerializedName("next_followup_date")
    val nextFollowupDate: String?,
    @SerializedName("area")
    val area: String?,
    @SerializedName("rate")
    val rate: String?,
    @SerializedName("total_value")
    val totalValue: String?,
    @SerializedName("tcm")
    val tcm: String?,
    @SerializedName("value_per_sqft")
    val valuePerSqft: String?,
    @SerializedName("feedback")
    val feedback: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class UpdateHourlyReportRequest(
    @SerializedName("report_date")
    val reportDate: String,
    @SerializedName("report_hour")
    val reportHour: Int,
    @SerializedName("location_latitude")
    val locationLatitude: Double,
    @SerializedName("location_longitude")
    val locationLongitude: Double,
    @SerializedName("work_done")
    val workDone: String,
    @SerializedName("reason_not_done")
    val reasonNotDone: String?,
    @SerializedName("work_types")
    val workTypes: List<Int>,
    @SerializedName("details")
    val details: List<UpdateHourlyReportDetail>
)

data class UpdateHourlyReportDetail(
    @SerializedName("work_type")
    val workType: Int,
    @SerializedName("project")
    val project: Int,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("plot_number")
    val plotNumber: String?,
    @SerializedName("customer_response")
    val customerResponse: String?,
    @SerializedName("reason_not_interested")
    val reasonNotInterested: String?,
    @SerializedName("other_reason")
    val otherReason: String?,
    @SerializedName("site_visit_done")
    val siteVisitDone: Boolean,
    @SerializedName("meeting_done")
    val meetingDone: Boolean,
    @SerializedName("booking_done")
    val bookingDone: Boolean,
    @SerializedName("next_followup_date")
    val nextFollowupDate: String?,
    @SerializedName("area")
    val area: String?,
    @SerializedName("rate")
    val rate: String?,
    @SerializedName("total_value")
    val totalValue: String?,
    @SerializedName("tcm")
    val tcm: String?,
    @SerializedName("value_per_sqft")
    val valuePerSqft: String?,
    @SerializedName("feedback")
    val feedback: String?
)
