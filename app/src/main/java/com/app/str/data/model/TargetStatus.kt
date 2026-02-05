package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class TargetStatusResponse(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("user_email")
    val userEmail: String?,
    @SerializedName("year")
    val year: Int,
    @SerializedName("monthly_status")
    val monthlyStatus: List<MonthlyStatus>
)

data class MonthlyStatus(
    @SerializedName("month")
    val month: String,
    @SerializedName("target_area")
    val targetArea: Double,
    @SerializedName("sold_area")
    val soldArea: Double,
    @SerializedName("status")
    val status: String, // "red", "green", or "gray"
    @SerializedName("carry_from_last_month")
    val carryFromLastMonth: Double?,
    @SerializedName("carry_forward")
    val carryForward: Double
)
