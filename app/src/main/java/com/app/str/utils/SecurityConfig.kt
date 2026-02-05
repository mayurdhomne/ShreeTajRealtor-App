package com.app.str.utils

object SecurityConfig {
    
    const val TOKEN_EXPIRY_BUFFER = 30_000L
    const val TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000L
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCKOUT_DURATION = 15 * 60 * 1000L
    
    val SECURE_ENDPOINTS = setOf(
        "profile/complete/",
        "attendance/",
        "hourly-reports/",
        "workplans/"
    )
    
    val PUBLIC_ENDPOINTS = setOf(
        "login/",
        "signup/",
        "verify-otp/",
        "resend-otp/"
    )
    
    fun clearMemory(vararg data: String?) {
        data.forEach { 
            it?.let { str ->
                val chars = str.toCharArray()
                chars.fill('\u0000')
            }
        }
    }
}