package com.app.str

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.str.databinding.ActivitySplashBinding
import com.app.str.utils.AuthState
import com.app.str.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels()
    
    @Inject
    lateinit var authManager: com.app.str.utils.AuthManager
    
    private var isNavigationHandled = false
    private val minSplashDuration = 2000L // Minimum 2 seconds
    private val maxSplashDuration = 5000L // Maximum 5 seconds timeout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar for full screen splash
        supportActionBar?.hide()
        
        setupUI()
        startAuthenticationCheck()
    }
    
    private fun setupUI() {
        // Set app logo and branding
        binding.apply {
            // Set text content
            tvAppName.text = "STR"
            tvTagline.text = "Smart Tracking & Reporting"
            
            // Start with invisible elements for animation
            logoContainer.alpha = 0f
            logoCard.alpha = 0f
            circle1.alpha = 0f
            circle2.alpha = 0f
            
            // Animate decorative circles with stagger effect
            circle1.animate()
                .alpha(0.15f)
                .setDuration(1500)
                .setStartDelay(0)
                .start()
                
            circle2.animate()
                .alpha(0.1f)
                .setDuration(1500)
                .setStartDelay(200)
                .start()
            
            // Animate logo card with simple fade (no bounce, no rotation)
            logoCard.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(300)
                .start()
            
            // Fade in main container
            logoContainer.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .start()
            
            // Animate app name with slide up effect
            tvAppName.translationY = 50f
            tvAppName.alpha = 0f
            tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(700)
                .start()
            
            // Animate tagline with slide up effect
            tvTagline.translationY = 30f
            tvTagline.alpha = 0f
            tvTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(900)
                .start()
        }
    }
    
    private fun startAuthenticationCheck() {
        println("SplashActivity: Starting enhanced authentication check with silent login support...")
        
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            
            // Setup timeout protection
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!isNavigationHandled) {
                    println("SplashActivity: Timeout reached, checking for stored credentials...")
                    lifecycleScope.launch {
                        checkStoredCredentialsAndNavigate()
                    }
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, maxSplashDuration)
            
            try {
                // Enhanced authentication check with silent login support
                performEnhancedAuthCheck(startTime, timeoutHandler, timeoutRunnable)
                
            } catch (e: Exception) {
                println("SplashActivity: Exception during auth check: ${e.message}")
                timeoutHandler.removeCallbacks(timeoutRunnable)
                checkStoredCredentialsAndNavigate()
            }
        }
    }
    
    private suspend fun performEnhancedAuthCheck(startTime: Long, timeoutHandler: Handler, timeoutRunnable: Runnable) {
        // Initialize authentication state
        authViewModel.checkAuthenticationStatus()
        
        // Observe authentication state with enhanced logic
        authViewModel.authState.collect { authState ->
            println("SplashActivity: Auth state received: $authState")
            
            // Calculate remaining splash time
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = (minSplashDuration - elapsedTime).coerceAtLeast(0L)
            
            when (authState) {
                AuthState.AUTHENTICATED -> {
                    println("SplashActivity: User authenticated with valid tokens")
                    updateSplashMessage("Welcome back!")
                    delay(remainingTime)
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    navigateToDashboard()
                }
                
                AuthState.TOKEN_EXPIRED -> {
                    println("SplashActivity: Token expired, but TokenAuthenticator will handle silent login")
                    updateSplashMessage("Refreshing session automatically...")
                    
                    // With silent login, expired tokens will be automatically refreshed
                    // If silent login succeeds, user will be navigated to dashboard
                    // If it fails, they'll see login screen
                    delay(remainingTime + 1000) // Give extra time for silent login
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    
                    // Check if silent login worked by checking session validity
                    val hasValidSession = authManager.isSessionValid()
                    if (hasValidSession) {
                        println("SplashActivity: Silent login successful, navigating to dashboard")
                        navigateToDashboard()
                    } else {
                        println("SplashActivity: Silent login failed, checking stored credentials")
                        checkStoredCredentialsAndNavigate()
                    }
                }
                
                AuthState.LOGGED_OUT -> {
                    println("SplashActivity: User logged out, checking for stored credentials...")
                    updateSplashMessage("Checking stored credentials...")
                    delay(remainingTime)
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    checkStoredCredentialsAndNavigate()
                }
                
                AuthState.ERROR -> {
                    println("SplashActivity: Authentication error, checking for stored credentials...")
                    updateSplashMessage("Checking stored credentials...")
                    delay(remainingTime)
                    timeoutHandler.removeCallbacks(timeoutRunnable) 
                    checkStoredCredentialsAndNavigate()
                }
                
                AuthState.UNKNOWN -> {
                    println("SplashActivity: Authentication state unknown, checking...")
                    updateSplashMessage("Checking authentication...")
                    // Continue waiting for a definitive state
                }
            }
        }
    }
    
    private fun updateSplashMessage(message: String) {
        runOnUiThread {
            binding.tvStatus.text = message
        }
    }
    
    /**
     * Check if stored credentials exist and navigate accordingly
     * This is the enhanced logic for silent login support
     */
    private suspend fun checkStoredCredentialsAndNavigate() {
        try {
            println("SplashActivity: Checking for stored tokens/credentials...")
            updateSplashMessage("Checking saved login...")
            
            // Check if any authentication data exists (tokens or credentials)
            val hasStoredTokens = authManager.hasStoredTokens()
            
            if (hasStoredTokens) {
                println("SplashActivity: Found stored authentication data")
                updateSplashMessage("Welcome back!")
                delay(500) // Brief delay for smooth UX
                navigateToDashboard()
            } else {
                println("SplashActivity: No stored authentication data, navigating to login")
                updateSplashMessage("Please login to continue")
                delay(500)
                navigateToLogin()
            }
            
        } catch (e: Exception) {
            println("SplashActivity: Error checking stored authentication data: ${e.message}")
            updateSplashMessage("Please login to continue")
            delay(500)
            navigateToLogin()
        }
    }
    
    private fun navigateToDashboard() {
        if (isNavigationHandled) return
        isNavigationHandled = true
        
        println("SplashActivity: Navigating to DashboardActivity")
        
        lifecycleScope.launch {
            try {
                // Enhanced exit animation with multiple elements
                binding.apply {
                    // Fade out decorative elements first
                    circle1.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .start()
                    
                    circle2.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .start()
                    
                    // Fade out logo card (no scale)
                    logoCard.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .start()
                    
                    // Slide and fade text elements
                    tvAppName.animate()
                        .alpha(0f)
                        .translationY(-30f)
                        .setDuration(300)
                        .start()
                    
                    tvTagline.animate()
                        .alpha(0f)
                        .translationY(-20f)
                        .setDuration(300)
                        .start()
                }
                
                // Wait for animations to complete
                delay(400)
                
                // Check profile completion status before navigating
                val isProfileCompleted = authViewModel.checkProfileStatus()
                
                // Fade out main container
                binding.logoContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        if (isProfileCompleted) {
                            // Profile completed - navigate to dashboard
                            val intent = Intent(this@SplashActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            // Profile not completed - navigate to profile complete
                            val intent = Intent(this@SplashActivity, ProfileCompleteActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        finish()
                        
                        // Add custom transition
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                    .start()
            } catch (e: Exception) {
                println("SplashActivity: Error navigating: ${e.message}")
                // Fallback navigation without animation - check profile status
                try {
                    val isProfileCompleted = runBlocking { authViewModel.checkProfileStatus() }
                    if (isProfileCompleted) {
                        val intent = Intent(this@SplashActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@SplashActivity, ProfileCompleteActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                } catch (profileCheckError: Exception) {
                    // If profile check fails, default to dashboard
                    val intent = Intent(this@SplashActivity, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                finish()
            }
        }
    }
    
    private fun navigateToLogin(message: String? = null) {
        if (isNavigationHandled) return
        isNavigationHandled = true
        
        println("SplashActivity: Navigating to MainActivity (Login)")
        
        lifecycleScope.launch {
            try {
                // Enhanced exit animation with multiple elements
                binding.apply {
                    // Fade out decorative elements first
                    circle1.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .start()
                    
                    circle2.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .start()
                    
                    // Fade out logo card (no scale)
                    logoCard.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .start()
                    
                    // Slide and fade text elements
                    tvAppName.animate()
                        .alpha(0f)
                        .translationY(-30f)
                        .setDuration(300)
                        .start()
                    
                    tvTagline.animate()
                        .alpha(0f)
                        .translationY(-20f)
                        .setDuration(300)
                        .start()
                }
                
                // Wait for animations to complete
                delay(400)
                
                // Fade out main container
                binding.logoContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        
                        // Pass error message if any
                        message?.let {
                            intent.putExtra("error_message", it)
                        }
                        
                        startActivity(intent)
                        finish()
                        
                        // Add custom transition
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                    .start()
            } catch (e: Exception) {
                println("SplashActivity: Error navigating to login: ${e.message}")
                // Fallback navigation without animation
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent back press during splash
        // Do nothing or show exit confirmation
    }
    
    override fun onDestroy() {
        super.onDestroy()
        println("SplashActivity: Activity destroyed")
    }
}