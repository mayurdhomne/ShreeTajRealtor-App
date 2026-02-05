package com.crm.realestate.data.models

import com.google.gson.annotations.SerializedName

/**
 * Data class for Work Plan
 */
data class WorkPlan(
    @SerializedName("id")
    val id: Int,

    @SerializedName("overall_progress")
    val overallProgress: Double?,

    @SerializedName("plan_type")
    val planType: String, // daily, weekly, monthly

    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("end_date")
    val endDate: String?,

    @SerializedName("remarks")
    val remarks: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("employee")
    val employee: Int?,

    @SerializedName("employee_id")
    val employeeId: String?
)

/**
 * Data class for Work Detail based on API response
 */
data class WorkDetail(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("work_plan")
    val workPlan: Int,
    
    @SerializedName("employee_id")
    val employeeId: String,
    
    @SerializedName("plan_type")
    val planType: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("target_quantity")
    val targetQuantity: Long,
    
    @SerializedName("achieved_quantity")
    val achievedQuantity: Int,
    
    @SerializedName("status")
    val status: String, // pending, in_progress, completed, not_achieved
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Request model for updating work detail status
 */
data class WorkDetailUpdateRequest(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("achieved_quantity")
    val achievedQuantity: Int,
    
    @SerializedName("status")
    val status: String
)

/**
 * Response wrapper for work details list
 */
data class WorkDetailsResponse(
    @SerializedName("results")
    val results: List<WorkDetail>? = null,
    
    @SerializedName("count")
    val count: Int? = null,
    
    @SerializedName("next")
    val next: String? = null,
    
    @SerializedName("previous")
    val previous: String? = null
)

/**
 * Response wrapper for work plans list
 */
data class WorkPlansResponse(
    @SerializedName("results")
    val results: List<WorkPlan>? = null,
    
    @SerializedName("count")
    val count: Int? = null,
    
    @SerializedName("next")
    val next: String? = null,
    
    @SerializedName("previous")
    val previous: String? = null
)

/**
 * Filter options for work details
 */
data class WorkDetailFilter(
    val status: String = "all", // all, pending, in_progress, completed, not_achieved
    val planType: String = "all" // all, daily, weekly, monthly
)

/**
 * Extended work detail with work plan information
 */
data class WorkDetailWithPlan(
    val id: Int,
    val workPlanId: Int,
    val planType: String,
    val remarks: String?,
    val overallProgress: Double?,
    val title: String,
    val targetQuantity: Long,
    val achievedQuantity: Int,
    val status: String,
    val updatedAt: String?
)