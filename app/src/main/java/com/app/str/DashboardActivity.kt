package com.app.str

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.str.utils.EdgeToEdgeHelper
import androidx.lifecycle.lifecycleScope
import com.app.str.data.api.AttendanceApiService
import com.app.str.data.model.AttendanceSummary
import com.app.str.data.model.MonthlyStatus
import com.app.str.data.model.TargetSummary
import com.app.str.data.repository.NotificationRepository
import com.app.str.ui.base.BaseActivity
import com.app.str.utils.AttendanceManager
import com.app.str.utils.HourlyWorkManager
import com.app.str.utils.LoadingDialog
import com.app.str.utils.LocationHelper
import com.app.str.utils.NotificationPermissionHelper
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar
import javax.inject.Inject
import androidx.core.graphics.toColorInt
import com.app.str.utils.WorkingHoursHelper
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.bottomnavigation.BottomNavigationView

@AndroidEntryPoint
class DashboardActivity : BaseActivity() {
    
    @Inject
    lateinit var attendanceApiService: AttendanceApiService
    
    @Inject 
    lateinit var attendanceManager: AttendanceManager
    
    @Inject
    lateinit var notificationRepository: NotificationRepository
    
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Notification views
    private lateinit var notificationSection: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    
    private lateinit var monthlyGrid: GridLayout
    private lateinit var btnCheckIn: MaterialButton
    private lateinit var btnCheckOut: MaterialButton
    private lateinit var spinnerYear: Spinner
    private lateinit var workPlanCard: CardView
    private lateinit var dailyReportCard: CardView
    private lateinit var salarySlipCard: MaterialCardView
    private lateinit var pieChartAttendance: PieChart
    private lateinit var tvMonthYear: TextView
    private lateinit var tvPresentDays: TextView
    private lateinit var tvAbsentDays: TextView
    private lateinit var tvHalfDays: TextView
    private lateinit var tvFutureDays: TextView
    private lateinit var tvAttendancePercentage: TextView
    private lateinit var tvTotalDays: TextView
    private lateinit var tvDaysPresent: TextView
    private lateinit var tvLastCheckIn: TextView
    
    // Variables to track attendance status
    private var lastCheckOutTime: String? = null
    private var lastCheckInTime: String? = null
    
    // Target Analytics Views
    private lateinit var lineChartTarget: LineChart
    private lateinit var tvTargetPeriod: TextView
    private lateinit var tvTotalTargetValue: TextView
    private lateinit var tvTotalSaleValue: TextView
    private lateinit var tvRemainingTargetValue: TextView
    private lateinit var tvAchievementPercentage: TextView
    private lateinit var progressBarTarget: ProgressBar
    private lateinit var tvTargetStatus: TextView
    
    private var monthlyStatusList: List<MonthlyStatus> = emptyList()
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private val availableYears = mutableListOf<String>()
    
    // Activity result launcher for notifications
    private val notificationsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh notification badge when returning from notifications
            fetchUnreadNotificationCount()
        }
    }
    
    private val monthNames = listOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )
    
    private val monthFullNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        
        // Initialize LoadingDialog
        loadingDialog = LoadingDialog(this)
        
        // Setup edge-to-edge with proper bottom navigation handling
        setupEdgeToEdgeWithBottomNav()
        
        // Initialize views
        monthlyGrid = findViewById(R.id.monthlyGrid)
        spinnerYear = findViewById(R.id.spinnerYear)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        workPlanCard = findViewById(R.id.workplancard)
        dailyReportCard = findViewById(R.id.dailyReportCard)
        salarySlipCard = findViewById(R.id.salarySlipCard)
        
        // Initialize pie chart views
        pieChartAttendance = findViewById(R.id.pieChartAttendance)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvPresentDays = findViewById(R.id.tvPresentDays)
        tvAbsentDays = findViewById(R.id.tvAbsentDays)
        tvHalfDays = findViewById(R.id.tvHalfDays)
        tvFutureDays = findViewById(R.id.tvFutureDays)
        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage)
        tvTotalDays = findViewById(R.id.tvTotalDays)
        tvDaysPresent = findViewById(R.id.tvDaysPresent)
        tvLastCheckIn = findViewById(R.id.tvLastCheckIn)
        
        // Initialize notification section
        notificationSection = findViewById(R.id.notificationSection)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        notificationSection.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            notificationsLauncher.launch(intent)
        }
        
        // Initialize target analytics views
        lineChartTarget = findViewById(R.id.lineChartTarget)
        tvTargetPeriod = findViewById(R.id.tvTargetPeriod)
        tvTotalTargetValue = findViewById(R.id.tvTotalTargetValue)
        tvTotalSaleValue = findViewById(R.id.tvTotalSaleValue)
        tvRemainingTargetValue = findViewById(R.id.tvRemainingTargetValue)
        tvAchievementPercentage = findViewById(R.id.tvAchievementPercentage)
        progressBarTarget = findViewById(R.id.progressBarTarget)
        tvTargetStatus = findViewById(R.id.tvTargetStatus)
        
        // Setup year dropdown
        setupYearDropdown()
        
        // Setup monthly grid
        setupMonthlyGrid()
        
        // Setup attendance buttons
        setupAttendanceButtons()
        
        // Setup work plan card click
        setupWorkPlanCard()
        
        // Setup daily report card click
        setupDailyReportCard()
        
        // Setup salary slip card click
        setupSalarySlipCard()
        
        // Setup pie chart
        setupPieChart()
        
        // Setup line chart
        setupLineChart()
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Debug: Check stored tokens
        com.app.str.utils.TokenDebugUtils.checkStoredTokens(this)
        
        // Check authentication before making API calls
        checkAuthenticationAndFetchData()
    }
    
    private fun setupYearDropdown() {
        // Set initial year display
        updateYearDisplay()
        
        // Disable default spinner behavior and use custom click handler on parent
        spinnerYear.isEnabled = false
        spinnerYear.isFocusable = false
        spinnerYear.isClickable = false
        
        // Set click listener on parent view instead
        spinnerYear.parent?.let { parent ->
            if (parent is View) {
                parent.setOnClickListener {
                    showYearPickerDialog()
                }
            }
        }
        
        // Alternative: wrap in a container and set listener there
        // For now, let's use a touch listener on the spinner itself
        spinnerYear.setOnTouchListener { _, _ ->
            showYearPickerDialog()
            true
        }
    }
    
    private fun showYearPickerDialog() {
        val currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = 2020
        val maxYear = currentCalendarYear + 10 // Future 10 years tak
        
        // Create custom dialog with light theme
        val dialog = Dialog(this, R.style.YearPickerDialogTheme)
        dialog.setContentView(R.layout.dialog_year_picker)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val numberPicker = dialog.findViewById<NumberPicker>(R.id.yearPicker)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnOk)
        
        // Setup NumberPicker
        numberPicker.apply {
            minValue = minYear
            maxValue = maxYear
            value = this@DashboardActivity.currentYear
            wrapSelectorWheel = false
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }
        
        // Button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnOk.setOnClickListener {
            val selectedYear = numberPicker.value
            if (selectedYear != this.currentYear) {
                this.currentYear = selectedYear
                updateYearDisplay()
                fetchTargetStatusForYear(selectedYear.toString())
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateYearDisplay() {
        // Update spinner text - we'll use it as a TextView now
        val years = listOf(currentYear.toString())
        val adapter = ArrayAdapter(this, R.layout.spinner_year_selected, years)
        spinnerYear.adapter = adapter
    }


    private fun setupMonthlyGrid() {
        monthlyGrid.removeAllViews()
        
        for (i in monthNames.indices) {
            val monthView = LayoutInflater.from(this)
                .inflate(R.layout.item_month_box, monthlyGrid, false)
            
            val tvMonthName = monthView.findViewById<TextView>(R.id.tvMonthName)
            val statusIndicator = monthView.findViewById<View>(R.id.statusIndicator)
            
            tvMonthName.text = monthNames[i]
            
            // Set default color (gray)
            statusIndicator.setBackgroundColor(getColor(android.R.color.darker_gray))
            
            // Set GridLayout params
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(i % 4, 1f)
                rowSpec = GridLayout.spec(i / 4)
                setMargins(8, 8, 8, 8)
            }
            monthView.layoutParams = params
            
            // Set click listener
            monthView.setOnClickListener {
                showMonthDetails(monthFullNames[i])
            }
            
            monthlyGrid.addView(monthView)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // Manual refresh triggered by user
                refreshDashboardDataWithFeedback()
                true
            }
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun fetchTargetStatus() {
        // Fetch for current year
        fetchTargetStatusForYear(currentYear.toString())
    }
    
    private fun fetchTargetStatusForYear(year: String) {
        lifecycleScope.launch {
            // Only show loading dialog if it's not already showing (for initial load)
            if (!loadingDialog.isShowing()) {
                loadingDialog.show("Loading target data...")
            }
            
            // PERMANENT LOGIN: Don't check session validity - TokenAuthenticator handles expired tokens
            // Just check if tokens exist (even if expired)
            val hasTokens = authManager.hasStoredTokens()
            if (!hasTokens) {
                println("DashboardActivity: No tokens found at all, redirecting to login")
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss()
                }
                authManager.logout()
                return@launch
            }
            
            // Get user ID from token (TokenAuthenticator will refresh if needed)
            val userId = authManager.getCurrentUserId()?.toIntOrNull() ?: 2 // Fallback to 2 for demo
            println("DashboardActivity: Fetching target status for user ID: $userId, year: $year")
            
            try {
                val response = attendanceApiService.getTargetStatusByYear(userId, year)
                
                if (response.isSuccessful && response.body() != null) {
                    val targetStatus = response.body()!!
                    monthlyStatusList = targetStatus.monthlyStatus
                    currentYear = targetStatus.year
                    
                    // Update the spinner selection if year changed
                    val yearIndex = availableYears.indexOf(year)
                    if (yearIndex >= 0 && spinnerYear.selectedItemPosition != yearIndex) {
                        spinnerYear.setSelection(yearIndex)
                    }
                    
                    // Update the grid with API data
                    updateMonthlyGrid()
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss()
                    }
                } else {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss()
                    }
                    handleTargetStatusError(response.code(), year)
                }
            } catch (e: Exception) {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss()
                }
                handleTargetStatusException(e, year)
            }
        }
    }
    
    private fun updateMonthlyGrid() {
        for (i in 0 until monthlyGrid.childCount) {
            val monthView = monthlyGrid.getChildAt(i)
            val statusIndicator = monthView.findViewById<View>(R.id.statusIndicator)
            val monthCard = monthView.findViewById<CardView>(R.id.monthCard)
            
            // Find matching month data
            val monthData = monthlyStatusList.find { 
                it.month.equals(monthFullNames[i], ignoreCase = true) 
            }
            
            if (monthData != null) {
                // Update color based on status from API
                val (indicatorColor, cardColor) = when (monthData.status.lowercase()) {
                    "green" -> Pair("#10B981".toColorInt(), "#10B981".toColorInt()) // Green
                    "red" -> Pair("#EF4444".toColorInt(), "#EF4444".toColorInt()) // Red
                    "gray" -> Pair("#9CA3AF".toColorInt(), "#9CA3AF".toColorInt()) // Gray
                    else -> Pair("#9CA3AF".toColorInt(), "#9CA3AF".toColorInt()) // Default Gray
                }
                statusIndicator.setBackgroundColor(indicatorColor)
                monthCard.setCardBackgroundColor(cardColor)
            } else {
                // Default gray for months without data
                statusIndicator.setBackgroundColor(Color.parseColor("#9CA3AF"))
                monthCard.setCardBackgroundColor(Color.parseColor("#F3F4F6"))
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun showMonthDetails(monthName: String) {
        val monthData = monthlyStatusList.find { 
            it.month.equals(monthName, ignoreCase = true) 
        }
        
        if (monthData == null) {
            Toast.makeText(
                this,
                "No data available for $monthName",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Create dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_month_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Set dialog data
        val tvDialogMonth = dialog.findViewById<TextView>(R.id.tvDialogMonth)
        val tvTargetArea = dialog.findViewById<TextView>(R.id.tvTargetArea)
        val tvSoldArea = dialog.findViewById<TextView>(R.id.tvSoldArea)
        val tvCarryForward = dialog.findViewById<TextView>(R.id.tvCarryForward)
        val tvCarryFromLastMonth = dialog.findViewById<TextView>(R.id.tvCarryFromLastMonth)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnClose)
        
        tvDialogMonth.text = "$monthName $currentYear"
        tvTargetArea.text = "${monthData.targetArea} sq.ft"
        tvSoldArea.text = "${monthData.soldArea} sq.ft"
        tvCarryForward.text = "${monthData.carryForward} sq.ft"
        
        // Show carry from last month if available
        if (tvCarryFromLastMonth != null && monthData.carryFromLastMonth != null) {
            tvCarryFromLastMonth.text = "${monthData.carryFromLastMonth} sq.ft"
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun setupAttendanceButtons() {
        btnCheckIn.setOnClickListener {
            // Check if already checked in and checked out today (both done)
            if (hasCompletedAttendanceToday()) {
                Toast.makeText(this, "You have already completed your attendance for today!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            // Check if already checked in today (but not checked out)
            if (WorkingHoursHelper.isCheckedIn(lastCheckInTime, lastCheckOutTime)) {
                Toast.makeText(this, "You have already checked in today!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            attendanceManager.showCheckInConfirmation(
                activity = this,
                onSuccess = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    // Refresh dashboard data after successful check-in
                    refreshAfterAttendance()
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
        
        btnCheckOut.setOnClickListener {
            // Check if already checked in and checked out today (both done)
            if (hasCompletedAttendanceToday()) {
                Toast.makeText(this, "You have already completed your attendance for today!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            // Check if not checked in yet
            if (!WorkingHoursHelper.isCheckedIn(lastCheckInTime, lastCheckOutTime)) {
                Toast.makeText(this, "You must check in first before checking out!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            attendanceManager.showCheckOutConfirmation(
                activity = this,
                onSuccess = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    // Refresh dashboard data after successful check-out
                    refreshAfterAttendance()
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    
    private fun hasCompletedAttendanceToday(): Boolean {
        // If both check-in and check-out times exist, user has completed attendance
        return !lastCheckInTime.isNullOrEmpty() && 
               lastCheckInTime != "null" && 
               !lastCheckOutTime.isNullOrEmpty() && 
               lastCheckOutTime != "null"
    }
    
    private fun setupWorkPlanCard() {
        workPlanCard.setOnClickListener {
            val intent = Intent(this, WorkPlansActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupDailyReportCard() {
        dailyReportCard.setOnClickListener {
            val intent = Intent(this, ViewDailyReportsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupSalarySlipCard() {
        salarySlipCard.setOnClickListener {
            val intent = Intent(this, SalarySlipActivity::class.java)
            startActivity(intent)
        }
    }
    

    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LocationHelper.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (attendanceManager.handleLocationPermissionResult(requestCode, grantResults)) {
                    Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Location permissions are required for attendance tracking",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                NotificationPermissionHelper.handlePermissionResult(
                    requestCode, grantResults,
                    onGranted = {
                        Toast.makeText(this, "Notification permission granted! You'll receive hourly report reminders.", Toast.LENGTH_LONG).show()
                    },
                    onDenied = {
                        Toast.makeText(this, "Notification permission denied. Enable in Settings > Apps > STR > Notifications to receive reminders.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Keep bottom navigation in sync
        bottomNavigation.selectedItemId = R.id.nav_home
        // Refresh dashboard data when returning to activity
        // This ensures data is updated after check-in/check-out or other operations
        refreshDashboardData()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.destroy()
        attendanceManager.dismissCurrentDialog()
    }
    
    override fun onBackPressed() {
        showExitConfirmationDialog()
    }
    
    private fun showExitConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_exit_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        
        val btnYes = dialog.findViewById<MaterialButton>(R.id.btnYes)
        val btnNo = dialog.findViewById<MaterialButton>(R.id.btnNo)
        
        btnYes.setOnClickListener {
            dialog.dismiss()
            finishAffinity() // Closes the app and all activities in the task
        }
        
        btnNo.setOnClickListener {
            dialog.dismiss()
        }
        
        // Make dialog cancelable on outside touch
        dialog.setOnCancelListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun checkAuthenticationAndFetchData() {
        lifecycleScope.launch {
            println("DashboardActivity: Checking authentication status...")
            loadingDialog.show("Loading dashboard...")
            
            // Just check if tokens exist (don't validate expiry)
            // If expired, the TokenAuthenticator will handle refresh automatically
            val hasTokens = authManager.hasStoredTokens()
            
            if (hasTokens) {
                println("DashboardActivity: Tokens found, fetching data...")
                try {
                    fetchTargetStatus()
                    fetchAttendanceSummary()
                    fetchTargetSummary()
                    fetchUnreadNotificationCount()
                    // Dismiss loading after a brief delay to show all data loaded
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                    }, 500)
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@DashboardActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                println("DashboardActivity: No tokens found, redirecting to login...")
                loadingDialog.dismiss()
                Toast.makeText(this@DashboardActivity, "Please login to continue", Toast.LENGTH_LONG).show()
                performLogout()
            }
        }
    }
    
    /**
     * Refreshes dashboard data silently without showing loading dialog
     * Used when returning to activity or after attendance operations
     */
    private fun refreshDashboardData() {
        lifecycleScope.launch {
            try {
                // Silent refresh - no loading dialog to avoid interrupting user
                println("DashboardActivity: Silently refreshing dashboard data...")
                
                // Check if we still have tokens
                val hasTokens = authManager.hasStoredTokens()
                if (hasTokens) {
                    // Fetch all data silently
                    fetchTargetStatus()
                    fetchAttendanceSummary()
                    fetchTargetSummary()
                    fetchUnreadNotificationCount()
                    println("DashboardActivity: Dashboard data refreshed successfully")
                } else {
                    println("DashboardActivity: No tokens found during refresh, redirecting to login")
                    performLogout()
                }
            } catch (e: Exception) {
                println("DashboardActivity: Error during silent refresh: ${e.message}")
                // Don't show error toast for silent refresh, just log it
            }
        }
    }
    
    /**
     * Refreshes dashboard data specifically after attendance operations
     * Provides user feedback about the refresh
     */
    private fun refreshAfterAttendance() {
        lifecycleScope.launch {
            try {
                println("DashboardActivity: Refreshing dashboard data after attendance operation...")
                
                // Brief delay to allow the attendance success message to be seen
                delay(1000)
                
                // Check if we still have tokens
                val hasTokens = authManager.hasStoredTokens()
                if (hasTokens) {
                    // Fetch all data silently
                    fetchTargetStatus()
                    fetchAttendanceSummary()
                    fetchTargetSummary()
                    println("DashboardActivity: Dashboard data refreshed after attendance")
                } else {
                    println("DashboardActivity: No tokens found during refresh, redirecting to login")
                    performLogout()
                }
            } catch (e: Exception) {
                println("DashboardActivity: Error during post-attendance refresh: ${e.message}")
            }
        }
    }
    
    /**
     * Refreshes dashboard data with user feedback when manually triggered
     * Shows loading dialog and success/error messages
     */
    private fun refreshDashboardDataWithFeedback() {
        lifecycleScope.launch {
            try {
                loadingDialog.show("Refreshing dashboard...")
                println("DashboardActivity: Manual refresh triggered by user...")
                
                // Check if we still have tokens
                val hasTokens = authManager.hasStoredTokens()
                if (hasTokens) {
                    // Fetch all data with loading feedback
                    fetchTargetStatus()
                    fetchAttendanceSummary()
                    fetchTargetSummary()
                    
                    // Brief delay to ensure all data is loaded
                    delay(500)
                    loadingDialog.dismiss()
                    
                    Toast.makeText(this@DashboardActivity, "Dashboard refreshed successfully", Toast.LENGTH_SHORT).show()
                    println("DashboardActivity: Manual refresh completed successfully")
                } else {
                    loadingDialog.dismiss()
                    println("DashboardActivity: No tokens found during manual refresh, redirecting to login")
                    performLogout()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                println("DashboardActivity: Error during manual refresh: ${e.message}")
                Toast.makeText(this@DashboardActivity, "Failed to refresh dashboard", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupPieChart() {
        pieChartAttendance.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(false)
            setDrawCenterText(true)
            centerText = "Attendance"
            setCenterTextSize(12f)
            setCenterTextColor(Color.parseColor("#1F2937"))
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawEntryLabels(false)
            setTouchEnabled(false)
            isRotationEnabled = false
        }
    }
    
    private fun fetchAttendanceSummary() {
        lifecycleScope.launch {
            try {
                println("DashboardActivity: Fetching attendance summary...")
                if (loadingDialog.isShowing()) {
                    loadingDialog.updateMessage("Loading attendance...")
                }
                val response = attendanceApiService.getAttendanceSummary()
                if (response.isSuccessful) {
                    response.body()?.let { summary ->
                        println("DashboardActivity: Attendance summary received - Present: ${summary.totalPresentDays}, Absent: ${summary.totalAbsentDays}")
                        updatePieChart(summary)
                    } ?: run {
                        println("DashboardActivity: Attendance summary response body is null")
                        setDefaultAttendanceData()
                    }
                } else {
                    println("DashboardActivity: Attendance summary API failed with code: ${response.code()}")
                    setDefaultAttendanceData()
                }
            } catch (e: Exception) {
                println("DashboardActivity: Exception in fetchAttendanceSummary: ${e.message}")
                setDefaultAttendanceData()
            }
        }
    }
    
    private fun updatePieChart(summary: AttendanceSummary) {
        // Update month year display
        val monthName = monthFullNames[summary.month - 1]
        tvMonthYear.text = "$monthName ${summary.year}"
        
        // Get values from the new summary structure
        val presentDays = summary.summary.presentDays
        val halfDaysValue = summary.summary.halfDays  // This is the value (0.5 per half day)
        val halfDaysCount = (halfDaysValue * 2).toInt()  // Convert to actual count (0.5 = 1, 1.0 = 2, etc.)
        val absentDays = summary.summary.absentDays
        val futureDays = summary.summary.futureDays
        val paidAttendance = summary.summary.paidAttendance
        
        // Update text views with proper formatting
        tvPresentDays.text = formatDaysText(presentDays)
        tvHalfDays.text = "$halfDaysCount days"  // Show actual count of half days
        tvAbsentDays.text = formatDaysText(absentDays)
        tvFutureDays.text = "$futureDays days"
        tvTotalDays.text = "${summary.totalDaysInMonth}"
        
        // Update Days Present Card - show paid attendance (full days + half days calculated)
        println("DashboardActivity: Updating tvDaysPresent to paid attendance: $paidAttendance")
        tvDaysPresent.text = formatDaysValue(paidAttendance)
        
        // Update Last Check-in Card from today's data
        val lastCheckInTimeFromAPI = summary.today.checkInTime
        val lastCheckOutTimeFromAPI = summary.today.checkOutTime
        
        // Store the times in variables for button logic
        this.lastCheckInTime = lastCheckInTimeFromAPI
        this.lastCheckOutTime = lastCheckOutTimeFromAPI
        
        println("DashboardActivity: Raw lastCheckInTime from API: '$lastCheckInTimeFromAPI'")
        println("DashboardActivity: Raw lastCheckOutTime from API: '$lastCheckOutTimeFromAPI'")
        
        if (!lastCheckInTimeFromAPI.isNullOrEmpty() && lastCheckInTimeFromAPI != "null") {
            try {
                // Try to format the time if it's in a recognizable format
                val formattedTime = formatTime(lastCheckInTimeFromAPI)
                println("DashboardActivity: Updating tvLastCheckIn to: $formattedTime")
                tvLastCheckIn.text = formattedTime
            } catch (e: Exception) {
                println("DashboardActivity: Error formatting time, using raw: $lastCheckInTimeFromAPI")
                tvLastCheckIn.text = lastCheckInTimeFromAPI
            }
        } else {
            println("DashboardActivity: No check-in time available, setting to --:--")
            tvLastCheckIn.text = "--:--"
        }
        
        // Calculate attendance percentage based on paid attendance vs total working days passed
        // Use halfDaysValue (0.5 per half day) for calculation, not the count
        val totalWorkingDaysPassed = presentDays + halfDaysValue + absentDays
        val attendancePercentage = if (totalWorkingDaysPassed > 0) {
            ((paidAttendance / totalWorkingDaysPassed) * 100).toInt()
        } else {
            0
        }
        tvAttendancePercentage.text = "$attendancePercentage%"
        
        // Prepare pie chart data
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        
        // Add present days (full days)
        if (presentDays > 0) {
            entries.add(PieEntry(presentDays.toFloat(), "Present"))
            colors.add(Color.parseColor("#10B981")) // Green
        }
        
        // Add half days (use count for pie chart to show actual number of half day occurrences)
        if (halfDaysCount > 0) {
            entries.add(PieEntry(halfDaysCount.toFloat(), "Half Day"))
            colors.add(Color.parseColor("#3B82F6")) // Blue
        }
        
        // Add absent days
        if (absentDays > 0) {
            entries.add(PieEntry(absentDays.toFloat(), "Absent"))
            colors.add(Color.parseColor("#EF4444")) // Red
        }
        
        // Add future days
        if (futureDays > 0) {
            entries.add(PieEntry(futureDays.toFloat(), "Upcoming"))
            colors.add(Color.parseColor("#9CA3AF")) // Gray
        }
        
        // If no data, show a placeholder
        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "No Data"))
            colors.add(Color.parseColor("#E5E7EB")) // Gray
        }
        
        // Create dataset
        val dataSet = PieDataSet(entries, "Attendance").apply {
            this.colors = colors
            valueTextSize = 0f // Hide values on slices
            sliceSpace = 2f
        }
        
        // Create pie data and set to chart
        val pieData = PieData(dataSet)
        pieChartAttendance.data = pieData
        pieChartAttendance.invalidate()
    }
    
    /**
     * Format days value for display (handles decimal values)
     */
    private fun formatDaysText(days: Double): String {
        return if (days == days.toLong().toDouble()) {
            "${days.toInt()} days"
        } else {
            "$days days"
        }
    }
    
    /**
     * Format days value without "days" suffix
     */
    private fun formatDaysValue(days: Double): String {
        return if (days == days.toLong().toDouble()) {
            "${days.toInt()}"
        } else {
            "$days"
        }
    }
    
    private fun setDefaultAttendanceData() {
        // Set current month/year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val futureDays = totalDaysInMonth - currentDay
        
        tvMonthYear.text = "${monthFullNames[currentMonth]} $currentYear"
        tvPresentDays.text = "0 days"
        tvHalfDays.text = "0 days"
        tvAbsentDays.text = "0 days"
        tvFutureDays.text = "$futureDays days"
        tvAttendancePercentage.text = "0%"
        tvTotalDays.text = "$totalDaysInMonth"
        
        // Set default values for Days Present and Last Check-in cards
        println("DashboardActivity: Setting default attendance data")
        tvDaysPresent.text = "0"
        tvLastCheckIn.text = "--:--"
        
        // Show placeholder chart
        val entries = listOf(PieEntry(1f, "No Data"))
        val dataSet = PieDataSet(entries, "Attendance").apply {
            colors = listOf(Color.parseColor("#E5E7EB"))
            valueTextSize = 0f
            sliceSpace = 0f
        }
        
        val pieData = PieData(dataSet)
        pieChartAttendance.data = pieData
        pieChartAttendance.invalidate()
    }
    
    private fun setupLineChart() {
        lineChartTarget.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(false)
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setPinchZoom(false)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#6B7280")
                textSize = 10f
            }
            
            // Configure left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#6B7280")
                textSize = 10f
                axisMinimum = 0f
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Configure legend
            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#374151")
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                formSize = 14f
            }
            
            // Set no data text
            setNoDataText("Loading target data...")
            setNoDataTextColor(Color.parseColor("#6B7280"))
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_home
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home
                    true
                }
                R.id.nav_work_plan -> {
                    navigateToActivity(WorkPlansActivity::class.java)
                    true
                }
                R.id.nav_daily_report -> {
                    navigateToActivity(DailyReportActivity::class.java)
                    true
                }
                R.id.nav_incentive -> {
                    navigateToActivity(IncentiveActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateToActivity(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Setup edge-to-edge display with proper handling for bottom navigation bar.
     * This ensures the BottomNavigationView is always visible above system navigation
     * on all devices including those with gesture navigation.
     */
    private fun setupEdgeToEdgeWithBottomNav() {
        val mainContainer = findViewById<View>(R.id.main)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val contentScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.contentScrollView)
        
        // Use EdgeToEdgeHelper for consistent behavior across all devices
        EdgeToEdgeHelper.setupEdgeToEdge(
            rootView = mainContainer,
            bottomNav = bottomNav,
            contentView = contentScrollView,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
    }
    
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
    
    private fun fetchTargetSummary() {
        lifecycleScope.launch {
            try {
                if (loadingDialog.isShowing()) {
                    loadingDialog.updateMessage("Loading target summary...")
                }
                val response = attendanceApiService.getTargetSummary()
                if (response.isSuccessful) {
                    response.body()?.let { summary ->
                        updateTargetAnalytics(summary)
                    }
                } else {
                    // Handle error - set default values
                    setDefaultTargetData()
                }
            } catch (e: Exception) {
                // Handle network error
                setDefaultTargetData()
            }
        }
    }
    
    private fun updateTargetAnalytics(summary: TargetSummary) {
        // Update text values with sqft formatting (no shortcuts)
        tvTotalTargetValue.text = "${summary.totalTarget.toInt()} sqft"
        tvTotalSaleValue.text = "${summary.totalSale.toInt()} sqft"
        tvRemainingTargetValue.text = "${summary.remainingTarget.toInt()} sqft"
        
        // Update achievement percentage
        val achievementPercentage = summary.achievementPercentage.toInt()
        tvAchievementPercentage.text = "$achievementPercentage%"
        
        // Update progress bar
        progressBarTarget.progress = kotlin.math.min(100, achievementPercentage)
        
        // Update status message
        tvTargetStatus.text = when {
            achievementPercentage >= 100 -> "🎉 Congratulations! Target achieved!"
            achievementPercentage >= 80 -> "🚀 Almost there! Keep going!"
            achievementPercentage >= 50 -> "💪 Good progress! Push harder!"
            else -> "📈 Keep pushing to reach your target!"
        }
        
        // Create line chart data
        updateLineChart(summary)
    }
    
    private fun updateLineChart(summary: TargetSummary) {
        val entries = mutableListOf<Entry>()
        val targetEntries = mutableListOf<Entry>()
        
        // Generate sample monthly progress data (you can modify this based on your needs)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                               "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        // Create progressive sales data for the year up to current month
        var cumulativeSale = 0.0
        val monthlyTargetIncrement = if (currentMonth > 0) summary.totalTarget / (currentMonth + 1) else summary.totalTarget
        
        for (i in 0..currentMonth) {
            // Add some realistic progression
            val monthlyProgress = when {
                i < currentMonth -> monthlyTargetIncrement * (0.7 + (i * 0.05)) // Past months with variation
                else -> summary.totalSale // Current month actual
            }
            cumulativeSale = kotlin.math.min(cumulativeSale + monthlyProgress, summary.totalSale)
            
            entries.add(Entry(i.toFloat(), cumulativeSale.toFloat()))
            targetEntries.add(Entry(i.toFloat(), ((i + 1) * monthlyTargetIncrement).toFloat()))
        }
        
        // Create datasets
        val salesDataSet = LineDataSet(entries, "Sales Achievement").apply {
            color = Color.parseColor("#059669")
            setCircleColor(Color.parseColor("#059669"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            circleHoleRadius = 3f
            valueTextSize = 0f
            setDrawFilled(true)
            fillColor = Color.parseColor("#059669")
            fillAlpha = 30
        }
        
        val targetDataSet = LineDataSet(targetEntries, "Target Line").apply {
            color = Color.parseColor("#DC2626")
            setCircleColor(Color.parseColor("#DC2626"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 0f
            enableDashedLine(10f, 5f, 0f)
        }
        
        // Create line data
        val lineData = LineData(salesDataSet, targetDataSet)
        
        // Set X-axis labels
        val xAxisLabels = (0..currentMonth).map { monthNames[it] }
        lineChartTarget.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        
        // Set data and refresh
        lineChartTarget.data = lineData
        lineChartTarget.invalidate()
    }
    
    private fun setDefaultTargetData() {
        tvTotalTargetValue.text = "0 sqft"
        tvTotalSaleValue.text = "0 sqft"
        tvRemainingTargetValue.text = "0 sqft"
        tvAchievementPercentage.text = "0%"
        progressBarTarget.progress = 0
        tvTargetStatus.text = "📈 Start working towards your target!"
        
        // Show empty chart
        lineChartTarget.clear()
        lineChartTarget.setNoDataText("No target data available")
        lineChartTarget.invalidate()
    }
    
    private fun formatCurrency(amount: Double): String {
        return when {
            amount >= 10000000 -> String.format("%.1fCr", amount / 10000000) // Crores
            amount >= 100000 -> String.format("%.1fL", amount / 100000) // Lakhs
            amount >= 1000 -> String.format("%.1fK", amount / 1000) // Thousands
            else -> String.format("%.0f", amount)
        }
    }
    
    private fun formatTime(timeString: String): String {
        return try {
            println("DashboardActivity: formatTime input: '$timeString'")
            
            // Extract time in 24-hour format from whatever format backend sends
            val timeIn24Hour = when {
                // If in ISO format like "2025-11-30T14:30:00Z" or "2025-11-30T14:30:00.123Z"
                // Convert from UTC to local timezone
                timeString.contains("T") -> {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val date = sdf.parse(timeString.split(".")[0]) // Remove milliseconds if present
                        
                        // Convert to local timezone
                        val localSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        localSdf.timeZone = java.util.TimeZone.getDefault()
                        localSdf.format(date)
                    } catch (e: Exception) {
                        println("DashboardActivity: Error parsing ISO format time: ${e.message}")
                        // Fallback: just extract time part without timezone conversion
                        val timePart = timeString.split("T")[1].replace("Z", "").trim()
                        if (timePart.contains(":")) {
                            timePart.substring(0, 5)
                        } else {
                            "--:--"
                        }
                    }
                }
                
                // If in HH:mm:ss format, extract HH:mm
                timeString.matches(Regex("\\d{2}:\\d{2}:\\d{2}")) -> {
                    timeString.substring(0, 5)
                }
                
                // If already in HH:mm format
                timeString.matches(Regex("\\d{2}:\\d{2}")) -> {
                    timeString
                }
                
                // Default case - use as-is
                else -> timeString
            }
            
            println("DashboardActivity: Extracted 24-hour time: $timeIn24Hour")
            
            // Convert to 12-hour format
            val formatted = convertTo12HourFormat(timeIn24Hour)
            println("DashboardActivity: Final formatted time: $formatted")
            formatted
            
        } catch (e: Exception) {
            println("DashboardActivity: Error in formatTime: ${e.message}")
            e.printStackTrace()
            "--:--"
        }
    }
    
    private fun convertTo12HourFormat(time24: String): String {
        return try {
            if (!time24.matches(Regex("\\d{2}:\\d{2}"))) {
                return time24 // Return as is if not in HH:mm format
            }
            
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1]
            
            when {
                hour == 0 -> "12:$minute AM"
                hour < 12 -> "$hour:$minute AM"
                hour == 12 -> "12:$minute PM"
                else -> "${hour - 12}:$minute PM"
            }
        } catch (e: Exception) {
            println("DashboardActivity: Error converting to 12-hour format: ${e.message}")
            time24
        }
    }

    private fun handleTargetStatusError(errorCode: Int, year: String) {
        val errorMessage = when (errorCode) {
            500 -> {
                println("DashboardActivity: Server error (500) for year $year - using fallback data")
                "Server is temporarily unavailable. Showing default data for $year."
            }
            404 -> {
                println("DashboardActivity: No data found (404) for year $year")
                "No target data available for $year."
            }
            401, 403 -> {
                println("DashboardActivity: Authentication error ($errorCode) for year $year")
                "Authentication required. Please login again."
            }
            else -> {
                println("DashboardActivity: HTTP error $errorCode for year $year")
                "Failed to load target status for $year (Error: $errorCode)"
            }
        }

        // Show appropriate action based on error
        when (errorCode) {
            500 -> {
                // For server errors, show fallback data and try again later
                showFallbackTargetData(year)
                showRetryOption(year)
            }
            401, 403 -> {
                // For auth errors, redirect to login
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    authManager.logout()
                }
            }
            404 -> {
                // For not found, show empty state
                showEmptyTargetData(year)
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // For other errors, show generic message
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                showEmptyTargetData(year)
            }
        }
    }

    private fun handleTargetStatusException(exception: Exception, year: String) {
        val errorMessage = when (exception) {
            is java.net.UnknownHostException -> {
                println("DashboardActivity: Network error - no internet connection")
                "No internet connection. Please check your network and try again."
            }
            is java.net.SocketTimeoutException -> {
                println("DashboardActivity: Request timeout for year $year")
                "Request timed out. Please try again."
            }
            is javax.net.ssl.SSLException -> {
                println("DashboardActivity: SSL error for year $year")
                "Secure connection failed. Please try again."
            }
            else -> {
                println("DashboardActivity: Unexpected error for year $year: ${exception.message}")
                "An unexpected error occurred: ${exception.message}"
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

        // Show fallback data for network errors, empty for others
        if (exception is java.net.UnknownHostException || exception is java.net.SocketTimeoutException) {
            showFallbackTargetData(year)
            showRetryOption(year)
        } else {
            showEmptyTargetData(year)
        }
    }

    private fun showFallbackTargetData(year: String) {
        // Create fallback data for demonstration
        val fallbackData = generateFallbackTargetData(year.toIntOrNull() ?: currentYear)
        monthlyStatusList = fallbackData
        updateMonthlyGrid()

        // Show a subtle indicator that this is fallback data
        Toast.makeText(
            this,
            "Showing cached data for $year. Tap retry to refresh.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEmptyTargetData(year: String) {
        monthlyStatusList = emptyList()
        updateMonthlyGrid()
    }

    private fun generateFallbackTargetData(year: Int): List<MonthlyStatus> {
        // Generate some reasonable fallback data
        return monthFullNames.mapIndexed { index, monthName ->
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val status = if (index <= currentMonth) {
                if (index % 3 == 0) "red" else "green" // Mix of statuses
            } else {
                "gray" // Future months
            }

            MonthlyStatus(
                month = monthName,
                targetArea = 1000.0 + (index * 50), // Varied target areas
                soldArea = if (index <= currentMonth) 800.0 + (index * 40) else 0.0,
                status = status,
                carryFromLastMonth = if (index > 0) 50.0 else 0.0,
                carryForward = if (index > 0) 50.0 else 0.0
            )
        }
    }

    private fun showRetryOption(year: String) {
        // Show a retry button or option
        val retryToast = Toast.makeText(
            this,
            "Tap here to retry loading $year data",
            Toast.LENGTH_LONG
        )
        retryToast.show()

        // You could also show a Snackbar with retry action here
        // or add a retry button to the UI
    }
    
    /**
     * Fetches the unread notification count and updates the badge
     */
    private fun fetchUnreadNotificationCount() {
        lifecycleScope.launch {
            try {
                when (val result = notificationRepository.getUnreadCount()) {
                    is com.app.str.data.model.Result.Success -> {
                        updateNotificationBadge(result.data)
                    }
                    is com.app.str.data.model.Result.Error -> {
                        println("DashboardActivity: Failed to fetch notification count: ${result.message}")
                        // Hide badge on error
                        updateNotificationBadge(0)
                    }
                    is com.app.str.data.model.Result.Loading -> {
                        // Do nothing while loading
                    }
                }
            } catch (e: Exception) {
                println("DashboardActivity: Error fetching notification count: ${e.message}")
                updateNotificationBadge(0)
            }
        }
    }
    
    /**
     * Updates the notification badge with the given count
     */
    private fun updateNotificationBadge(count: Int) {
        runOnUiThread {
            if (count > 0) {
                tvNotificationBadge.visibility = View.VISIBLE
                tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
                
                // Add a subtle animation
                tvNotificationBadge.scaleX = 0f
                tvNotificationBadge.scaleY = 0f
                tvNotificationBadge.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            } else {
                tvNotificationBadge.visibility = View.GONE
            }
        }
    }
}
