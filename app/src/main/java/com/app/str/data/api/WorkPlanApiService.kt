package com.app.str.data.api

import com.app.str.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for work plans
 */
interface WorkPlanApiService {
    
    /**
     * Get all work plans for logged-in user only
     * @param filter Optional filter: "daily", "weekly", or "monthly"
     * @param date Optional date filter in format "yyyy-MM-dd"
     */
    @GET("workplans/user/all/")
    suspend fun getAllWorkPlans(
        @Query("filter") filter: String? = null,
        @Query("date") date: String? = null
    ): Response<WorkPlansAllResponse>
    
    /**
     * Create a new work plan
     */
    @POST("attendance/workplans/user/")
    suspend fun createWorkPlan(@Body request: CreateWorkPlanRequest): Response<WorkPlanResponse>
    
    /**
     * Update an existing work plan
     */
    @PUT("attendance/workplans/user/{id}/")
    suspend fun updateWorkPlan(
        @Path("id") id: Int,
        @Body request: UpdateWorkPlanRequest
    ): Response<WorkPlanResponse>
    
    /**
     * Delete a work plan
     */
    @DELETE("attendance/workplans/user/{id}/")
    suspend fun deleteWorkPlan(@Path("id") id: Int): Response<Unit>
    
    /**
     * Get available work plan titles
     */
    @GET("workplan-titles/")
    suspend fun getWorkPlanTitles(): Response<List<AvailableWorkTitle>>
    
    /**
     * Get list of coworkers for assignment
     */
    @GET("workplan/dropdowns")
    suspend fun getCoworkers(): Response<CoworkersResponse>
}
