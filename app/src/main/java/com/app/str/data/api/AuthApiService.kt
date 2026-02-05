package com.app.str.data.api

import com.app.str.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    
    @POST("signup/")
    suspend fun signUp(@Body request: SignUpRequest): Response<SignUpResponse>
    
    @POST("verify-otp/")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<SignUpResponse>
    
    @POST("resend-otp/")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<ResendOtpResponse>
    
    @POST("login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("logout/")
    suspend fun logout(@Body request: LogoutRequest): Response<LogoutResponse>

    @POST("token/refresh/")
    suspend fun refreshTokenCreate(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
}