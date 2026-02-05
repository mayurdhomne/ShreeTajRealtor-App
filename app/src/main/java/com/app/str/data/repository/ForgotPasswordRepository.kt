package com.app.str.data.repository

import com.app.str.data.api.ForgotPasswordApiService
import com.app.str.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ForgotPasswordRepository @Inject constructor(
    private val apiService: ForgotPasswordApiService
) {
    
    private val gson = Gson()
    
    private fun parseErrorResponse(errorBody: String?): String {
        return try {
            if (errorBody.isNullOrEmpty()) {
                return "An unknown error occurred"
            }
            
            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
            
            // Check different error fields and return the first non-null one
            when {
                !errorResponse.nonFieldErrors.isNullOrEmpty() -> 
                    errorResponse.nonFieldErrors.first()
                !errorResponse.email.isNullOrEmpty() -> 
                    errorResponse.email.first()
                !errorResponse.password.isNullOrEmpty() -> 
                    errorResponse.password.first()
                errorResponse.message != null -> 
                    errorResponse.message
                errorResponse.error != null -> 
                    errorResponse.error
                errorResponse.detail != null -> 
                    errorResponse.detail
                else -> 
                    "An unknown error occurred"
            }
        } catch (e: Exception) {
            "An error occurred while processing the response"
        }
    }
    
    suspend fun requestOtp(request: ForgotPasswordRequestOtpRequest): Result<ForgotPasswordRequestOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("ForgotPasswordRepository: Requesting OTP for ${request.email}")
                val response = apiService.requestOtp(request)
                
                if (response.isSuccessful && response.body() != null) {
                    println("ForgotPasswordRepository: OTP request successful")
                    Result.Success(response.body()!!)
                } else {
                    println("ForgotPasswordRepository: OTP request failed with code: ${response.code()}")
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("ForgotPasswordRepository: OTP request exception: ${e.message}")
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun verifyOtp(request: ForgotPasswordVerifyOtpRequest): Result<ForgotPasswordVerifyOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("ForgotPasswordRepository: Verifying OTP for ${request.email}")
                val response = apiService.verifyOtp(request)
                
                if (response.isSuccessful && response.body() != null) {
                    println("ForgotPasswordRepository: OTP verification successful")
                    Result.Success(response.body()!!)
                } else {
                    println("ForgotPasswordRepository: OTP verification failed with code: ${response.code()}")
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("ForgotPasswordRepository: OTP verification exception: ${e.message}")
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun resendOtp(request: ForgotPasswordResendOtpRequest): Result<ForgotPasswordResendOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("ForgotPasswordRepository: Resending OTP for ${request.email}")
                val response = apiService.resendOtp(request)
                
                if (response.isSuccessful && response.body() != null) {
                    println("ForgotPasswordRepository: OTP resend successful")
                    Result.Success(response.body()!!)
                } else {
                    println("ForgotPasswordRepository: OTP resend failed with code: ${response.code()}")
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("ForgotPasswordRepository: OTP resend exception: ${e.message}")
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun resetPassword(request: ForgotPasswordResetRequest): Result<ForgotPasswordResetResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("ForgotPasswordRepository: Resetting password for ${request.email}")
                val response = apiService.resetPassword(request)
                
                if (response.isSuccessful && response.body() != null) {
                    println("ForgotPasswordRepository: Password reset successful")
                    Result.Success(response.body()!!)
                } else {
                    println("ForgotPasswordRepository: Password reset failed with code: ${response.code()}")
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("ForgotPasswordRepository: Password reset exception: ${e.message}")
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}
