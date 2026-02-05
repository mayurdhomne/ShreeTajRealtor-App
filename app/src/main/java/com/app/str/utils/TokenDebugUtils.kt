package com.app.str.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenDebugUtils {
    
    fun checkStoredTokens(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val prefs = EncryptedSharedPreferences.create(
                context,
                "secure_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val accessToken = prefs.getString("access_token", null)
            val refreshToken = prefs.getString("refresh_token", null)
            
            println("TokenDebugUtils: Access token exists: ${accessToken != null}")
            println("TokenDebugUtils: Refresh token exists: ${refreshToken != null}")
            
            if (accessToken != null) {
                println("TokenDebugUtils: Access token preview: ${accessToken.take(20)}...")
            }
            
        } catch (e: Exception) {
            println("TokenDebugUtils: Error checking tokens: ${e.message}")
        }
    }
}