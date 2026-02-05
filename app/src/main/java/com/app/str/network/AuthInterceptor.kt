package com.app.str.network

import com.app.str.utils.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    // Event flow for 401 unauthorized responses
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(replay = 0)
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Skip token injection for auth endpoints
        if (shouldSkipTokenInjection(url)) {
            println("AuthInterceptor: Skipping token for URL: $url")
            return chain.proceed(originalRequest)
        }
        
        // Get raw access token (even if expired) - let TokenAuthenticator handle refresh
        val token = runBlocking { tokenManager.getRawAccessToken() }
        println("AuthInterceptor: Retrieved token: ${if (token != null) "Token exists (${token.take(20)}...)" else "No token"}")
        
        val requestWithAuth = if (token != null) {
            println("AuthInterceptor: Adding Authorization header")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            println("AuthInterceptor: No token available for URL: $url")
            originalRequest
        }
        
        val response = chain.proceed(requestWithAuth)
        println("AuthInterceptor: Response code: ${response.code} for URL: $url")
        
        // PERMANENT LOGIN: Don't emit unauthorized events for 401 responses
        // TokenAuthenticator handles 401s automatically via OkHttp's authenticator pattern
        // Emitting events here causes duplicate logout attempts and breaks permanent login
        
        // Note: 401 errors are handled by TokenAuthenticator only - no need for additional handling
        
        return response
    }
    
    private fun shouldSkipTokenInjection(url: String): Boolean {
        val authEndpoints = listOf(
            "api/login/",
            "api/signup/",
            "api/verify-otp/",
            "api/resend-otp/",
            "token/refresh/"  // Skip token injection for refresh endpoint
        )
        
        val shouldSkip = authEndpoints.any { endpoint -> url.contains(endpoint) }
        println("AuthInterceptor: URL: $url, Should skip: $shouldSkip")
        return shouldSkip
    }
}