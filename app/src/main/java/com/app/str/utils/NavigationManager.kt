package com.app.str.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.app.str.DashboardActivity
import com.app.str.MainActivity
import com.app.str.SplashActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() {
    
    /**
     * Navigate to splash screen (app entry point)
     */
    fun navigateToSplash(context: Context) {
        val intent = Intent(context, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }
    
    /**
     * Navigate to login screen
     */
    fun navigateToLogin(context: Context, errorMessage: String? = null, clearStack: Boolean = true) {
        val intent = Intent(context, MainActivity::class.java)
        
        if (clearStack) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        errorMessage?.let {
            intent.putExtra("error_message", it)
        }
        
        context.startActivity(intent)
        if (context is Activity && clearStack) {
            context.finish()
        }
    }
    
    /**
     * Navigate to dashboard
     */
    fun navigateToDashboard(context: Context, clearStack: Boolean = true) {
        val intent = Intent(context, DashboardActivity::class.java)
        
        if (clearStack) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        context.startActivity(intent)
        if (context is Activity && clearStack) {
            context.finish()
        }
    }
    
    /**
     * Navigate with custom transition
     */
    fun navigateWithTransition(
        context: Context, 
        targetClass: Class<*>, 
        clearStack: Boolean = true,
        enterAnim: Int = android.R.anim.fade_in,
        exitAnim: Int = android.R.anim.fade_out
    ) {
        val intent = Intent(context, targetClass)
        
        if (clearStack) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        context.startActivity(intent)
        
        if (context is Activity) {
            context.overridePendingTransition(enterAnim, exitAnim)
            if (clearStack) {
                context.finish()
            }
        }
    }
}