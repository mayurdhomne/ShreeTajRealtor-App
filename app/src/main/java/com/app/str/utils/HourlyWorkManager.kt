package com.app.str.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.str.worker.ReportCheckWorker
import java.util.concurrent.TimeUnit

object HourlyWorkManager {
    
    private const val TAG = "HourlyWorkManager"
    
    /**
     * Start the hourly report check worker
     * Runs every 15 minutes to:
     * 1. Check if user needs to submit hourly report
     * 2. Check if auto-submit should be triggered (if 1 hour passed since reminder)
     */
    fun startHourlyReportCheck(context: Context) {
        Log.d(TAG, "Starting hourly report check worker")
        
        // Cancel any existing work first to ensure clean state
        WorkManager.getInstance(context).cancelUniqueWork(ReportCheckWorker.WORK_NAME)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create periodic work with 15 minute interval for reliable auto-submit
        // WorkManager minimum is 15 minutes, which is perfect for our use case
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReportCheckWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            // No initial delay - start checking immediately
            .build()
        
        // Also create immediate one-time work for instant check
        val immediateWorkRequest = OneTimeWorkRequestBuilder<ReportCheckWorker>()
            .setConstraints(constraints)
            .build()
        
        // Enqueue both: immediate + periodic
        WorkManager.getInstance(context).apply {
            // Run immediately
            enqueue(immediateWorkRequest)
            
            // Schedule periodic
            enqueueUniquePeriodicWork(
                ReportCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
        }
        
        Log.d(TAG, "Immediate and periodic hourly report check workers scheduled successfully")
        Log.d(TAG, "Periodic worker will run every 15 minutes")
        Log.d(TAG, "Auto-submit will trigger if report not submitted within 1 hour")
        
        // Show worker status for debugging
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            getWorkerStatus(context)
        }, 2000) // Check status after 2 seconds
    }
    
    /**
     * Stop the hourly report check worker
     * Called when user checks out or after 7:00 PM
     */
    fun stopHourlyReportCheck(context: Context) {
        Log.d(TAG, "Stopping hourly report check worker")
        
        WorkManager.getInstance(context)
            .cancelUniqueWork(ReportCheckWorker.WORK_NAME)
        
        Log.d(TAG, "Hourly report check worker cancelled")
    }
    
    /**
     * Check if the worker is currently scheduled
     */
    fun isWorkerScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ReportCheckWorker.WORK_NAME)
            .get()
        
        return workInfos.any { !it.state.isFinished }
    }
    
    /**
     * Get detailed worker status for debugging
     */
    fun getWorkerStatus(context: Context) {
        try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReportCheckWorker.WORK_NAME)
                .get()
            
            Log.d(TAG, "=== WORKER STATUS DEBUG ===")
            Log.d(TAG, "Total workers found: ${workInfos.size}")
            
            workInfos.forEachIndexed { index, workInfo ->
                Log.d(TAG, "Worker $index:")
                Log.d(TAG, "  ID: ${workInfo.id}")
                Log.d(TAG, "  State: ${workInfo.state}")
                Log.d(TAG, "  Tags: ${workInfo.tags}")
                Log.d(TAG, "  Run attempt: ${workInfo.runAttemptCount}")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d(TAG, "  Next schedule time: ${workInfo.nextScheduleTimeMillis}")
                    if (workInfo.nextScheduleTimeMillis > 0) {
                        val nextRun = (workInfo.nextScheduleTimeMillis - System.currentTimeMillis()) / 1000 / 60
                        Log.d(TAG, "  Next run in: ~$nextRun minutes")
                    }
                }
            }
            Log.d(TAG, "=== END WORKER STATUS ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting worker status", e)
        }
    }
    

    
    /**
     * Clear all existing work and restart fresh - useful when previous workers are in FAILED state
     */
    fun clearAndRestartHourlyWork(context: Context) {
        Log.d(TAG, "=== Clearing all existing work and restarting ===")
        
        // Cancel all existing work
        WorkManager.getInstance(context).cancelAllWork()
        
        // Wait a moment for cancellation to complete
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Start fresh
            startHourlyReportCheck(context)
            Log.d(TAG, "=== Fresh hourly work started ===")
        }, 1000)
    }
    
    /**
     * DEBUG: Run worker immediately for testing auto-submit
     * This enqueues a one-time work request that runs immediately
     */
    fun debugRunWorkerNow(context: Context) {
        Log.d(TAG, "=== DEBUG: Running worker immediately ===")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateWorkRequest = OneTimeWorkRequestBuilder<ReportCheckWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        
        Log.d(TAG, "=== DEBUG: One-time worker enqueued ===")
    }
}
