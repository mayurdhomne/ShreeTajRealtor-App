package com.app.str.utils

import com.app.str.data.repository.AuthRepository
import com.app.str.network.AuthInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val authInterceptor: AuthInterceptor
) {
    
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Public flows for UI to observe
    val authState: StateFlow<AuthState> = tokenManager.authState
    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn
    
    // Events for navigation
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(replay = 0)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()
    
    init {
        // PERMANENT LOGIN: Removed unauthorized event listener
        // TokenAuthenticator handles 401s automatically - no need for additional handling
        
        // Initialize auth state on app start
        managerScope.launch {
            tokenManager.initializeAuthState()
            checkAuthenticationStatus()
        }
    }
    
    /**
     * Check current authentication status and emit appropriate navigation events
     * PERMANENT LOGIN: Focus on token existence rather than validity
     */
    suspend fun checkAuthenticationStatus() {
        println("AuthManager: Checking authentication status...")
        try {
            // PERMANENT LOGIN: Check if any tokens exist first (don't validate)
            val hasStoredTokens = hasStoredTokens()
            println("AuthManager: Has stored tokens: $hasStoredTokens")
            
            if (hasStoredTokens) {
                // Tokens exist - navigate to dashboard (TokenAuthenticator will handle refresh if needed)
                println("AuthManager: Tokens found, emitting NavigateToDashboard")
                _navigationEvents.emit(NavigationEvent.NavigateToDashboard)
            } else {
                // No tokens at all - need to login
                println("AuthManager: No tokens found, emitting NavigateToLogin")
                _navigationEvents.emit(NavigationEvent.NavigateToLogin)
            }
        } catch (e: Exception) {
            // On any error, check if tokens exist before redirecting
            println("AuthManager: Error during auth check: ${e.message}")
            val hasTokens = try { hasStoredTokens() } catch (ex: Exception) { false }
            if (hasTokens) {
                println("AuthManager: Tokens exist despite error, emitting NavigateToDashboard")
                _navigationEvents.emit(NavigationEvent.NavigateToDashboard)
            } else {
                println("AuthManager: No tokens found, emitting NavigateToLogin")
                _navigationEvents.emit(NavigationEvent.NavigateToLogin)
            }
        }
    }
    
    /**
     * Handle successful login
     */
    suspend fun handleLoginSuccess() {
        println("AuthManager: Handling login success, emitting navigation event")
        _navigationEvents.emit(NavigationEvent.NavigateToDashboard)
    }
    
    /**
     * Handle complete logout
     */
    suspend fun logout() {
        try {
            // Clear all authentication data
            authRepository.logout()
            
            // Emit logout event
            _navigationEvents.emit(NavigationEvent.NavigateToLogin)
        } catch (e: Exception) {
            // Force logout even on error
            tokenManager.logout()
            _navigationEvents.emit(NavigationEvent.NavigateToLogin)
        }
    }
    
    /**
     * Handle unauthorized access (401 responses)
     * PERMANENT LOGIN: Only handle if tokens are completely missing
     */
    private suspend fun handleUnauthorizedAccess() {
        try {
            println("AuthManager: Handling unauthorized access")
            
            // Check if any tokens exist (don't validate - TokenAuthenticator handles refresh)
            val hasStoredTokens = hasStoredTokens()
            
            if (hasStoredTokens) {
                println("AuthManager: Tokens exist, TokenAuthenticator will handle refresh - no logout needed")
                // Tokens exist - let TokenAuthenticator handle refresh automatically
                // Don't logout here as it breaks permanent login
                return
            } else {
                println("AuthManager: No tokens found, need to logout")
                // Only logout if no tokens exist at all
                logout()
            }
        } catch (e: Exception) {
            println("AuthManager: Exception in handleUnauthorizedAccess: ${e.message}")
            // Check if tokens exist before logout
            val hasTokens = try { hasStoredTokens() } catch (ex: Exception) { false }
            if (!hasTokens) {
                logout()
            }
        }
    }
    
    /**
     * Attempt to refresh access token using refresh token
     * PERMANENT LOGIN: Don't logout on refresh failures - preserve session
     */
    private suspend fun attemptTokenRefresh() {
        try {
            println("AuthManager: Attempting to refresh token...")
            when (val result = authRepository.refreshToken()) {
                is com.app.str.data.model.Result.Success -> {
                    println("AuthManager: Token refresh successful, navigating to dashboard")
                    _navigationEvents.emit(NavigationEvent.NavigateToDashboard)
                }
                is com.app.str.data.model.Result.Error -> {
                    println("AuthManager: Token refresh failed: ${result.message}")
                    // PERMANENT LOGIN: Don't logout on refresh failure - tokens preserved
                    println("AuthManager: Preserving session despite refresh failure")
                }
                else -> {
                    println("AuthManager: Unexpected refresh result - preserving session")
                }
            }
        } catch (e: Exception) {
            println("AuthManager: Exception during token refresh: ${e.message}")
            // PERMANENT LOGIN: Don't logout on exceptions - preserve session
            println("AuthManager: Preserving session despite refresh exception")
        }
    }
    
    /**
     * Check if current session is valid
     */
    suspend fun isSessionValid(): Boolean {
        return try {
            val accessToken = authRepository.getValidAccessToken()
            accessToken != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if tokens are stored (regardless of validity)
     * Use this to avoid clearing tokens that can be refreshed
     */
    suspend fun hasStoredTokens(): Boolean {
        return try {
            val rawToken = tokenManager.getRawAccessToken()
            val refreshToken = tokenManager.getRefreshToken()
            rawToken != null || refreshToken != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get user ID from current token
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            val token = authRepository.getValidAccessToken() ?: tokenManager.getRawAccessToken()
            token?.let { tokenManager.getUserIdFromToken(it) }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Navigation events for UI layer
 */
sealed class NavigationEvent {
    object NavigateToLogin : NavigationEvent()
    object NavigateToDashboard : NavigationEvent()
    data class ShowError(val message: String) : NavigationEvent()
}