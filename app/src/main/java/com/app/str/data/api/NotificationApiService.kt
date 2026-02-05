package com.app.str.data.api

import com.app.str.data.model.CreateNotificationRequest
import com.app.str.data.model.MarkReadResponse
import com.app.str.data.model.Notification
import com.app.str.data.model.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API service for notification endpoints
 * Based on Postman collection: Notifications API - str_project
 */
interface NotificationApiService {
    
    /**
     * Get list of notifications with optional filters
     * @param notifyDate Filter by notification date (YYYY-MM-DD)
     * @param isRead Filter by read status (true/false)
     */
    @GET("notifications/")
    suspend fun getNotifications(
        @Query("notify_date") notifyDate: String? = null,
        @Query("is_read") isRead: Boolean? = null
    ): Response<List<Notification>>
    
    /**
     * Get a single notification by ID
     */
    @GET("notifications/{id}/")
    suspend fun getNotification(@Path("id") id: Int): Response<Notification>
    
    /**
     * Create a new notification
     * Regular users create for themselves, superusers can specify user
     */
    @POST("notifications/")
    suspend fun createNotification(@Body request: CreateNotificationRequest): Response<Notification>
    
    /**
     * Mark a single notification as read
     */
    @POST("notifications/{id}/mark_read/")
    suspend fun markAsRead(@Path("id") id: Int): Response<MarkReadResponse>
    
    /**
     * Mark all notifications as read for the current user
     */
    @POST("notifications/mark_all_read/")
    suspend fun markAllAsRead(): Response<MarkReadResponse>
    
    /**
     * Get notifications for a specific user (superuser or self)
     */
    @GET("notifications/user/{user_id}/")
    suspend fun getNotificationsForUser(@Path("user_id") userId: Int): Response<List<Notification>>
    
    /**
     * Get unread notification count
     */
    @GET("notifications/unread_count/")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>
}
