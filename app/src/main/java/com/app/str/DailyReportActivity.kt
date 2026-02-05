package com.app.str

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.str.adapter.WorkTypeAdapter
import com.app.str.utils.EdgeToEdgeHelper
import com.app.str.data.api.DailyReportApiService
import com.app.str.data.api.WorkType
import com.app.str.data.model.DailyReportRequest
import com.app.str.data.model.WorkDetail
import com.app.str.databinding.ActivityDailyReportBinding
import com.app.str.ui.base.BaseActivity
import com.app.str.utils.AutoSubmitReportManager
import com.app.str.utils.LoadingDialog
import com.app.str.utils.LocationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DailyReportActivity : BaseActivity() {
    
    private lateinit var binding: ActivityDailyReportBinding
    
    @Inject
    lateinit var dailyReportApiService: DailyReportApiService
    
    private lateinit var locationHelper: LocationHelper
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var bottomNavigation: BottomNavigationView
    
    private var currentStep = 1
    private val totalSteps = 3
    
    // Form data
    private var workDone: String = ""
    private var reasonNotDone: String = ""
    private var reportDate: String = ""
    private var reportHour: Int = 0
    private var locationLatitude: Double? = null
    private var locationLongitude: Double? = null
    
    // Work types
    private var workTypesList = mutableListOf<WorkType>()
    private var selectedWorkType: WorkType? = null
    
    // Customer details
    private var projectMap = mutableMapOf<String, Int>()
    
    // All work detail entries collected
    private val collectedWorkDetails = mutableListOf<WorkDetail>()
    
    // Current entry data
    private var currentCustomerName: String = ""
    private var currentMobileNumber: String = ""
    private var currentPlotNumber: String = ""
    private var currentProjectId: Int? = null
    private var currentCustomerResponse: String = ""
    private var currentReasonNotInterested: String = ""
    private var currentOtherReason: String = ""
    private var currentSiteVisitDone: Boolean = false
    private var currentMeetingDone: Boolean = false
    private var currentBookingDone: Boolean = false
    private var currentNextFollowupDate: String = ""
    private var currentArea: Double? = null
    private var currentTotalValue: Long? = null
    private var currentTcm: Int? = null
    private var currentValuePerSqft: Int? = null
    private var currentFeedback: String = ""
    
    // Work Type Adapter
    private lateinit var workTypeAdapter: WorkTypeAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityDailyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize LoadingDialog
        loadingDialog = LoadingDialog(this)
        
        // Setup edge-to-edge with proper bottom navigation handling
        setupEdgeToEdgeWithBottomNav()
        
        try {
            locationHelper = LocationHelper(this)
            setupUI()
            setupBottomNavigation()
            initializeCurrentDateTime()
            getCurrentLocation()
            fetchWorkTypesAndProjects()
            showStep(currentStep)
            
            // Check authentication before allowing report submission
            checkAuthentication()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupUI() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            handleBackNavigation()
        }
        
        // Setup next button
        binding.btnNext.setOnClickListener {
            handleNextClick()
        }
        
        // Setup add more button
        binding.btnAddMore.setOnClickListener {
            handleAddMore()
        }
        
        // Setup work done card selection
        binding.cardYes.setOnClickListener {
            selectWorkDoneOption(true)
        }
        
        binding.cardNo.setOnClickListener {
            selectWorkDoneOption(false)
        }
        
        // Setup customer response cards
        binding.cardInterested.setOnClickListener {
            selectCustomerResponse("interested")
        }
        
        binding.cardNotInterested.setOnClickListener {
            selectCustomerResponse("not_interested")
        }
        
        binding.cardNotSure.setOnClickListener {
            selectCustomerResponse("not_sure")
        }
        
        // Setup date picker
        binding.etNextFollowupDate.setOnClickListener {
            showDatePicker()
        }
        
        // Setup auto-calculation for booking fields
        setupBookingCalculations()
    }
    
    private fun setupBookingCalculations() {
        // Add text watchers for auto-calculation
        binding.etArea.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateTotalValue()
            }
        })
        
        binding.etValuePerSqft.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateTotalValue()
            }
        })
        
        binding.etTotalValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // If user manually enters total value, calculate value per sqft
                if (binding.etTotalValue.isFocused) {
                    calculateValuePerSqft()
                }
            }
        })
    }
    
    private fun calculateTotalValue() {
        if (binding.etArea.isFocused || binding.etValuePerSqft.isFocused) {
            val areaText = binding.etArea.text.toString()
            val valuePerSqftText = binding.etValuePerSqft.text.toString()
            
            if (areaText.isNotEmpty() && valuePerSqftText.isNotEmpty()) {
                val area = areaText.toDoubleOrNull()
                val valuePerSqft = valuePerSqftText.toIntOrNull()
                
                if (area != null && valuePerSqft != null) {
                    val totalValue = (area * valuePerSqft).toLong()
                    binding.etTotalValue.setText(totalValue.toString())
                }
            }
        }
    }
    
    private fun calculateValuePerSqft() {
        val areaText = binding.etArea.text.toString()
        val totalValueText = binding.etTotalValue.text.toString()
        
        if (areaText.isNotEmpty() && totalValueText.isNotEmpty()) {
            val area = areaText.toDoubleOrNull()
            val totalValue = totalValueText.toLongOrNull()
            
            if (area != null && totalValue != null && area > 0) {
                val valuePerSqft = (totalValue / area).toInt()
                binding.etValuePerSqft.setText(valuePerSqft.toString())
            }
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = binding.bottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_daily_report
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToActivity(DashboardActivity::class.java)
                    true
                }
                R.id.nav_work_plan -> {
                    navigateToActivity(WorkPlansActivity::class.java)
                    true
                }
                R.id.nav_daily_report -> {
                    // Already on Daily Report
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
     * Setup edge-to-edge display with proper handling for bottom navigation bar
     * and the bottom action bar (Next/Submit button).
     */
    private fun setupEdgeToEdgeWithBottomNav() {
        // Use EdgeToEdgeHelper with action bar support for screens with fixed bottom buttons
        EdgeToEdgeHelper.setupEdgeToEdgeWithActionBar(
            rootView = binding.root,
            bottomNav = binding.bottomNavigation,
            bottomActionBar = binding.bottomActionBar,
            contentView = binding.contentScrollView,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
    }
    
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
    
    private fun setupWorkTypeRecyclerView() {
        if (workTypesList.isEmpty()) {
            Toast.makeText(this, "No work types available", Toast.LENGTH_SHORT).show()
            return
        }
        
        workTypeAdapter = WorkTypeAdapter(workTypesList) { selectedWorkType, position ->
            this.selectedWorkType = selectedWorkType
        }
        
        binding.recyclerViewWorkTypes.apply {
            layoutManager = LinearLayoutManager(this@DailyReportActivity)
            adapter = workTypeAdapter
        }
    }
    
    private fun initializeCurrentDateTime() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        reportDate = dateFormat.format(calendar.time)
        reportHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        binding.tvCurrentDateTime.text = "Report for: ${SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(calendar.time)}"
    }
    
    private fun getCurrentLocation() {
        if (!locationHelper.hasLocationPermissions()) {
            locationHelper.requestLocationPermissions(this)
            return
        }
        
        lifecycleScope.launch {
            try {
                loadingDialog.show("Getting your location...")
                val location = locationHelper.getCurrentLocation()
                locationLatitude = location.latitude
                locationLongitude = location.longitude
                
                // Get address from coordinates
                val address = locationHelper.getAddressFromLocation(location.latitude, location.longitude)
                binding.tvLocationStatus.text = "Location: $address"
                loadingDialog.dismiss()
            } catch (e: Exception) {
                loadingDialog.dismiss()
                binding.tvLocationStatus.text = "Location: Failed to capture - ${e.message}"
                Toast.makeText(this@DailyReportActivity, "Location capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun fetchWorkTypesAndProjects() {
        lifecycleScope.launch {
            try {
                loadingDialog.show("Loading data...")
                
                // Fetch Work Types
                val workTypesResponse = dailyReportApiService.getWorkTypes()
                if (workTypesResponse.isSuccessful && workTypesResponse.body() != null) {
                    workTypesList.clear()
                    workTypesList.addAll(workTypesResponse.body() ?: emptyList())
                    setupWorkTypeRecyclerView()
                } else {
                    Toast.makeText(
                        this@DailyReportActivity,
                        "Failed to load work types: ${workTypesResponse.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Fetch Projects
                val projectsResponse = dailyReportApiService.getProjects()
                if (projectsResponse.isSuccessful && projectsResponse.body() != null) {
                    val projects = projectsResponse.body() ?: emptyList()
                    projectMap.clear()
                    projects.forEach { project ->
                        projectMap[project.name] = project.id
                    }
                    setupProjectDropdown()
                } else {
                    Toast.makeText(
                        this@DailyReportActivity,
                        "Failed to load projects: ${projectsResponse.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    setupProjectDropdown()
                }
                
                loadingDialog.dismiss()
                
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(
                    this@DailyReportActivity,
                    "Error loading data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                
                setupWorkTypeRecyclerView()
                setupProjectDropdown()
            }
        }
    }
    
    private fun setupProjectDropdown() {
        val projectNames = projectMap.keys.toList()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            projectNames
        )
        
        binding.spinnerProjectName.setAdapter(adapter)
        binding.spinnerProjectName.setOnItemClickListener { _, _, position, _ ->
            val projectName = projectNames[position]
            currentProjectId = projectMap[projectName]
        }
    }
    
    private fun showStep(step: Int) {
        // Hide all steps
        binding.layoutStep1.visibility = android.view.View.GONE
        binding.layoutStep2.visibility = android.view.View.GONE
        binding.layoutStep3.visibility = android.view.View.GONE
        
        // Update progress
        binding.progressBar.progress = (step * 100) / totalSteps
        binding.tvProgress.text = "Step $step of $totalSteps"
        
        // Show current step
        when (step) {
            1 -> {
                binding.layoutStep1.visibility = android.view.View.VISIBLE
                binding.tvStepTitle.text = "Work Status"
                binding.btnNext.isEnabled = true
                binding.btnAddMore.visibility = android.view.View.GONE
                updateNextButtonText()
            }
            2 -> {
                binding.layoutStep2.visibility = android.view.View.VISIBLE
                binding.tvStepTitle.text = "Work Type"
                binding.btnNext.text = "Next"
                binding.btnNext.isEnabled = workTypesList.isNotEmpty()
                binding.btnAddMore.visibility = android.view.View.GONE
                
                if (workTypesList.isEmpty()) {
                    Toast.makeText(
                        this, 
                        "No work types available. Cannot proceed with report.", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            3 -> {
                binding.layoutStep3.visibility = android.view.View.VISIBLE
                binding.tvStepTitle.text = "Customer Details"
                binding.btnNext.text = "Submit Report"
                binding.btnAddMore.visibility = android.view.View.VISIBLE
                binding.btnNext.isEnabled = true
                
                // Show/hide fields based on work type
                showFormFieldsBasedOnWorkType()
            }
        }
    }
    
    private fun showFormFieldsBasedOnWorkType() {
        val workTypeId = selectedWorkType?.id ?: 0
        
        when (workTypeId) {
            1 -> { // Site Visit - Full form without booking fields
                binding.layoutExtendedFields.visibility = android.view.View.VISIBLE
                binding.layoutBookingFields.visibility = android.view.View.GONE
            }
            2 -> { // Booking Closed - Full form with booking fields
                binding.layoutExtendedFields.visibility = android.view.View.VISIBLE
                binding.layoutBookingFields.visibility = android.view.View.VISIBLE
            }
            else -> { // Other work types - Basic form only
                binding.layoutExtendedFields.visibility = android.view.View.GONE
                binding.layoutBookingFields.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun handleNextClick() {
        when (currentStep) {
            1 -> {
                if (validateStep1()) {
                    if (workDone == "no") {
                        // Skip to submit if work not done
                        submitReport()
                    } else {
                        currentStep++
                        showStep(currentStep)
                    }
                }
            }
            2 -> {
                if (validateStep2()) {
                    currentStep++
                    showStep(currentStep)
                }
            }
            3 -> {
                if (validateStep3()) {
                    // Collect current entry
                    collectCurrentEntry()
                    // Submit all collected entries
                    submitReport()
                }
            }
        }
    }
    
    private fun handleAddMore() {
        if (validateStep3()) {
            // Collect current entry
            collectCurrentEntry()
            
            Toast.makeText(this, "Entry added. Add another work type entry.", Toast.LENGTH_SHORT).show()
            
            // Reset form and go back to step 1
            resetFormFields()
            currentStep = 1
            showStep(currentStep)
        }
    }
    
    private fun collectCurrentEntry() {
        val workTypeId = selectedWorkType?.id ?: return
        
        val workDetail = WorkDetail(
            workType = workTypeId,
            project = currentProjectId,
            customerName = currentCustomerName.ifEmpty { null },
            mobileNumber = currentMobileNumber.ifEmpty { null },
            plotNumber = if (workTypeId == 1 || workTypeId == 2) currentPlotNumber.ifEmpty { null } else null,
            customerResponse = if (workTypeId == 1 || workTypeId == 2) currentCustomerResponse.ifEmpty { null } else null,
            reasonNotInterested = if (workTypeId == 1 || workTypeId == 2) currentReasonNotInterested.ifEmpty { null } else null,
            otherReason = if (workTypeId == 1 || workTypeId == 2) currentOtherReason.ifEmpty { null } else null,
            siteVisitDone = if (workTypeId == 1 || workTypeId == 2) currentSiteVisitDone else null,
            meetingDone = if (workTypeId == 1 || workTypeId == 2) currentMeetingDone else null,
            bookingDone = if (workTypeId == 1 || workTypeId == 2) currentBookingDone else null,
            nextFollowupDate = if (workTypeId == 1 || workTypeId == 2) currentNextFollowupDate.ifEmpty { null } else null,
            area = if (workTypeId == 2) currentArea else null,
            rate = null,
            totalValue = if (workTypeId == 2) currentTotalValue else null,
            tcm = if (workTypeId == 2) currentTcm?.toString() else null,
            valuePerSqft = if (workTypeId == 2) currentValuePerSqft else null,
            feedback = currentFeedback.ifEmpty { null }
        )
        
        collectedWorkDetails.add(workDetail)
    }
    
    private fun resetFormFields() {
        // Reset all form fields
        binding.etCustomerName.text?.clear()
        binding.etMobileNumber.text?.clear()
        binding.spinnerProjectName.text?.clear()
        binding.etPlotNumber.text?.clear()
        binding.etReasonNotInterested.text?.clear()
        binding.etOtherReason.text?.clear()
        binding.checkBoxSiteVisit.isChecked = false
        binding.checkBoxMeeting.isChecked = false
        binding.checkBoxBooking.isChecked = false
        binding.etNextFollowupDate.text?.clear()
        binding.etArea.text?.clear()
        binding.etTotalValue.text?.clear()
        binding.etTcm.text?.clear()
        binding.etValuePerSqft.text?.clear()
        binding.etFeedback.text?.clear()
        
        // Reset customer response selection
        resetCustomerResponseCards()
        
        // Reset current entry variables
        currentCustomerName = ""
        currentMobileNumber = ""
        currentPlotNumber = ""
        currentProjectId = null
        currentCustomerResponse = ""
        currentReasonNotInterested = ""
        currentOtherReason = ""
        currentSiteVisitDone = false
        currentMeetingDone = false
        currentBookingDone = false
        currentNextFollowupDate = ""
        currentArea = null
        currentTotalValue = null
        currentTcm = null
        currentValuePerSqft = null
        currentFeedback = ""
        
        // Reset work type selection
        workTypeAdapter.setSelectedPosition(-1)
        selectedWorkType = null
    }
    
    private fun handleBackNavigation() {
        if (currentStep > 1) {
            currentStep--
            showStep(currentStep)
        } else {
            finish()
        }
    }
    
    private fun validateStep1(): Boolean {
        if (workDone.isEmpty()) {
            Toast.makeText(this, "Please select whether work was done or not", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (workDone == "no") {
            reasonNotDone = binding.etReasonNotDone.text.toString().trim()
            if (reasonNotDone.isEmpty()) {
                binding.etReasonNotDone.error = "Please provide reason for not completing work"
                return false
            }
        }
        
        return true
    }
    
    private fun validateStep2(): Boolean {
        if (selectedWorkType == null) {
            Toast.makeText(this, "Please select a work type", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun validateStep3(): Boolean {
        currentCustomerName = binding.etCustomerName.text.toString().trim()
        currentMobileNumber = binding.etMobileNumber.text.toString().trim()
        currentFeedback = binding.etFeedback.text.toString().trim()
        
        val workTypeId = selectedWorkType?.id ?: 0
        
        // Validate mobile number format if provided
        if (currentMobileNumber.isNotEmpty() && (currentMobileNumber.length < 10 || currentMobileNumber.length > 15)) {
            binding.etMobileNumber.error = "Please enter a valid mobile number"
            return false
        }
        
        // Extended validation for Site Visit and Booking Closed
        if (workTypeId == 1 || workTypeId == 2) {
            currentPlotNumber = binding.etPlotNumber.text.toString().trim()
            currentReasonNotInterested = binding.etReasonNotInterested.text.toString().trim()
            currentOtherReason = binding.etOtherReason.text.toString().trim()
            currentSiteVisitDone = binding.checkBoxSiteVisit.isChecked
            currentMeetingDone = binding.checkBoxMeeting.isChecked
            currentBookingDone = binding.checkBoxBooking.isChecked
            currentNextFollowupDate = binding.etNextFollowupDate.text.toString().trim()
            
            if (currentCustomerResponse.isEmpty()) {
                Toast.makeText(this, "Please select customer response", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (currentCustomerResponse == "not_interested" && currentReasonNotInterested.isEmpty()) {
                binding.etReasonNotInterested.error = "Please provide reason for not being interested"
                return false
            }
        }
        
        // Additional validation for Booking Closed
        if (workTypeId == 2) {
            val areaText = binding.etArea.text.toString().trim()
            val totalValueText = binding.etTotalValue.text.toString().trim()
            val tcmText = binding.etTcm.text.toString().trim()
            val valuePerSqftText = binding.etValuePerSqft.text.toString().trim()
            
            currentArea = areaText.toDoubleOrNull()
            currentTotalValue = totalValueText.toLongOrNull()
            currentTcm = tcmText.toIntOrNull()
            currentValuePerSqft = valuePerSqftText.toIntOrNull()
        }
        
        return true
    }
    
    private fun selectWorkDoneOption(isYes: Boolean) {
        if (isYes) {
            workDone = "yes"
            binding.cardYes.strokeColor = resources.getColor(R.color.primary_burgundy, null)
            binding.cardYes.strokeWidth = 4
            binding.checkYes.visibility = android.view.View.VISIBLE

            binding.cardNo.strokeColor = resources.getColor(R.color.text_secondary, null)
            binding.cardNo.strokeWidth = 2
            binding.checkNo.visibility = android.view.View.GONE
            
            binding.layoutReasonNotDone.visibility = android.view.View.GONE
            binding.etReasonNotDone.text?.clear()
        } else {
            workDone = "no"
            binding.cardNo.strokeColor = resources.getColor(R.color.primary_burgundy, null)
            binding.cardNo.strokeWidth = 4
            binding.checkNo.visibility = android.view.View.VISIBLE

            binding.cardYes.strokeColor = resources.getColor(R.color.text_secondary, null)
            binding.cardYes.strokeWidth = 2
            binding.checkYes.visibility = android.view.View.GONE
            
            binding.layoutReasonNotDone.visibility = android.view.View.VISIBLE
        }
        
        updateNextButtonText()
    }
    
    private fun selectCustomerResponse(response: String) {
        currentCustomerResponse = response
        
        resetCustomerResponseCards()
        
        when (response) {
            "interested" -> {
                binding.cardInterested.strokeColor =
                    resources.getColor(R.color.primary_burgundy, null)
                binding.cardInterested.strokeWidth = 4
                binding.checkInterested.visibility = android.view.View.VISIBLE
                binding.layoutReasonNotInterested.visibility = android.view.View.GONE
                binding.etReasonNotInterested.text?.clear()
            }
            "not_interested" -> {
                binding.cardNotInterested.strokeColor =
                    resources.getColor(R.color.primary_burgundy, null)
                binding.cardNotInterested.strokeWidth = 4
                binding.checkNotInterested.visibility = android.view.View.VISIBLE
                binding.layoutReasonNotInterested.visibility = android.view.View.VISIBLE
            }
            "not_sure" -> {
                binding.cardNotSure.strokeColor = resources.getColor(R.color.primary_burgundy, null)
                binding.cardNotSure.strokeWidth = 4
                binding.checkNotSure.visibility = android.view.View.VISIBLE
                binding.layoutReasonNotInterested.visibility = android.view.View.GONE
                binding.etReasonNotInterested.text?.clear()
            }
        }
    }
    
    private fun resetCustomerResponseCards() {
        binding.cardInterested.strokeColor = resources.getColor(R.color.text_secondary, null)
        binding.cardInterested.strokeWidth = 2
        binding.cardNotInterested.strokeColor = resources.getColor(R.color.text_secondary, null)
        binding.cardNotInterested.strokeWidth = 2
        binding.cardNotSure.strokeColor = resources.getColor(R.color.text_secondary, null)
        binding.cardNotSure.strokeWidth = 2
        
        binding.checkInterested.visibility = android.view.View.GONE
        binding.checkNotInterested.visibility = android.view.View.GONE
        binding.checkNotSure.visibility = android.view.View.GONE
    }
    
    private fun updateNextButtonText() {
        if (currentStep == 1) {
            if (workDone == "no") {
                binding.btnNext.text = "Submit Report"
            } else {
                binding.btnNext.text = "Next"
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                currentNextFollowupDate = dateFormat.format(selectedDate.time)
                binding.etNextFollowupDate.setText(currentNextFollowupDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    private fun submitReport() {
        lifecycleScope.launch {
            try {
                if (!authManager.hasStoredTokens()) {
                    Toast.makeText(this@DailyReportActivity, "Please login to submit report", Toast.LENGTH_LONG).show()
                    performLogout()
                    return@launch
                }
                
                loadingDialog.show("Submitting your report...")
                
                // Fix location coordinates to max 6 decimal places
                val formattedLatitude = locationLatitude?.let { 
                    String.format("%.6f", it) 
                }
                val formattedLongitude = locationLongitude?.let { 
                    String.format("%.6f", it) 
                }
                
                // Get unique work type IDs from collected work details
                val workTypeIds = collectedWorkDetails.mapNotNull { it.workType }.distinct()
                
                val request = DailyReportRequest(
                    reportDate = reportDate,
                    reportHour = reportHour,
                    locationLatitude = formattedLatitude,
                    locationLongitude = formattedLongitude,
                    workDone = workDone,
                    reasonNotDone = reasonNotDone.ifEmpty { null },
                    workTypes = if (workDone == "no") null else workTypeIds,
                    details = if (workDone == "no") emptyList() else collectedWorkDetails
                )
                
                val response = dailyReportApiService.submitDailyReport(request)
                
                if (response.isSuccessful) {
                    loadingDialog.updateMessage("Report submitted successfully!")
                    Toast.makeText(this@DailyReportActivity, "Daily report submitted successfully!", Toast.LENGTH_LONG).show()
                    
                    // Clear any pending auto-submit since user manually submitted
                    AutoSubmitReportManager.clearPendingReport(this@DailyReportActivity)
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        // Navigate to Dashboard
                        navigateToActivity(DashboardActivity::class.java)
                        finish()
                    }, 500)
                } else {
                    loadingDialog.dismiss()
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@DailyReportActivity, "Failed to submit report: $errorBody", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(this@DailyReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        handleBackNavigation()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LocationHelper.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                    getCurrentLocation()
                } else {
                    binding.tvLocationStatus.text = "Location: Permission denied"
                    Toast.makeText(this, "Location permissions are required for accurate reporting", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun checkAuthentication() {
        lifecycleScope.launch {
            val hasTokens = authManager.hasStoredTokens()
            if (!hasTokens) {
                Toast.makeText(this@DailyReportActivity, "Please login to continue", Toast.LENGTH_LONG).show()
                performLogout()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.destroy()
    }
    
    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_daily_report
    }
}
