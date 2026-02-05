package com.app.str.data.api

import com.app.str.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ForgotPasswordApiService {
    
    @POST("forgot-password/request/")
    suspend fun requestOtp(@Body request: ForgotPasswordRequestOtpRequest): Response<ForgotPasswordRequestOtpResponse>
    
    @POST("forgot-password/verify-otp/")
    suspend fun verifyOtp(@Body request: ForgotPasswordVerifyOtpRequest): Response<ForgotPasswordVerifyOtpResponse>
    
    @POST("forgot-password/resend-otp/")
    suspend fun resendOtp(@Body request: ForgotPasswordResendOtpRequest): Response<ForgotPasswordResendOtpResponse>
    
    @POST("forgot-password/reset/")
    suspend fun resetPassword(@Body request: ForgotPasswordResetRequest): Response<ForgotPasswordResetResponse>
}
