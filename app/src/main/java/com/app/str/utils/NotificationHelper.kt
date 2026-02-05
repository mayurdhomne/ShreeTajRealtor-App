package com.app.str.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.str.DailyReportActivity
import com.app.str.R

object NotificationHelper {
    
    private const val CHANNEL_ID = "hourly_report_channel"
    private const val CHANNEL_NAME = "Hourly Report Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for hourly report submission reminders"
    private const val NOTIFICATION_ID = 1001
    private const val AUTO_SUBMIT_NOTIFICATION_ID = 1002
    
    /**
     * Creates notification channel for Android O and above
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("NotificationHelper", "Notification channel created with importance: ${channel.importance}")
        }
    }
    
    /**
     * Shows a notification for hourly report submission reminder
     */
    fun showReportReminderNotification(context: Context) {
        android.util.Log.d("NotificationHelper", "=== Starting notification creation ===")
        
        // Ensure notification channel exists
        createNotificationChannel(context)
        
        // Create an intent to open DailyReportActivity when notification is tapped
        val intent = Intent(context, DailyReportActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification with enhanced visibility
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Hourly Report Reminder")
            .setContentText("You need to submit your hourly report.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You need to submit your hourly report. Tap here to submit your report now.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
        
        // Show the notification
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Check if notifications are enabled
            if (!notificationManager.areNotificationsEnabled()) {
                return
            }
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted (Android 13+)
            android.util.Log.e("NotificationHelper", "Notification permission not granted", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error showing notification", e)
        }
    }
    
    /**
     * Shows a notification when report is auto-submitted
     */
    fun showAutoSubmitNotification(context: Context) {
        android.util.Log.d("NotificationHelper", "=== Showing auto-submit notification ===")
        
        // Ensure notification channel exists
        createNotificationChannel(context)
        
        // Create an intent to open DailyReportActivity when notification is tapped
        val intent = Intent(context, DailyReportActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Report Auto-Submitted")
            .setContentText("Your hourly report was automatically submitted.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your hourly report was automatically submitted as 'No work done' because it wasn't submitted within 1 hour. You can edit it from the app.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
        
        // Show the notification
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Check if notifications are enabled
            if (!notificationManager.areNotificationsEnabled()) {
                return
            }
            
            notificationManager.notify(AUTO_SUBMIT_NOTIFICATION_ID, notification)
            
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Notification permission not granted", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error showing auto-submit notification", e)
        }
    }
}
