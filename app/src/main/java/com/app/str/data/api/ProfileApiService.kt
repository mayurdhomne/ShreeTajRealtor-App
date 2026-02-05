package com.app.str.data.api

import com.app.str.data.model.ProfileResponse
import com.app.str.data.model.ProfileUpdateRequest
import com.app.str.data.model.ProfileUpdateResponse
import com.app.str.data.model.ProfileCompletionRequest
import com.app.str.data.model.ProfileCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface ProfileApiService {
    
    @GET("profile/complete/")
    suspend fun getProfile(): Response<ProfileResponse>
    
    @PUT("profile/complete/")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<ProfileUpdateResponse>
    
    @POST("profile/complete/")
    suspend fun completeProfile(@Body request: ProfileCompletionRequest): Response<ProfileCompletionResponse>
}