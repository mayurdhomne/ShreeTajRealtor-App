package com.app.str.data.repository

import com.app.str.data.api.NotificationApiService
import com.app.str.data.model.MarkReadResponse
import com.app.str.data.model.Notification
import com.app.str.data.model.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling notification data operations
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApiService: NotificationApiService
) {
    
    /**
     * Get notifications with optional filters
     */
    suspend fun getNotifications(
        notifyDate: String? = null,
        isRead: Boolean? = null
    ): Result<List<Notification>> {
        return try {
            val response = notificationApiService.getNotifications(notifyDate, isRead)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to fetch notifications")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
    
    /**
     * Get today's unread notifications
     */
    suspend fun getTodayUnreadNotifications(today: String): Result<List<Notification>> {
        return getNotifications(notifyDate = today, isRead = false)
    }
    
    /**
     * Get single notification by ID
     */
    suspend fun getNotification(id: Int): Result<Notification> {
        return try {
            val response = notificationApiService.getNotification(id)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to fetch notification")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
    
    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(id: Int): Result<MarkReadResponse> {
        return try {
            val response = notificationApiService.markAsRead(id)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to mark as read")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(): Result<MarkReadResponse> {
        return try {
            val response = notificationApiService.markAllAsRead()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to mark all as read")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = notificationApiService.getUnreadCount()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.unreadCount)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get unread count")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
}
