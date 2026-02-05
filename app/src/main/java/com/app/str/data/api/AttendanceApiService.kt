package com.app.str.data.api

import com.app.str.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API service for attendance and target status endpoints
 * These require authentication
 */
interface AttendanceApiService {
    
    @GET("user/{user_id}/target-status/")
    suspend fun getTargetStatus(@Path("user_id") userId: Int): Response<TargetStatusResponse>
    
    @GET("user/{user_id}/target-status/")
    suspend fun getTargetStatusByYear(@Path("user_id") userId: Int, @Query("year") year: String): Response<TargetStatusResponse>
    
    @POST("attendance/check-in/")
    suspend fun checkIn(@Body request: CheckInRequest): Response<AttendanceResponse>
    
    @POST("attendance/check-out/")
    suspend fun checkOut(@Body request: CheckOutRequest): Response<AttendanceResponse>
    
    @GET("attendance/summary/")
    suspend fun getAttendanceSummary(): Response<AttendanceSummary>
    
    @GET("target/summary/")
    suspend fun getTargetSummary(): Response<TargetSummary>
}
