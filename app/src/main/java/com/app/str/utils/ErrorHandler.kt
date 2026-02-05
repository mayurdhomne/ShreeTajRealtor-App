package com.app.str.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Utility class for handling errors in a consistent manner across the app
 */
object ErrorHandler {
    
    private val errorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Create a CoroutineExceptionHandler for consistent error handling
     */
    fun createHandler(
        context: Context? = null,
        onError: ((String) -> Unit)? = null
    ) = CoroutineExceptionHandler { _, exception ->
        val errorMessage = getErrorMessage(exception)
        
        errorScope.launch {
            context?.let {
                Toast.makeText(it, errorMessage, Toast.LENGTH_LONG).show()
            }
            onError?.invoke(errorMessage)
        }
    }
    
    /**
     * Get user-friendly error message based on exception type
     */
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is ConnectException -> "Unable to connect to server. Please try again later."
            is SocketTimeoutException -> "Connection timeout. Please try again."
            is SSLException -> "Secure connection failed. Please try again."
            is SecurityException -> "Authentication failed. Please login again."
            else -> throwable.message ?: "An unexpected error occurred"
        }
    }
    
    /**
     * Handle network errors specifically
     */
    fun handleNetworkError(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "No internet connection"
            is ConnectException -> "Server connection failed"
            is SocketTimeoutException -> "Request timeout"
            is retrofit2.HttpException -> {
                when (error.code()) {
                    401 -> "Authentication failed"
                    403 -> "Access forbidden"
                    404 -> "Resource not found"
                    500 -> "Server error"
                    else -> "Network error (${error.code()})"
                }
            }
            else -> "Network error occurred"
        }
    }
    
    /**
     * Log error messages for debugging
     */
    fun logError(tag: String, message: String, exception: Exception?) {
        val logMessage = "[$tag] $message"
        if (exception != null) {
            println("$logMessage - ${exception.message}")
            exception.printStackTrace()
        } else {
            println(logMessage)
        }
    }
}