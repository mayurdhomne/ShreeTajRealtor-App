package com.app.str.ui.base

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.app.str.MainActivity
import com.app.str.utils.AuthManager
import com.app.str.utils.NavigationEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light mode - disable dark theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setupGlobalAuthObserver()
    }
    
    private fun setupGlobalAuthObserver() {
        // Observe navigation events globally
        lifecycleScope.launch {
            authManager.navigationEvents.collect { event ->
                handleGlobalNavigationEvent(event)
            }
        }
    }
    
    private fun handleGlobalNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToLogin -> {
                navigateToLogin()
            }
            is NavigationEvent.NavigateToDashboard -> {
                // Let individual activities handle dashboard navigation
            }
            is NavigationEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun navigateToLogin() {
        // Only navigate to login if we're not already on the login screen
        if (this::class != MainActivity::class) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    /**
     * Call this method when user explicitly logs out
     */
    protected fun performLogout() {
        lifecycleScope.launch {
            authManager.logout()
        }
    }
}