package com.app.str.utils

import com.app.str.data.api.AuthApiService
import com.app.str.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * TokenAuthenticator implementing permanent login session
 * - Uses token_refresh_create API endpoint for token refresh
 * - Never automatically logs out users (permanent session)
 * - Only clears tokens on explicit user logout
 * - Handles network failures gracefully without logout
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Authenticator {
    
    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_RETRY_ATTEMPTS = 2
    }
    
    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.toString()
        logDebug("Received 401 Unauthorized for URL: $url")
        logDebug("Initiating token refresh process for permanent login session")
        
        // Prevent infinite loops with retry limit
        val responseCount = response.priorResponseCount()
        if (responseCount >= MAX_RETRY_ATTEMPTS) {
            logDebug("Maximum retry attempts ($responseCount) reached for URL: $url")
            logDebug("Stopping retry cycle - PERMANENT SESSION: tokens preserved")
            // DO NOT clear tokens - permanent session requirement
            return null
        }

        return runBlocking {
            performTokenRefresh(response)
        }
    }
    
    /**
     * Performs token refresh using token_refresh_create API
     * If refresh fails, attempts silent login with stored credentials
     * Implements permanent login - never clears tokens automatically
     */
    private suspend fun performTokenRefresh(originalResponse: Response): Request? {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            
            if (refreshToken == null) {
                logDebug("No refresh token available - attempting silent login")
                return attemptSilentLogin(originalResponse)
            }
            
            logDebug("Initiating token refresh API call to token_refresh_create")
            val refreshResponse = authApiService.refreshTokenCreate(RefreshTokenRequest(refreshToken))
            
            handleRefreshResponse(refreshResponse, originalResponse, refreshToken)
            
        } catch (networkException: Exception) {
            logDebug("Network exception during token refresh, attempting silent login")
            attemptSilentLogin(originalResponse)
        }
    }
    
    /**
     * Handles the refresh API response
     */
    private suspend fun handleRefreshResponse(
        refreshResponse: retrofit2.Response<com.app.str.data.model.RefreshTokenResponse>,
        originalResponse: Response,
        oldRefreshToken: String
    ): Request? {
        
        return if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
            val tokens = refreshResponse.body()!!
            logDebug("Token refresh successful - received new tokens")
            
            // Save both new tokens (both are required from token_refresh_create)
            val newRefreshToken = tokens.refresh ?: oldRefreshToken
            val newAccessToken = tokens.access
            
            logTokenInfo("New access token", newAccessToken)
            logTokenInfo("New refresh token", newRefreshToken)
            
            try {
                tokenManager.saveTokens(newAccessToken, newRefreshToken)
                logDebug("New tokens saved successfully")
                
                // Retry original request with new access token
                logDebug("Retrying original request with new access token")
                originalResponse.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                    
            } catch (saveException: Exception) {
                logDebug("Failed to save new tokens: ${saveException.message}")
                // Even if save fails, don't clear tokens - permanent session
                null
            }
            
        } else {
            handleRefreshFailure(refreshResponse.code(), refreshResponse.message())
        }
    }
    
    /**
     * Handles refresh API failures (4xx, 5xx responses)
     * Attempts silent login if refresh fails
     */
    private suspend fun handleRefreshFailure(code: Int, message: String): Request? {
        logDebug("Token refresh failed - HTTP $code: $message")
        
        when (code) {
            401, 403 -> {
                logDebug("Refresh token invalid/expired (HTTP $code) - attempting silent login")
                return attemptSilentLogin(null)
            }
            in 500..599 -> {
                logDebug("Server error during refresh (HTTP $code) - tokens preserved")
                // Server issues shouldn't affect user session
            }
            else -> {
                logDebug("Unexpected refresh failure (HTTP $code) - tokens preserved")
            }
        }
        
        // Return null to stop retry cycle
        return null
    }
    
    /**
     * Attempts silent login using stored user credentials
     */
    private suspend fun attemptSilentLogin(originalResponse: Response?): Request? {
        return try {
            val credentials = tokenManager.getUserCredentials()
            if (credentials == null) {
                logDebug("No stored credentials available for silent login")
                return null
            }
            
            logDebug("Attempting silent login with stored credentials")
            val loginRequest = com.app.str.data.model.LoginRequest(
                email = credentials.first,
                password = credentials.second
            )
            
            val loginResponse = authApiService.login(loginRequest)
            
            if (loginResponse.isSuccessful && loginResponse.body() != null) {
                val loginData = loginResponse.body()!!
                logDebug("Silent login successful - received new tokens")
                
                // Save new tokens
                tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                logDebug("New tokens saved from silent login")
                
                // Retry original request with new access token
                return originalResponse?.request?.newBuilder()
                    ?.header("Authorization", "Bearer ${loginData.accessToken}")
                    ?.build()
                    
            } else {
                logDebug("Silent login failed - HTTP ${loginResponse.code()}: ${loginResponse.message()}")
                // Clear stored credentials if they are invalid
                if (loginResponse.code() == 401) {
                    logDebug("Invalid credentials - clearing stored credentials")
                    tokenManager.clearUserCredentials()
                }
                return null
            }
            
        } catch (e: Exception) {
            logDebug("Exception during silent login: ${e.message}")
            return null
        }
    }
    
    /**
     * Handles network exceptions during token refresh
     * Implements permanent login - never clears tokens on network issues
     */
    private fun handleNetworkException(exception: Exception): Request? {
        logDebug("Network exception during token refresh: ${exception.javaClass.simpleName}")
        logDebug("Exception message: ${exception.message}")
        
        // Network issues are temporary - don't affect permanent session
        logDebug("Permanent session: Preserving tokens despite network failure")
        
        // Log exception for debugging without exposing sensitive data
        if (exception.cause != null) {
            logDebug("Exception cause: ${exception.cause?.javaClass?.simpleName}")
        }
        
        return null
    }
    
    /**
     * Counts prior response attempts to prevent infinite loops
     */
    private fun Response.priorResponseCount(): Int {
        var count = 0
        var response = this.priorResponse
        while (response != null) {
            count++
            response = response.priorResponse
        }
        return count
    }
    
    /**
     * Safe logging that doesn't expose sensitive token data
     */
    private fun logDebug(message: String) {
        println("$TAG: $message")
    }
    
    /**
     * Safe token logging - only shows first few characters for debugging
     */
    private fun logTokenInfo(tokenType: String, token: String) {
        if (token.length >= 20) {
            logDebug("$tokenType: ${token.take(20)}... (${token.length} chars total)")
        } else {
            logDebug("$tokenType: ${token.take(10)}... (${token.length} chars total)")
        }
    }
}
