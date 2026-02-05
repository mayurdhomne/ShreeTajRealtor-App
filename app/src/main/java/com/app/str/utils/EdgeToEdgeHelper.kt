package com.app.str.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Helper class for handling edge-to-edge display with BottomNavigationView.
 * 
 * This ensures content is never hidden behind the BottomNavigationView on all devices,
 * including those with gesture navigation where the system navigation bar height varies.
 * 
 * Usage:
 * ```kotlin
 * EdgeToEdgeHelper.setupEdgeToEdge(
 *     rootView = binding.main,
 *     bottomNav = binding.bottomNavigation,
 *     contentView = binding.nestedScrollView, // or RecyclerView, etc.
 *     appBarLayout = binding.appBarLayout // optional
 * )
 * ```
 */
object EdgeToEdgeHelper {
    
    /**
     * Sets up edge-to-edge display with proper WindowInsets handling.
     * 
     * @param rootView The root CoordinatorLayout or ConstraintLayout
     * @param bottomNav The BottomNavigationView
     * @param contentView The scrollable content view (NestedScrollView, RecyclerView, etc.)
     * @param appBarLayout Optional AppBarLayout for status bar insets
     * @param additionalBottomPadding Extra padding to add below content (default 16dp in pixels)
     */
    fun setupEdgeToEdge(
        rootView: View,
        bottomNav: BottomNavigationView,
        contentView: View? = null,
        appBarLayout: View? = null,
        additionalBottomPadding: Int = 0
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply status bar insets to AppBarLayout if provided, otherwise to root
            if (appBarLayout != null) {
                appBarLayout.updatePadding(
                    left = appBarLayout.paddingLeft,
                    top = systemBarsInsets.top,
                    right = appBarLayout.paddingRight,
                    bottom = appBarLayout.paddingBottom
                )
                // Apply only side insets to root
                view.updatePadding(
                    left = systemBarsInsets.left,
                    top = 0,
                    right = systemBarsInsets.right,
                    bottom = 0
                )
            } else {
                // Apply top and side insets to root
                view.updatePadding(
                    left = systemBarsInsets.left,
                    top = systemBarsInsets.top,
                    right = systemBarsInsets.right,
                    bottom = 0
                )
            }
            
            // Apply bottom insets to BottomNavigationView
            bottomNav.updatePadding(
                left = bottomNav.paddingLeft,
                top = bottomNav.paddingTop,
                right = bottomNav.paddingRight,
                bottom = systemBarsInsets.bottom
            )
            
            // Update content view padding to account for BottomNavigationView
            contentView?.let { content ->
                // Post to ensure BottomNavigationView has been measured
                bottomNav.post {
                    val totalBottomPadding = bottomNav.height + additionalBottomPadding
                    content.updatePadding(
                        left = content.paddingLeft,
                        top = content.paddingTop,
                        right = content.paddingRight,
                        bottom = totalBottomPadding
                    )
                    
                    // Ensure clipToPadding is false for scrollable views
                    if (content is ViewGroup) {
                        content.clipToPadding = false
                    }
                }
            }
            
            windowInsets
        }
        
        // Request insets to be applied immediately
        ViewCompat.requestApplyInsets(rootView)
    }
    
    /**
     * Sets up edge-to-edge for activities without a scrollable content view.
     * Uses margin on content container instead of padding.
     * 
     * @param rootView The root CoordinatorLayout or ConstraintLayout
     * @param bottomNav The BottomNavigationView
     * @param contentContainer The container that should not overlap with BottomNavigationView
     * @param appBarLayout Optional AppBarLayout for status bar insets
     */
    fun setupEdgeToEdgeWithMargin(
        rootView: View,
        bottomNav: BottomNavigationView,
        contentContainer: View,
        appBarLayout: View? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply status bar insets to AppBarLayout if provided
            if (appBarLayout != null) {
                appBarLayout.updatePadding(
                    left = appBarLayout.paddingLeft,
                    top = systemBarsInsets.top,
                    right = appBarLayout.paddingRight,
                    bottom = appBarLayout.paddingBottom
                )
                view.updatePadding(
                    left = systemBarsInsets.left,
                    top = 0,
                    right = systemBarsInsets.right,
                    bottom = 0
                )
            } else {
                view.updatePadding(
                    left = systemBarsInsets.left,
                    top = systemBarsInsets.top,
                    right = systemBarsInsets.right,
                    bottom = 0
                )
            }
            
            // Apply bottom insets to BottomNavigationView
            bottomNav.updatePadding(
                left = bottomNav.paddingLeft,
                top = bottomNav.paddingTop,
                right = bottomNav.paddingRight,
                bottom = systemBarsInsets.bottom
            )
            
            // Update content container margin
            bottomNav.post {
                contentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottomNav.height
                }
            }
            
            windowInsets
        }
        
        ViewCompat.requestApplyInsets(rootView)
    }
    
    /**
     * Sets up ONLY bottom navigation insets without touching top/status bar.
     * Use this for layouts that already have fitsSystemWindows="true" on AppBarLayout.
     * 
     * @param rootView The root view to listen for insets
     * @param bottomNav The BottomNavigationView
     * @param contentView The scrollable content view (NestedScrollView, RecyclerView, etc.)
     * @param additionalBottomPadding Extra padding to add below content
     */
    fun setupBottomNavOnly(
        rootView: View,
        bottomNav: BottomNavigationView,
        contentView: View? = null,
        additionalBottomPadding: Int = 0
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply bottom insets to BottomNavigationView only
            bottomNav.updatePadding(
                left = bottomNav.paddingLeft,
                top = bottomNav.paddingTop,
                right = bottomNav.paddingRight,
                bottom = systemBarsInsets.bottom
            )
            
            // Update content view padding to account for BottomNavigationView
            contentView?.let { content ->
                bottomNav.post {
                    val totalBottomPadding = bottomNav.height + additionalBottomPadding
                    content.updatePadding(
                        left = content.paddingLeft,
                        top = content.paddingTop,
                        right = content.paddingRight,
                        bottom = totalBottomPadding
                    )
                    
                    if (content is ViewGroup) {
                        content.clipToPadding = false
                    }
                }
            }
            
            windowInsets
        }
        
        ViewCompat.requestApplyInsets(rootView)
    }
    
    /**
     * Applies proper bottom padding to a RecyclerView to ensure last items are visible
     * above the BottomNavigationView.
     * 
     * @param recyclerView The RecyclerView to apply padding to
     * @param bottomNav The BottomNavigationView
     * @param additionalPadding Extra padding to add (optional)
     */
    fun applyRecyclerViewPadding(
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        bottomNav: BottomNavigationView,
        additionalPadding: Int = 16
    ) {
        bottomNav.post {
            recyclerView.updatePadding(
                left = recyclerView.paddingLeft,
                top = recyclerView.paddingTop,
                right = recyclerView.paddingRight,
                bottom = bottomNav.height + additionalPadding
            )
            recyclerView.clipToPadding = false
        }
    }
    
    /**
     * Sets up edge-to-edge display with a bottom action bar positioned above BottomNavigationView.
     * This is useful for screens that have a fixed button/action bar above the bottom navigation.
     * 
     * @param rootView The root CoordinatorLayout or ConstraintLayout
     * @param bottomNav The BottomNavigationView
     * @param bottomActionBar The fixed action bar that should appear above the BottomNavigationView
     * @param contentView The scrollable content view (NestedScrollView, RecyclerView, etc.)
     * @param additionalBottomPadding Extra padding to add below content (accounts for action bar)
     */
    fun setupEdgeToEdgeWithActionBar(
        rootView: View,
        bottomNav: BottomNavigationView,
        bottomActionBar: View,
        contentView: View? = null,
        additionalBottomPadding: Int = 0
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top and side insets to root
            view.updatePadding(
                left = systemBarsInsets.left,
                top = systemBarsInsets.top,
                right = systemBarsInsets.right,
                bottom = 0
            )
            
            // Apply bottom insets to BottomNavigationView
            bottomNav.updatePadding(
                left = bottomNav.paddingLeft,
                top = bottomNav.paddingTop,
                right = bottomNav.paddingRight,
                bottom = systemBarsInsets.bottom
            )
            
            // Position bottom action bar above BottomNavigationView
            bottomNav.post {
                val bottomNavHeight = bottomNav.height
                bottomActionBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottomNavHeight
                }
                
                // Update content view padding to account for both action bar and bottom nav
                contentView?.let { content ->
                    val actionBarHeight = bottomActionBar.height
                    val totalBottomPadding = bottomNavHeight + actionBarHeight + additionalBottomPadding
                    content.updatePadding(
                        left = content.paddingLeft,
                        top = content.paddingTop,
                        right = content.paddingRight,
                        bottom = totalBottomPadding
                    )
                    
                    if (content is ViewGroup) {
                        content.clipToPadding = false
                    }
                }
            }
            
            windowInsets
        }
        
        ViewCompat.requestApplyInsets(rootView)
    }
}
