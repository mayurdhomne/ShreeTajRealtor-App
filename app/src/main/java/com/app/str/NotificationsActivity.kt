package com.app.str

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.str.adapter.NotificationAdapter
import com.app.str.data.model.Notification
import com.app.str.data.model.Result
import com.app.str.data.repository.NotificationRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvUnreadCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var btnMarkAllRead: MaterialButton
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipUnread: Chip
    private lateinit var chipRead: Chip
    private lateinit var chipToday: Chip
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: NotificationAdapter
    private var allNotifications: List<Notification> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL

    private enum class FilterType {
        ALL, UNREAD, READ, TODAY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupMarkAllReadButton()
        loadNotifications()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvUnreadCount = findViewById(R.id.tvUnreadCount)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        chipAll = findViewById(R.id.chipAll)
        chipUnread = findViewById(R.id.chipUnread)
        chipRead = findViewById(R.id.chipRead)
        chipToday = findViewById(R.id.chipToday)
        rvNotifications = findViewById(R.id.rvNotifications)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { notification ->
                if (!notification.isRead) {
                    markAsRead(notification)
                }
            },
            onMarkReadClick = { notification ->
                markAsRead(notification)
            }
        )

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    private fun setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            currentFilter = when (checkedIds.first()) {
                R.id.chipAll -> FilterType.ALL
                R.id.chipUnread -> FilterType.UNREAD
                R.id.chipRead -> FilterType.READ
                R.id.chipToday -> FilterType.TODAY
                else -> FilterType.ALL
            }
            applyFilter()
        }
    }

    private fun setupMarkAllReadButton() {
        btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun loadNotifications() {
        showLoading(true)
        
        lifecycleScope.launch {
            when (val result = notificationRepository.getNotifications()) {
                is Result.Success -> {
                    allNotifications = result.data
                    updateUI()
                    showLoading(false)
                }
                is Result.Error -> {
                    showLoading(false)
                    showError("Failed to load notifications: ${result.message}")
                }
                is Result.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    private fun updateUI() {
        // Update stats
        val unreadCount = allNotifications.count { !it.isRead }
        val totalCount = allNotifications.size
        
        tvUnreadCount.text = "$unreadCount Unread"
        tvTotalCount.text = "$totalCount total notifications"
        
        // Show/hide mark all read button
        btnMarkAllRead.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        
        // Apply current filter
        applyFilter()
    }

    private fun applyFilter() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val filteredList = when (currentFilter) {
            FilterType.ALL -> allNotifications
            FilterType.UNREAD -> allNotifications.filter { !it.isRead }
            FilterType.READ -> allNotifications.filter { it.isRead }
            FilterType.TODAY -> allNotifications.filter { it.notifyDate == today }
        }
        
        adapter.submitList(filteredList)
        
        // Show/hide empty state
        if (filteredList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun markAsRead(notification: Notification) {
        lifecycleScope.launch {
            when (val result = notificationRepository.markAsRead(notification.id)) {
                is Result.Success -> {
                    // Update local list
                    allNotifications = allNotifications.map {
                        if (it.id == notification.id) it.copy(isRead = true) else it
                    }
                    updateUI()
                    
                    // Set result to update dashboard badge
                    setResult(RESULT_OK)
                }
                is Result.Error -> {
                    showError("Failed to mark as read: ${result.message}")
                }
                is Result.Loading -> { }
            }
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            when (val result = notificationRepository.markAllAsRead()) {
                is Result.Success -> {
                    // Update local list
                    allNotifications = allNotifications.map { it.copy(isRead = true) }
                    updateUI()
                    Toast.makeText(this@NotificationsActivity, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                    
                    // Set result to update dashboard badge
                    setResult(RESULT_OK)
                }
                is Result.Error -> {
                    showError("Failed to mark all as read: ${result.message}")
                }
                is Result.Loading -> { }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            rvNotifications.visibility = View.GONE
            emptyState.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_REFRESH_BADGE = "refresh_badge"
    }
}
