package com.app.str.utils

import android.app.Activity
import android.content.Intent
import com.app.str.DashboardActivity
import com.app.str.IncentiveActivity
import com.app.str.ProfileActivity
import com.app.str.R
import com.app.str.WorkPlansActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Helper class for managing bottom navigation across activities
 * Provides consistent navigation behavior and smooth transitions
 */
object BottomNavigationHelper {

    /**
     * Setup bottom navigation with proper selection and listeners
     * @param activity The activity containing the bottom navigation
     * @param bottomNavigation The BottomNavigationView instance
     * @param currentItemId The menu item ID that should be selected
     */
    fun setup(
        activity: Activity,
        bottomNavigation: BottomNavigationView,
        currentItemId: Int
    ) {
        bottomNavigation.selectedItemId = currentItemId
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) {
                        navigateToActivity(activity, DashboardActivity::class.java)
                    }
                    true
                }
                R.id.nav_work_plan -> {
                    if (currentItemId != R.id.nav_work_plan) {
                        navigateToActivity(activity, WorkPlansActivity::class.java)
                    }
                    true
                }
                R.id.nav_incentive -> {
                    if (currentItemId != R.id.nav_incentive) {
                        navigateToActivity(activity, IncentiveActivity::class.java)
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (currentItemId != R.id.nav_profile) {
                        navigateToActivity(activity, ProfileActivity::class.java)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Navigate to an activity with smooth transition and proper flags
     * @param fromActivity The current activity
     * @param toActivityClass The target activity class
     */
    fun navigateToActivity(fromActivity: Activity, toActivityClass: Class<*>) {
        val intent = Intent(fromActivity, toActivityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        fromActivity.startActivity(intent)
        fromActivity.overridePendingTransition(0, 0)
    }

    /**
     * Navigate to Dashboard and finish current activity
     * Useful for back navigation from child activities
     */
    fun navigateToDashboardAndFinish(activity: Activity) {
        val intent = Intent(activity, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
        activity.finish()
    }
}
