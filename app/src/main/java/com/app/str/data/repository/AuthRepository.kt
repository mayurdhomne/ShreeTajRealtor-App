package com.app.str.data.repository

import com.app.str.data.api.AuthApiService
import com.app.str.data.api.ProfileApiService
import com.app.str.data.model.*
import com.app.str.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val profileApiService: ProfileApiService,
    private val tokenManager: TokenManager
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
    
    suspend fun signUp(request: SignUpRequest): Result<SignUpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.signUp(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun verifyOtp(request: OtpRequest): Result<SignUpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyOtp(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun resendOtp(request: ResendOtpRequest): Result<ResendOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.resendOtp(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("AuthRepository: Making login API call...")
                val response = apiService.login(request)
                println("AuthRepository: API response code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    println("AuthRepository: Login successful, access token: ${loginResponse.accessToken.take(20)}...")
                    println("AuthRepository: Login successful, refresh token: ${loginResponse.refreshToken.take(20)}...")
                    
                    // Save tokens on successful login
                    try {
                        println("AuthRepository: About to save tokens...")
                        tokenManager.saveTokens(
                            loginResponse.accessToken,
                            loginResponse.refreshToken
                        )
                        
                        // Save user credentials for automatic silent login (always enabled)
                        try {
                            println("AuthRepository: Enabling silent login and saving credentials...")
                            tokenManager.setSilentLoginEnabled(true)
                            tokenManager.saveUserCredentials(request.email, request.password)
                            println("AuthRepository: Silent login enabled and credentials saved automatically")
                        } catch (credError: Exception) {
                            println("AuthRepository: Warning - Failed to save user credentials: ${credError.message}")
                            // Don't fail login if credential saving fails
                        }
                        
                        println("AuthRepository: Tokens saved successfully, returning success result")
                        Result.Success(loginResponse)
                    } catch (tokenError: Exception) {
                        println("AuthRepository: Failed to save tokens: ${tokenError.message}")
                        tokenError.printStackTrace()
                        Result.Error("Failed to save authentication data: ${tokenError.message}")
                    }
                } else {
                    println("AuthRepository: Login failed with code: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    println("AuthRepository: Error body: $errorBody")
                    val errorMessage = parseErrorResponse(errorBody)
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("AuthRepository: Login exception: ${e.message}")
                e.printStackTrace()
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun completeProfile(request: ProfileCompletionRequest): Result<ProfileCompletionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = profileApiService.completeProfile(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun refreshToken(): Result<RefreshTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken == null) {
                    println("AuthRepository: No refresh token available")
                    return@withContext Result.Error("No refresh token available")
                }
                
                println("AuthRepository: Attempting to refresh token using token_refresh_create endpoint...")
                val response = apiService.refreshTokenCreate(RefreshTokenRequest(refreshToken))
                
                if (response.isSuccessful && response.body() != null) {
                    val refreshResponse = response.body()!!
                    println("AuthRepository: Token refresh successful")
                    println("AuthRepository: New access token received: ${refreshResponse.access.take(20)}...")
                    println("AuthRepository: New refresh token: ${refreshResponse.refresh?.take(20) ?: "null (keeping existing)"}")
                    
                    // Save new tokens
                    // If refresh token is not in response, keep the existing one by passing the current refresh token
                    try {
                        val tokenToSave = refreshResponse.refresh ?: refreshToken
                        println("AuthRepository: Saving access token and refresh token...")
                        
                        tokenManager.saveTokens(
                            accessToken = refreshResponse.access,
                            refreshToken = tokenToSave
                        )
                        println("AuthRepository: New tokens saved successfully")
                        Result.Success(refreshResponse)
                    } catch (tokenError: Exception) {
                        println("AuthRepository: Failed to save refreshed tokens: ${tokenError.message}")
                        tokenError.printStackTrace()
                        Result.Error("Failed to save refreshed tokens: ${tokenError.message}")
                    }
                } else {
                    println("AuthRepository: Token refresh failed with code: ${response.code()}")
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                println("AuthRepository: Token refresh exception: ${e.message}")
                e.printStackTrace()
                Result.Error("Network error during token refresh: ${e.message}", e)
            }
        }
    }
    
    suspend fun logout() {
        tokenManager.logout()
    }
    
    suspend fun clearTokens() {
        tokenManager.clearTokens()
    }
    
    suspend fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }
    
    suspend fun getValidAccessToken(): String? {
        return tokenManager.getAccessToken()
    }
    
    /**
     * Get TokenManager instance for advanced token operations
     */
    fun getTokenManager(): TokenManager {
        return tokenManager
    }
}