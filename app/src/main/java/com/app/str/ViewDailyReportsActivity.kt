package com.app.str

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.str.adapter.HourlyReportsAdapter
import com.app.str.utils.EdgeToEdgeHelper
import com.app.str.data.api.DailyReportApiService
import com.app.str.data.model.HourlyReportDetail
import com.app.str.data.model.HourlyReportResponse
import com.app.str.data.model.UpdateHourlyReportDetail
import com.app.str.data.model.UpdateHourlyReportRequest
import com.app.str.databinding.ActivityViewDailyReportsBinding
import com.app.str.ui.base.BaseActivity
import com.app.str.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ViewDailyReportsActivity : BaseActivity() {

    private lateinit var binding: ActivityViewDailyReportsBinding
    
    @Inject
    lateinit var dailyReportApiService: DailyReportApiService
    
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var recyclerViewReports: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var reportsAdapter: HourlyReportsAdapter
    private var reportsList = mutableListOf<HourlyReportResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityViewDailyReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadingDialog = LoadingDialog(this)
        
        // FIXED: Proper edge-to-edge handling with dynamic content padding
        setupEdgeToEdgeWithBottomNav()
        
        initializeViews()
        setupBackButton()
        setupRecyclerView()
        setupBottomNavigation()
        setupSwipeRefresh()
        fetchReports()
    }
    
    /**
     * Setup edge-to-edge display with proper handling for bottom navigation bar.
     */
    private fun setupEdgeToEdgeWithBottomNav() {
        // Use EdgeToEdgeHelper for consistent behavior across all devices
        EdgeToEdgeHelper.setupEdgeToEdge(
            rootView = binding.main,
            bottomNav = binding.bottomNavigation,
            contentView = binding.recyclerViewReports,
            appBarLayout = binding.appBarLayout,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
    }

    private fun initializeViews() {
        recyclerViewReports = binding.recyclerViewReports
        emptyStateLayout = binding.emptyStateLayout
        bottomNavigation = binding.bottomNavigation
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Create report button in empty state
        binding.btnCreateReport.setOnClickListener {
            val intent = Intent(this, DailyReportActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.button_color)
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchReports()
        }
    }

    private fun setupRecyclerView() {
        reportsAdapter = HourlyReportsAdapter(reportsList) { report, detail, detailIndex ->
            // Handle edit click for individual customer detail
            openEditCustomerDetail(report, detail, detailIndex)
        }
        
        recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(this@ViewDailyReportsActivity)
            adapter = reportsAdapter
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_work_plan -> {
                    val intent = Intent(this, WorkPlansActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_daily_report -> {
                    // Already on this screen
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        
        // Set selected item
        bottomNavigation.selectedItemId = R.id.nav_daily_report
    }

    private fun fetchReports() {
        lifecycleScope.launch {
            try {
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    loadingDialog.show("Loading reports...")
                }
                
                val response = dailyReportApiService.getHourlyReports()
                
                if (response.isSuccessful && response.body() != null) {
                    reportsList.clear()
                    reportsList.addAll(response.body()!!)
                    
                    updateStats()
                    
                    if (reportsList.isEmpty()) {
                        showEmptyState()
                    } else {
                        showReports()
                    }
                    
                    reportsAdapter.updateReports(reportsList)
                } else {
                    Toast.makeText(
                        this@ViewDailyReportsActivity,
                        "Failed to load reports: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState()
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
                loadingDialog.dismiss()
            } catch (e: Exception) {
                binding.swipeRefreshLayout.isRefreshing = false
                loadingDialog.dismiss()
                Toast.makeText(
                    this@ViewDailyReportsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState()
            }
        }
    }
    
    private fun updateStats() {
        val total = reportsList.size
        binding.tvTotalReports.text = total.toString()
        
        // Calculate today's reports
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val todayCount = reportsList.count { it.reportDate == today }
        binding.tvTodayReports.text = todayCount.toString()
        
        // Calculate this week's reports
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val weekCount = reportsList.count { it.reportDate >= weekStart }
        binding.tvWeekReports.text = weekCount.toString()
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showReports() {
        emptyStateLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun openEditCustomerDetail(
        report: HourlyReportResponse,
        detail: HourlyReportDetail,
        detailIndex: Int
    ) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.Theme_STR)
        val view = LayoutInflater.from(bottomSheetDialog.context).inflate(R.layout.bottom_sheet_edit_customer_detail, null)
        bottomSheetDialog.setContentView(view)

        // Initialize views
        val etCustomerName = view.findViewById<TextInputEditText>(R.id.etCustomerName)
        val etMobileNumber = view.findViewById<TextInputEditText>(R.id.etMobileNumber)
        val etPlotNumber = view.findViewById<TextInputEditText>(R.id.etPlotNumber)
        val rgCustomerResponse = view.findViewById<RadioGroup>(R.id.rgCustomerResponse)
        val layoutReasonNotInterested = view.findViewById<TextInputLayout>(R.id.layoutReasonNotInterested)
        val etReasonNotInterested = view.findViewById<TextInputEditText>(R.id.etReasonNotInterested)
        val etOtherReason = view.findViewById<TextInputEditText>(R.id.etOtherReason)
        val cbSiteVisit = view.findViewById<CheckBox>(R.id.cbSiteVisit)
        val cbMeeting = view.findViewById<CheckBox>(R.id.cbMeeting)
        val cbBooking = view.findViewById<CheckBox>(R.id.cbBooking)
        val etNextFollowup = view.findViewById<TextInputEditText>(R.id.etNextFollowupDate)
        val etArea = view.findViewById<TextInputEditText>(R.id.etArea)
        val etRate = view.findViewById<TextInputEditText>(R.id.etRate)
        val etTotalValue = view.findViewById<TextInputEditText>(R.id.etTotalValue)
        val etTcm = view.findViewById<TextInputEditText>(R.id.etTcm)
        val etValuePerSqft = view.findViewById<TextInputEditText>(R.id.etValuePerSqft)
        val etFeedback = view.findViewById<TextInputEditText>(R.id.etFeedback)
        val layoutExtendedFields = view.findViewById<LinearLayout>(R.id.layoutExtendedFields)
        val layoutBookingFields = view.findViewById<LinearLayout>(R.id.layoutBookingFields)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val ivClose = view.findViewById<ImageView>(R.id.ivClose)

        // Get work type ID
        val workTypeId = detail.workType

        // Show/hide fields based on work type (matching DailyReportActivity logic)
        when (workTypeId) {
            1 -> { // Site Visit - Extended fields without booking
                layoutExtendedFields.visibility = View.VISIBLE
                layoutBookingFields.visibility = View.GONE
            }
            2 -> { // Booking Closed - All fields
                layoutExtendedFields.visibility = View.VISIBLE
                layoutBookingFields.visibility = View.VISIBLE
            }
            else -> { // Other work types - Basic only
                layoutExtendedFields.visibility = View.GONE
                layoutBookingFields.visibility = View.GONE
            }
        }

        // Pre-fill data
        etCustomerName.setText(detail.customerName)
        etMobileNumber.setText(detail.mobileNumber)
        etPlotNumber.setText(detail.plotNumber ?: "")
        etFeedback.setText(detail.feedback ?: "")

        // Customer response
        when (detail.customerResponse) {
            "interested" -> rgCustomerResponse.check(R.id.rbInterested)
            "not_interested" -> {
                rgCustomerResponse.check(R.id.rbNotInterested)
                layoutReasonNotInterested.visibility = View.VISIBLE
            }
        }

        // Reason not interested and other reason
        etReasonNotInterested.setText(detail.reasonNotInterested ?: "")
        etOtherReason.setText(detail.otherReason ?: "")

        // Checkboxes
        cbSiteVisit.isChecked = detail.siteVisitDone
        cbMeeting.isChecked = detail.meetingDone
        cbBooking.isChecked = detail.bookingDone

        // Extended fields
        etNextFollowup.setText(detail.nextFollowupDate ?: "")
        etArea.setText(detail.area ?: "")
        etRate.setText(detail.rate ?: "")
        etTotalValue.setText(detail.totalValue ?: "")
        etTcm.setText(detail.tcm ?: "")
        etValuePerSqft.setText(detail.valuePerSqft ?: "")

        // Customer response change listener
        rgCustomerResponse.setOnCheckedChangeListener { _, checkedId ->
            layoutReasonNotInterested.visibility = if (checkedId == R.id.rbNotInterested) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Next followup date picker
        etNextFollowup.setOnClickListener {
            showDatePicker { selectedDate ->
                etNextFollowup.setText(selectedDate)
            }
        }

        // Close button
        ivClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Save button
        btnSave.setOnClickListener {
            saveCustomerDetailChanges(
                report, detail, detailIndex,
                etCustomerName, etMobileNumber, etPlotNumber,
                rgCustomerResponse, etReasonNotInterested, etOtherReason,
                cbSiteVisit, cbMeeting, cbBooking,
                etNextFollowup, etArea, etRate, etTotalValue, etTcm, etValuePerSqft,
                etFeedback, bottomSheetDialog, workTypeId
            )
        }

        bottomSheetDialog.show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = Calendar.getInstance()
                date.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(dateFormat.format(date.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveCustomerDetailChanges(
        report: HourlyReportResponse,
        originalDetail: HourlyReportDetail,
        detailIndex: Int,
        etCustomerName: TextInputEditText,
        etMobileNumber: TextInputEditText,
        etPlotNumber: TextInputEditText,
        rgCustomerResponse: RadioGroup,
        etReasonNotInterested: TextInputEditText,
        etOtherReason: TextInputEditText,
        cbSiteVisit: CheckBox,
        cbMeeting: CheckBox,
        cbBooking: CheckBox,
        etNextFollowup: TextInputEditText,
        etArea: TextInputEditText,
        etRate: TextInputEditText,
        etTotalValue: TextInputEditText,
        etTcm: TextInputEditText,
        etValuePerSqft: TextInputEditText,
        etFeedback: TextInputEditText,
        dialog: BottomSheetDialog,
        workTypeId: Int
    ) {
        // Validate required fields
        val customerName = etCustomerName.text.toString().trim()
        val mobileNumber = etMobileNumber.text.toString().trim()

        if (customerName.isEmpty()) {
            Toast.makeText(this, "Customer name is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (mobileNumber.isEmpty()) {
            Toast.makeText(this, "Mobile number is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Get customer response
        val customerResponse = when (rgCustomerResponse.checkedRadioButtonId) {
            R.id.rbInterested -> "interested"
            R.id.rbNotInterested -> "not_interested"
            else -> null
        }

        // Get reason not interested and other reason
        val reasonNotInterested = if (customerResponse == "not_interested") {
            etReasonNotInterested.text.toString().trim().ifEmpty { null }
        } else null

        val otherReason = etOtherReason.text.toString().trim().ifEmpty { null }

        // Build updated detail based on work type
        val updatedDetail = UpdateHourlyReportDetail(
            workType = originalDetail.workType,
            project = originalDetail.project,
            customerName = customerName,
            mobileNumber = mobileNumber,
            plotNumber = if (workTypeId == 1 || workTypeId == 2) etPlotNumber.text.toString().trim().ifEmpty { null } else null,
            customerResponse = if (workTypeId == 1 || workTypeId == 2) customerResponse else null,
            reasonNotInterested = if (workTypeId == 1 || workTypeId == 2) reasonNotInterested else null,
            otherReason = if (workTypeId == 1 || workTypeId == 2) otherReason else null,
            siteVisitDone = if (workTypeId == 1 || workTypeId == 2) cbSiteVisit.isChecked else false,
            meetingDone = if (workTypeId == 1 || workTypeId == 2) cbMeeting.isChecked else false,
            bookingDone = if (workTypeId == 1 || workTypeId == 2) cbBooking.isChecked else false,
            nextFollowupDate = if (workTypeId == 1 || workTypeId == 2) etNextFollowup.text.toString().trim().ifEmpty { null } else null,
            area = if (workTypeId == 2) etArea.text.toString().trim().ifEmpty { null } else null,
            rate = if (workTypeId == 2) etRate.text.toString().trim().ifEmpty { null } else null,
            totalValue = if (workTypeId == 2) etTotalValue.text.toString().trim().ifEmpty { null } else null,
            tcm = if (workTypeId == 2) etTcm.text.toString().trim().ifEmpty { null } else null,
            valuePerSqft = if (workTypeId == 2) etValuePerSqft.text.toString().trim().ifEmpty { null } else null,
            feedback = etFeedback.text.toString().trim().ifEmpty { null }
        )

        // Build complete details list with updated detail at index
        val updateDetails = report.details.mapIndexed { index, detail ->
            if (index == detailIndex) {
                updatedDetail
            } else {
                // Keep other details unchanged
                UpdateHourlyReportDetail(
                    workType = detail.workType,
                    project = detail.project,
                    customerName = detail.customerName,
                    mobileNumber = detail.mobileNumber,
                    plotNumber = detail.plotNumber,
                    customerResponse = detail.customerResponse,
                    reasonNotInterested = detail.reasonNotInterested,
                    otherReason = detail.otherReason,
                    siteVisitDone = detail.siteVisitDone,
                    meetingDone = detail.meetingDone,
                    bookingDone = detail.bookingDone,
                    nextFollowupDate = detail.nextFollowupDate,
                    area = detail.area,
                    rate = detail.rate,
                    totalValue = detail.totalValue,
                    tcm = detail.tcm,
                    valuePerSqft = detail.valuePerSqft,
                    feedback = detail.feedback
                )
            }
        }

        // Build update request with unchanged report header
        val updateRequest = UpdateHourlyReportRequest(
            reportDate = report.reportDate,
            reportHour = report.reportHour,
            locationLatitude = report.locationLatitude.toDoubleOrNull() ?: 0.0,
            locationLongitude = report.locationLongitude.toDoubleOrNull() ?: 0.0,
            workDone = report.workDone,
            reasonNotDone = report.reasonNotDone,
            workTypes = report.workTypes.map { it.id },
            details = updateDetails
        )

        // Make API call
        lifecycleScope.launch {
            try {
                loadingDialog.show("Updating customer detail...")
                
                val response = dailyReportApiService.updateHourlyReport(report.id, updateRequest)
                
                if (response.isSuccessful) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@ViewDailyReportsActivity,
                        "Customer detail updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    fetchReports() // Refresh the list
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@ViewDailyReportsActivity,
                        "Failed to update: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(
                    this@ViewDailyReportsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh reports when returning to this activity
        fetchReports()
        bottomNavigation.selectedItemId = R.id.nav_daily_report
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.destroy()
    }
}
