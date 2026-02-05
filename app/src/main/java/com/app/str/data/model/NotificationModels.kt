package com.app.str.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for Notification API
 */

// Response for list of notifications
data class NotificationListResponse(
    @SerializedName("count") val count: Int? = null,
    @SerializedName("next") val next: String? = null,
    @SerializedName("previous") val previous: String? = null,
    @SerializedName("results") val results: List<Notification>? = null
)

// Single notification model
data class Notification(
    @SerializedName("id") val id: Int,
    @SerializedName("user") val user: Int,
    @SerializedName("message") val message: String,
    @SerializedName("notify_date") val notifyDate: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String? = null
)

// Request to create a notification
data class CreateNotificationRequest(
    @SerializedName("user") val user: Int? = null, // Only for superuser
    @SerializedName("message") val message: String,
    @SerializedName("notify_date") val notifyDate: String
)

// Response for mark read operations
data class MarkReadResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("updated_count") val updatedCount: Int? = null
)

// Response for unread count
data class UnreadCountResponse(
    @SerializedName("unread_count") val unreadCount: Int
)
