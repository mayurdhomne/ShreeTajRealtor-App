package com.app.str.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response for get all work plans API
 */
data class WorkPlansAllResponse(
    @SerializedName("filter_type")
    val filterType: String,
    
    @SerializedName("total_count")
    val totalCount: Int,
    
    @SerializedName("data")
    val data: WorkPlansData
)

data class WorkPlansData(
    @SerializedName("user_created")
    val userCreated: List<WorkPlanItem>,
    
    @SerializedName("admin_created")
    val adminCreated: List<WorkPlanItem>
)

data class WorkPlanItem(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("titles")
    val titles: List<WorkPlanTitle>,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("status")
    val status: String, // pending, in_process, completed
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("created_by")
    val createdBy: String,
    
    @SerializedName("coworkers")
    val coworkers: List<Coworker>
)

data class WorkPlanTitle(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?
)

data class Coworker(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("first_name")
    val firstName: String?,
    
    @SerializedName("last_name")
    val lastName: String?,
    
    @SerializedName("username")
    val username: String? = null,
    
    @SerializedName("email")
    val email: String? = null
) {
    fun getFullName(): String {
        return when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            else -> username ?: "Unknown"
        }
    }
}

/**
 * Request for creating a work plan
 */
data class CreateWorkPlanRequest(
    @SerializedName("titles")
    val titles: List<Int>, // Array of title IDs
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("status")
    val status: String = "pending", // pending, in_process, completed
    
    @SerializedName("date")
    val date: String, // Format: YYYY-MM-DD
    
    @SerializedName("coworkers")
    val coworkers: List<Int>? = null // Array of coworker IDs
)

/**
 * Request for updating a work plan
 */
data class UpdateWorkPlanRequest(
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("status")
    val status: String, // pending, in_process, completed
    
    @SerializedName("date")
    val date: String, // Format: YYYY-MM-DD
    
    @SerializedName("coworkers")
    val coworkers: List<Int>? = null // Array of coworker IDs
)

/**
 * Response for create/update work plan
 */
data class WorkPlanResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("titles")
    val titles: List<WorkPlanTitle>,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("created_by")
    val createdBy: String
)

/**
 * Available work title for selection (predefined by admin)
 */
data class AvailableWorkTitle(
    val id: Int,
    val title: String,
    val description: String?
)

/**
 * Response for fetching coworkers
 */
data class CoworkersResponse(
    @SerializedName("coworkers")
    val coworkers: List<Coworker>
)
