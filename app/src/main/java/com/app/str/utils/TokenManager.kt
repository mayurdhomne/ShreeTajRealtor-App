package com.app.str.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.auth0.android.jwt.JWT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    
    private val mutex = Mutex()
    
    // Token state flows for reactive updates
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _authState = MutableStateFlow(AuthState.UNKNOWN)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // In-memory token storage for performance
    @Volatile
    private var cachedAccessToken: String? = null
    
    @Volatile
    private var cachedRefreshToken: String? = null
    
    private val masterKey by lazy {
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedSharedPreferences by lazy {
        getOrCreateEncryptedSharedPreferences()
    }
    
    /**
     * Create EncryptedSharedPreferences with fallback for corrupted keystore
     */
    private fun getOrCreateEncryptedSharedPreferences(): android.content.SharedPreferences {
        return try {
            logTokenOperation("Creating EncryptedSharedPreferences")
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            logTokenOperation("ERROR creating EncryptedSharedPreferences: ${e.message}")
            logTokenOperation("Attempting to recover by clearing corrupted preferences...")
            
            try {
                // Clear any corrupted preference files
                val prefFile = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefFile.edit().clear().apply()
                
                // Try to create EncryptedSharedPreferences again
                EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackException: Exception) {
                logTokenOperation("ERROR: Fallback also failed: ${fallbackException.message}")
                logTokenOperation("Using regular SharedPreferences as last resort")
                
                // Last resort: use regular SharedPreferences with warning
                logTokenOperation("WARNING: Using unencrypted storage due to encryption issues")
                context.getSharedPreferences("${PREF_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }
    
    /**
     * Save tokens securely with validation
     * Enhanced for permanent login session - ensures both tokens are always saved
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) = mutex.withLock {
        try {
            logTokenOperation("Starting token save operation")
            logTokenPreview("Access token", accessToken)
            logTokenPreview("Refresh token", refreshToken)
            
            logTokenOperation("Validating JWT token formats")
            // Validate both tokens before saving
            if (!isValidJWT(accessToken)) {
                val error = "Invalid JWT format for access token"
                logTokenOperation("ERROR: $error")
                throw IllegalArgumentException(error)
            }
            
            if (!isValidJWT(refreshToken)) {
                val error = "Invalid JWT format for refresh token"
                logTokenOperation("ERROR: $error")
                throw IllegalArgumentException(error)
            }
            logTokenOperation("JWT validation passed for both tokens")
            
            logTokenOperation("Saving tokens to encrypted storage")
            try {
                encryptedSharedPreferences.edit()
                    .putString(ACCESS_TOKEN_KEY, accessToken)
                    .putString(REFRESH_TOKEN_KEY, refreshToken)
                    .putLong(TOKEN_SAVE_TIME_KEY, System.currentTimeMillis())
                    .apply()
                logTokenOperation("Encrypted storage save completed")
            } catch (encryptionException: Exception) {
                logTokenOperation("ERROR during token save to encrypted storage: ${encryptionException.message}")
                logTokenOperation("Attempting to recreate encrypted preferences...")
                
                // Force recreate the encrypted preferences and try again
                val newPrefs = getOrCreateEncryptedSharedPreferences()
                newPrefs.edit()
                    .putString(ACCESS_TOKEN_KEY, accessToken)
                    .putString(REFRESH_TOKEN_KEY, refreshToken)
                    .putLong(TOKEN_SAVE_TIME_KEY, System.currentTimeMillis())
                    .apply()
                logTokenOperation("Token save successful after preference recreation")
            }
            
            // Update cache for performance
            logTokenOperation("Updating in-memory cache")
            cachedAccessToken = accessToken
            cachedRefreshToken = refreshToken
            logTokenOperation("Cache updated successfully")
            
            // Update auth state for UI reactivity
            logTokenOperation("Updating authentication state")
            updateAuthState()
            logTokenOperation("Token save operation completed successfully")
            
        } catch (e: Exception) {
            logTokenOperation("ERROR during token save: ${e.message}")
            e.printStackTrace()
            // For permanent login: Don't clear existing tokens on save error
            // This preserves user session even if there are temporary storage issues
            throw e
        }
    }
    
    /**
     * Get access token with validation
     */
    suspend fun getAccessToken(): String? = mutex.withLock {
        try {
            if (cachedAccessToken == null) {
                try {
                    cachedAccessToken = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
                    println("TokenManager: Loaded token from storage: ${if (cachedAccessToken != null) "Token exists" else "No token"}")
                } catch (encryptionException: Exception) {
                    println("TokenManager: Encryption error loading token: ${encryptionException.message}")
                    // Return null to indicate no token available due to encryption issues
                    cachedAccessToken = null
                }
            }
            
            val isValid = cachedAccessToken != null && isTokenValid(cachedAccessToken!!)
            println("TokenManager: Token validation result: $isValid")
            
            return@withLock if (isValid) {
                cachedAccessToken
            } else {
                println("TokenManager: Token is null or invalid")
                null
            }
        } catch (e: Exception) {
            println("TokenManager: Error getting access token: ${e.message}")
            null
        }
    }
    
    /**
     * Get raw access token without validation (for use in interceptor)
     * This allows expired tokens to be sent, so the authenticator can refresh them
     */
    suspend fun getRawAccessToken(): String? = mutex.withLock {
        try {
            if (cachedAccessToken == null) {
                try {
                    cachedAccessToken = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
                } catch (encryptionException: Exception) {
                    println("TokenManager: Encryption error in getRawAccessToken: ${encryptionException.message}")
                    cachedAccessToken = null
                }
            }
            return@withLock cachedAccessToken
        } catch (e: Exception) {
            println("TokenManager: Error getting raw access token: ${e.message}")
            null
        }
    }
    
    /**
     * Get refresh token (without expiry validation since refresh tokens have long expiry)
     * Returns null only if token doesn't exist or is invalid JWT format
     */
    suspend fun getRefreshToken(): String? = mutex.withLock {
        try {
            if (cachedRefreshToken == null) {
                try {
                    cachedRefreshToken = encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
                } catch (encryptionException: Exception) {
                    cachedRefreshToken = null
                }
            }
            
            // Only check if it's a valid JWT format, don't check expiry
            return@withLock if (cachedRefreshToken != null && isValidJWT(cachedRefreshToken!!)) {
                cachedRefreshToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if user has valid authentication
     */
    suspend fun isAuthenticated(): Boolean = mutex.withLock {
        val accessToken = getAccessTokenInternal()
        val refreshToken = getRefreshTokenInternal()
        return@withLock accessToken != null || refreshToken != null
    }
    
    /**
     * Check if access token is valid and not expired
     */
    fun isTokenValid(token: String): Boolean {
        return try {
            if (!isValidJWT(token)) {
                return false
            }
            
            val jwt = JWT(token)
            val expirationDate = jwt.expiresAt
            
            if (expirationDate == null) {
                return false
            }
            
            val currentTime = System.currentTimeMillis()
            val expiryTime = expirationDate.time
            val isValid = expiryTime > currentTime + TOKEN_EXPIRY_BUFFER
            
            println("TokenManager: isTokenValid - Current time: $currentTime")
            println("TokenManager: isTokenValid - Expiry time: $expiryTime")
            println("TokenManager: isTokenValid - Time until expiry: ${(expiryTime - currentTime) / 1000} seconds")
            println("TokenManager: isTokenValid - Token valid: $isValid")
            
            isValid
        } catch (e: Exception) {
            println("TokenManager: isTokenValid - Exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if token needs refresh (expires within 5 minutes)
     */
    fun shouldRefreshToken(token: String): Boolean {
        return try {
            val jwt = JWT(token)
            val expirationDate = jwt.expiresAt
            
            expirationDate?.let {
                it.time - System.currentTimeMillis() < TOKEN_REFRESH_THRESHOLD
            } ?: true
        } catch (e: Exception) {
            true
        }
    }
    
    /**
     * Get token expiration time
     */
    fun getTokenExpirationTime(token: String): Date? {
        return try {
            JWT(token).expiresAt
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user ID from token
     */
    fun getUserIdFromToken(token: String): String? {
        return try {
            JWT(token).getClaim("user_id").asString()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clear all stored tokens and reset state
     * DEPRECATED for permanent login - only use for explicit user logout
     */
    @Deprecated("Use logout() for explicit user action. clearTokens() should not be called automatically.")
    suspend fun clearTokens() = mutex.withLock {
        logTokenOperation("WARNING: clearTokens() called - this should only happen on explicit user logout")
        clearTokensInternal()
        updateAuthState()
    }
    
    /**
     * Check if user has any stored tokens (for permanent login session)
     * This method helps determine if user had a previous session
     */
    suspend fun hasAnyStoredTokens(): Boolean = mutex.withLock {
        try {
            var accessToken: String? = null
            var refreshToken: String? = null
            
            try {
                accessToken = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
                refreshToken = encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
            } catch (encryptionException: Exception) {
                logTokenOperation("Encryption error checking tokens: ${encryptionException.message}")
                // Return false if we can't read due to encryption issues
                return@withLock false
            }
            
            val hasTokens = accessToken != null || refreshToken != null
            logTokenOperation("Token check - Access: ${accessToken != null}, Refresh: ${refreshToken != null}")
            return@withLock hasTokens
        } catch (e: Exception) {
            logTokenOperation("Error checking stored tokens: ${e.message}")
            return@withLock false
        }
    }
    
    /**
     * Complete logout - clear all auth data including stored credentials
     */
    suspend fun logout() = mutex.withLock {
        try {
            logTokenOperation("Performing complete logout - clearing all data")
            // Clear encrypted preferences (tokens + credentials)
            encryptedSharedPreferences.edit().clear().apply()
            
            // Clear memory cache
            cachedAccessToken = null
            cachedRefreshToken = null
            
            // Update state
            _isLoggedIn.value = false
            _authState.value = AuthState.LOGGED_OUT
            logTokenOperation("Complete logout successful")
        } catch (e: Exception) {
            logTokenOperation("ERROR during logout: ${e.message}")
            // Force clear even if there's an error
            cachedAccessToken = null
            cachedRefreshToken = null
            _isLoggedIn.value = false
            _authState.value = AuthState.ERROR
        }
    }
    
    /**
     * Initialize auth state on app start
     */
    suspend fun initializeAuthState() = mutex.withLock {
        updateAuthState()
    }
    
    // Private helper methods
    
    private fun clearTokensInternal() {
        encryptedSharedPreferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(TOKEN_SAVE_TIME_KEY)
            .apply()
        
        cachedAccessToken = null
        cachedRefreshToken = null
    }
    
    private suspend fun updateAuthState() {
        try {
            println("TokenManager: updateAuthState - Starting...")
            val accessToken = getAccessTokenInternal()
            println("TokenManager: updateAuthState - Got access token: ${accessToken != null}")
            val refreshToken = getRefreshTokenInternal()
            println("TokenManager: updateAuthState - Got refresh token: ${refreshToken != null}")
            
            when {
                accessToken != null -> {
                    println("TokenManager: updateAuthState - Setting AUTHENTICATED state")
                    _isLoggedIn.value = true
                    _authState.value = AuthState.AUTHENTICATED
                    println("TokenManager: updateAuthState - AUTHENTICATED state set")
                }
                refreshToken != null -> {
                    println("TokenManager: updateAuthState - Setting TOKEN_EXPIRED state")
                    _isLoggedIn.value = true
                    _authState.value = AuthState.TOKEN_EXPIRED
                    println("TokenManager: updateAuthState - TOKEN_EXPIRED state set")
                }
                else -> {
                    println("TokenManager: updateAuthState - Setting LOGGED_OUT state")
                    _isLoggedIn.value = false
                    _authState.value = AuthState.LOGGED_OUT
                    println("TokenManager: updateAuthState - LOGGED_OUT state set")
                }
            }
            println("TokenManager: updateAuthState - Completed successfully")
        } catch (e: Exception) {
            println("TokenManager: updateAuthState - Error: ${e.message}")
            e.printStackTrace()
            _isLoggedIn.value = false
            _authState.value = AuthState.ERROR
        }
    }

    /**
     * Internal method to get access token without mutex (for use within locked methods)
     */
    private fun getAccessTokenInternal(): String? {
        return try {
            if (cachedAccessToken == null) {
                try {
                    cachedAccessToken = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
                    println("TokenManager: getAccessTokenInternal - Loaded token from storage: ${if (cachedAccessToken != null) "Token exists" else "No token"}")
                } catch (encryptionException: Exception) {
                    println("TokenManager: getAccessTokenInternal - Encryption error: ${encryptionException.message}")
                    cachedAccessToken = null
                }
            }
            
            val isValid = cachedAccessToken != null && isTokenValid(cachedAccessToken!!)
            println("TokenManager: getAccessTokenInternal - Token validation result: $isValid")
            
            if (isValid) {
                cachedAccessToken
            } else {
                println("TokenManager: getAccessTokenInternal - Token is null or invalid")
                null
            }
        } catch (e: Exception) {
            println("TokenManager: getAccessTokenInternal - Error: ${e.message}")
            null
        }
    }

    /**
     * Internal method to get refresh token without mutex (for use within locked methods)
     * No expiry validation - only JWT format check
     */
    private fun getRefreshTokenInternal(): String? {
        return try {
            if (cachedRefreshToken == null) {
                try {
                    cachedRefreshToken = encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
                } catch (encryptionException: Exception) {
                    println("TokenManager: getRefreshTokenInternal - Encryption error: ${encryptionException.message}")
                    cachedRefreshToken = null
                }
            }
            
            // Only validate JWT format, not expiry
            if (cachedRefreshToken != null && isValidJWT(cachedRefreshToken!!)) {
                cachedRefreshToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isValidJWT(token: String): Boolean {
        return try {
            // Basic JWT format validation (3 parts separated by dots)
            val parts = token.split(".")
            parts.size == 3 && parts.all { it.isNotEmpty() }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Safe logging for token operations without exposing sensitive data
     */
    private fun logTokenOperation(message: String) {
        println("TokenManager: $message")
    }
    
    /**
     * Safe token preview logging - shows only first few characters for debugging
     */
    private fun logTokenPreview(tokenType: String, token: String) {
        when {
            token.length >= 20 -> logTokenOperation("$tokenType preview: ${token.take(20)}... (${token.length} chars)")
            token.length >= 10 -> logTokenOperation("$tokenType preview: ${token.take(10)}... (${token.length} chars)") 
            else -> logTokenOperation("$tokenType preview: [SHORT_TOKEN] (${token.length} chars)")
        }
    }
    
    /**
     * Save user credentials securely for automatic silent re-authentication
     * This is always enabled for seamless user experience
     */
    suspend fun saveUserCredentials(email: String, password: String) = mutex.withLock {
        try {
            logTokenOperation("Saving user credentials for automatic silent login (encrypted)")
            try {
                encryptedSharedPreferences.edit()
                    .putString(USER_EMAIL_KEY, email)
                    .putString(USER_PASSWORD_KEY, password)
                    .putBoolean(SILENT_LOGIN_ENABLED_KEY, true)
                    .apply()
            } catch (encryptionException: Exception) {
                logTokenOperation("Encryption error saving credentials: ${encryptionException.message}")
                throw encryptionException
            }
            logTokenOperation("User credentials saved successfully for automatic re-login")
        } catch (e: Exception) {
            logTokenOperation("ERROR saving user credentials: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get stored user credentials for automatic silent login
     */
    suspend fun getUserCredentials(): Pair<String, String>? = mutex.withLock {
        try {
            var email: String? = null
            var password: String? = null
            
            try {
                email = encryptedSharedPreferences.getString(USER_EMAIL_KEY, null)
                password = encryptedSharedPreferences.getString(USER_PASSWORD_KEY, null)
            } catch (encryptionException: Exception) {
                logTokenOperation("Encryption error retrieving credentials: ${encryptionException.message}")
                return@withLock null
            }
            
            return@withLock if (email != null && password != null) {
                logTokenOperation("Retrieved user credentials for automatic silent login")
                Pair(email, password)
            } else {
                logTokenOperation("No user credentials found for silent login")
                null
            }
        } catch (e: Exception) {
            logTokenOperation("ERROR retrieving user credentials: ${e.message}")
            null
        }
    }
    
    /**
     * Internal method to enable silent login (always enabled by default)
     */
    suspend fun setSilentLoginEnabled(enabled: Boolean) = mutex.withLock {
        encryptedSharedPreferences.edit()
            .putBoolean(SILENT_LOGIN_ENABLED_KEY, enabled)
            .apply()
        logTokenOperation("Silent login status set to: $enabled")
    }
    
    /**
     * Clear user credentials (but keep tokens)
     */
    suspend fun clearUserCredentials() = mutex.withLock {
        encryptedSharedPreferences.edit()
            .remove(USER_EMAIL_KEY)
            .remove(USER_PASSWORD_KEY)
            .putBoolean(SILENT_LOGIN_ENABLED_KEY, false)
            .apply()
        logTokenOperation("User credentials cleared")
    }

    companion object {
        private const val PREF_NAME = "secure_auth_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_SAVE_TIME_KEY = "token_save_time"
        
        // Silent login credentials (encrypted)
        private const val USER_EMAIL_KEY = "user_email"
        private const val USER_PASSWORD_KEY = "user_password"
        private const val SILENT_LOGIN_ENABLED_KEY = "silent_login_enabled"
        
        // Token expiry buffer (30 seconds)
        private const val TOKEN_EXPIRY_BUFFER = 30_000L
        
        // Token refresh threshold (5 minutes before expiry)
        private const val TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000L
    }
}

/**
 * Authentication states for reactive UI updates
 */
enum class AuthState {
    UNKNOWN,
    AUTHENTICATED,
    TOKEN_EXPIRED,
    LOGGED_OUT,
    ERROR
}