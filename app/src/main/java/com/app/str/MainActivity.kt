package com.app.str

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.str.utils.AuthState
import com.app.str.utils.NavigationEvent
import com.app.str.utils.applyTopPadding
import com.app.str.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Bas ek line - extension function magic! ✨
        findViewById<android.view.View>(R.id.main).applyTopPadding()
        
        // Handle error message from splash if any
        handleSplashErrorMessage()
        
        setupAuthObservers()
    }
    
    private fun setupAuthObservers() {
        // Observe navigation events from AuthManager
        lifecycleScope.launch {
            authViewModel.navigationEvents.collect { event ->
                handleNavigationEvent(event)
            }
        }
        
        // Observe auth state changes
        lifecycleScope.launch {
            authViewModel.authState.collect { authState ->
                handleAuthStateChange(authState)
            }
        }
    }
    
    private fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToDashboard -> {
                // Only navigate if not already handled by fragments
                println("MainActivity: Dashboard navigation event received")
                // Don't handle here - let fragments handle it
            }
            is NavigationEvent.NavigateToLogin -> {
                // Already on login screen (MainActivity), no action needed
                println("MainActivity: Login navigation event received")
            }
            is NavigationEvent.ShowError -> {
                // Show error message to user
                println("MainActivity: Error event: ${event.message}")
                // You can implement a snackbar or dialog here
            }
        }
    }
    
    private fun handleAuthStateChange(authState: AuthState) {
        when (authState) {
            AuthState.AUTHENTICATED -> {
                // User is authenticated, navigation will be handled by NavigationEvent
            }
            AuthState.TOKEN_EXPIRED -> {
                // Token expired, AuthManager will handle refresh or logout
            }
            AuthState.LOGGED_OUT -> {
                // User is logged out, ensure we're on login screen
            }
            AuthState.ERROR -> {
                // Handle authentication error
            }
            AuthState.UNKNOWN -> {
                // Initial state, AuthManager is checking authentication
            }
        }
    }
    
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun handleSplashErrorMessage() {
        intent.getStringExtra("error_message")?.let { errorMessage ->
            // Show error message to user
            // You can show a Snackbar, Toast, or Dialog
            println("MainActivity: Error from splash: $errorMessage")
            // For now, just log it. You can implement UI feedback later
        }
    }
}