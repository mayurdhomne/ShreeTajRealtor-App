package com.app.str.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.Notification
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit,
    private val onMarkReadClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardNotification: MaterialCardView = itemView.findViewById(R.id.cardNotification)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnMarkRead: MaterialButton = itemView.findViewById(R.id.btnMarkRead)

        fun bind(notification: Notification) {
            tvMessage.text = notification.message
            
            // Format date
            tvDate.text = formatDate(notification.notifyDate)
            
            // Handle read/unread state
            if (notification.isRead) {
                unreadIndicator.visibility = View.GONE
                tvStatus.text = "Read"
                tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                btnMarkRead.visibility = View.GONE
                cardNotification.strokeWidth = 0
                cardNotification.alpha = 0.8f
            } else {
                unreadIndicator.visibility = View.VISIBLE
                tvStatus.text = "Unread"
                tvStatus.setTextColor(itemView.context.getColor(R.color.primary_burgundy))
                btnMarkRead.visibility = View.VISIBLE
                cardNotification.strokeWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                cardNotification.strokeColor = itemView.context.getColor(R.color.primary_light)
                cardNotification.alpha = 1.0f
            }
            
            // Click listeners
            cardNotification.setOnClickListener {
                onItemClick(notification)
            }
            
            btnMarkRead.setOnClickListener {
                onMarkReadClick(notification)
            }
        }
        
        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
