package com.app.str.data.api

import com.app.str.data.model.DailyReportRequest
import com.app.str.data.model.DailyReportResponse
import com.app.str.data.model.HourlyReportResponse
import com.app.str.data.model.UpdateHourlyReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DailyReportApiService {
    
    @POST("hourly-reports/create/")
    suspend fun submitDailyReport(
        @Body request: DailyReportRequest
    ): Response<DailyReportResponse>
    
    @GET("hourly-reports/")
    suspend fun getHourlyReports(): Response<List<HourlyReportResponse>>
    
    @PUT("hourly-reports/update/{id}/")
    suspend fun updateHourlyReport(
        @Path("id") id: Int,
        @Body request: UpdateHourlyReportRequest
    ): Response<HourlyReportResponse>
    
    @GET("hourly-reports/pending/")
    suspend fun getPendingHourlyReports(): Response<List<DailyReportResponse>>
    
    @GET("work-types/")
    suspend fun getWorkTypes(): Response<List<WorkType>>
    
    @GET("projects/")
    suspend fun getProjects(): Response<List<Project>>
}

// Work Types Response Models
data class WorkType(
    val id: Int,
    val name: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

// Projects Response Models
data class Project(
    val id: Int,
    val name: String,
    val project_type: String? = null,
    val description: String? = null,
    val total_plots: Int? = null,
    val available_plots: Int? = null,
    val sold_plots: Int? = null,
    val remaining_plots: Int? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val launch_date: String? = null,
    val expected_completion_date: String? = null,
    val is_active: Boolean? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val created_by: Int? = null
)