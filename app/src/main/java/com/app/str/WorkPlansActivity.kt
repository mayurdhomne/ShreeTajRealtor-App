package com.app.str

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.str.adapter.WorkPlanAdapter
import com.app.str.data.model.AvailableWorkTitle
import com.app.str.data.model.CreateWorkPlanRequest
import com.app.str.data.model.UpdateWorkPlanRequest
import com.app.str.data.model.WorkPlanItem
import com.app.str.databinding.ActivityWorkPlansBinding
import com.app.str.databinding.BottomSheetCreateWorkPlanBinding
import com.app.str.databinding.DialogEditWorkPlanBinding
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.CreatePlanState
import com.app.str.viewmodel.DeletePlanState
import com.app.str.viewmodel.UpdatePlanState
import com.app.str.viewmodel.WorkPlanState
import com.app.str.viewmodel.WorkPlanViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.app.str.utils.EdgeToEdgeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class WorkPlansActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWorkPlansBinding
    private val viewModel: WorkPlanViewModel by viewModels()
    
    @Inject
    lateinit var authManager: com.app.str.utils.AuthManager
    
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var allPlansAdapter: WorkPlanAdapter
    private lateinit var yourPlansAdapter: WorkPlanAdapter
    private lateinit var allottedPlansAdapter: WorkPlanAdapter
    
    private var currentTab = 0
    private var currentFilter: String? = null
    private var currentDate: String? = null
    private var selectedWeek: Int? = null
    private var selectedWeekStartDate: String? = null
    private var selectedWeekEndDate: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize LoadingDialog
        loadingDialog = LoadingDialog(this)
        
        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilterIcon()
        setupWeekFilter()
        setupObservers()
        setupClickListeners()
        setupBottomNavigation()
        
        // Check authentication before loading data
        checkAuthenticationAndLoadData()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = binding.bottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_work_plan
        
        // Use setupBottomNavOnly since layout already has fitsSystemWindows for status bar
        EdgeToEdgeHelper.setupBottomNavOnly(
            rootView = binding.root,
            bottomNav = bottomNavigation,
            contentView = binding.contentScrollView,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
        
        // Position FAB above BottomNavigationView dynamically
        bottomNavigation.post {
            val fabMargin = bottomNavigation.height + resources.getDimensionPixelSize(R.dimen.padding_medium)
            (binding.fabCreatePlan.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)?.let { params ->
                params.bottomMargin = fabMargin
                binding.fabCreatePlan.layoutParams = params
            }
        }
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToActivity(DashboardActivity::class.java)
                    true
                }
                R.id.nav_work_plan -> {
                    // Already on Work Plan
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
    
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = android.content.Intent(this, activityClass)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            navigateToActivity(DashboardActivity::class.java)
            finish()
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Your Plans"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Allotted Plans"))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateContentForTab(currentTab)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        // All Plans Adapter
        allPlansAdapter = WorkPlanAdapter(
            onEditClick = { workPlan -> showEditDialog(workPlan) },
            onDeleteClick = { workPlan -> showDeleteConfirmation(workPlan) },
            onStatusClick = { workPlan -> handleStatusClick(workPlan) }
        )
        
        // Your Plans Adapter
        yourPlansAdapter = WorkPlanAdapter(
            onEditClick = { workPlan -> showEditDialog(workPlan) },
            onDeleteClick = { workPlan -> showDeleteConfirmation(workPlan) },
            onStatusClick = { workPlan -> handleStatusClick(workPlan) }
        )
        
        // Allotted Plans Adapter
        allottedPlansAdapter = WorkPlanAdapter(
            onEditClick = { workPlan -> showEditDialog(workPlan) },
            onDeleteClick = { workPlan -> showDeleteConfirmation(workPlan) },
            onStatusClick = { workPlan -> handleStatusClick(workPlan) }
        )
        
        binding.recyclerViewWorkDetails.apply {
            layoutManager = LinearLayoutManager(this@WorkPlansActivity)
            adapter = allPlansAdapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadWorkPlans(currentFilter, currentDate)
        }
    }
    
    private fun setupFilterIcon() {
        binding.ivFilter.setOnClickListener {
            showFilterDialog()
        }
    }
    
    private fun setupWeekFilter() {
        // Initially hidden
        binding.weekFilterLayout.visibility = View.GONE
        
        binding.weekFilterDropdown.setOnClickListener {
            if (currentFilter == "weekly") {
                showWeekSelectionDialog()
            }
        }
    }
    
    private fun getWeeksInCurrentMonth(): List<Pair<String, Pair<String, String>>> {
        // Returns list of pairs: ("Week 1 (1-7 Jan)", Pair(startDate, endDate))
        val weeks = mutableListOf<Pair<String, Pair<String, String>>>()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Get month name
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Start from the first day of the month
        calendar.set(currentYear, currentMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        var weekNumber = 1
        var dayOfMonth = 1
        
        while (dayOfMonth <= daysInMonth) {
            // Week start date
            calendar.set(currentYear, currentMonth, dayOfMonth)
            val weekStartDay = dayOfMonth
            val weekStartDate = dateFormat.format(calendar.time)
            
            // Calculate week end (either end of week or end of month)
            val daysUntilSunday = Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 1
            var weekEndDay = minOf(dayOfMonth + daysUntilSunday, daysInMonth)
            
            // If it's the first week and starts mid-week, end on Saturday
            if (weekNumber == 1) {
                weekEndDay = minOf(weekEndDay, daysInMonth)
            }
            
            calendar.set(currentYear, currentMonth, weekEndDay)
            val weekEndDate = dateFormat.format(calendar.time)
            
            val monthName = monthFormat.format(calendar.time)
            val weekLabel = "Week $weekNumber ($weekStartDay-$weekEndDay $monthName)"
            
            weeks.add(Pair(weekLabel, Pair(weekStartDate, weekEndDate)))
            
            // Move to next week
            dayOfMonth = weekEndDay + 1
            weekNumber++
        }
        
        return weeks
    }
    
    private fun showWeekSelectionDialog() {
        val weeksData = getWeeksInCurrentMonth()
        val weekOptions = weeksData.map { it.first }.toTypedArray()
        
        val currentSelection = if (selectedWeek != null && selectedWeek!! <= weeksData.size) {
            selectedWeek!! - 1
        } else {
            -1
        }
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Select Week")
            .setSingleChoiceItems(weekOptions, currentSelection) { dialogInterface, which ->
                selectedWeek = which + 1
                val selectedWeekData = weeksData[which]
                binding.weekFilterDropdown.setText(selectedWeekData.first, false)
                
                // Store the week date range for local filtering
                selectedWeekStartDate = selectedWeekData.second.first
                selectedWeekEndDate = selectedWeekData.second.second
                
                // Load data for selected week using the start date
                loadWorkPlansForWeek(selectedWeekData.second.first)
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
        
        dialog.window?.decorView?.post {
            setTextColorRecursive(dialog.window?.decorView, ContextCompat.getColor(this, R.color.text_primary))
        }
    }
    
    private fun loadWorkPlansForWeek(startDate: String) {
        // Use the start date of the selected week for API call
        currentDate = startDate
        
        // Reload data with filter and date
        viewModel.loadWorkPlans(currentFilter, currentDate)
    }
    
    /**
     * Filter plans by selected week date range if weekly filter is active
     */
    private fun filterPlansByWeekIfNeeded(plans: List<WorkPlanItem>): List<WorkPlanItem> {
        // Only filter if weekly filter is active and a week is selected
        if (currentFilter != "weekly" || selectedWeek == null || 
            selectedWeekStartDate == null || selectedWeekEndDate == null) {
            return plans
        }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        return try {
            val startDate = dateFormat.parse(selectedWeekStartDate!!)
            val endDate = dateFormat.parse(selectedWeekEndDate!!)
            
            if (startDate == null || endDate == null) {
                return plans
            }
            
            plans.filter { plan ->
                try {
                    val planDate = dateFormat.parse(plan.date)
                    if (planDate != null) {
                        // Check if plan date is within the selected week range (inclusive)
                        !planDate.before(startDate) && !planDate.after(endDate)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WorkPlansActivity", "Error parsing plan date: ${plan.date}", e)
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkPlansActivity", "Error filtering plans by week", e)
            plans
        }
    }
    
    private fun showFilterDialog() {
        val filterOptions = arrayOf("All Plans", "Daily", "Weekly", "Monthly")
        val filterValues = arrayOf(null, "daily", "weekly", "monthly")
        
        // Determine current selection
        val currentSelection = when (currentFilter) {
            "daily" -> 1
            "weekly" -> 2
            "monthly" -> 3
            else -> 0
        }
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Filter Work Plans")
            .setSingleChoiceItems(filterOptions, currentSelection) { dialogInterface, which ->
                currentFilter = filterValues[which]
                
                // Show/hide week filter based on selection
                if (currentFilter == "weekly") {
                    binding.weekFilterLayout.visibility = View.VISIBLE
                    selectedWeek = null
                    selectedWeekStartDate = null
                    selectedWeekEndDate = null
                    binding.weekFilterDropdown.setText("", false)
                    currentDate = null
                } else {
                    binding.weekFilterLayout.visibility = View.GONE
                    selectedWeek = null
                    selectedWeekStartDate = null
                    selectedWeekEndDate = null
                    
                    // When filter is applied, remove date filter
                    // When "All Plans" is selected, restore current date filter
                    if (currentFilter == null) {
                        currentDate = getCurrentDate()
                    } else {
                        currentDate = null
                    }
                }
                
                // Update title text based on filter
                val titleText = when (currentFilter) {
                    "daily" -> "Daily Work Plans"
                    "weekly" -> "Weekly Work Plans"
                    "monthly" -> "Monthly Work Plans"
                    else -> when (currentTab) {
                        0 -> "All Work Plans"
                        1 -> "Your Work Plans"
                        2 -> "Allotted Work Plans"
                        else -> "Work Details"
                    }
                }
                binding.tvTableTitle.text = titleText
                
                // Only reload data if not weekly (weekly requires week selection)
                if (currentFilter != "weekly") {
                    viewModel.loadWorkPlans(currentFilter, currentDate)
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
        
        // Set text colors for all text views in the dialog - delayed to ensure proper rendering
        dialog.window?.decorView?.post {
            setTextColorRecursive(dialog.window?.decorView, ContextCompat.getColor(this, R.color.text_primary))
        }
    }
    
    private fun setTextColorRecursive(view: View?, color: Int) {
        if (view == null) return
        when (view) {
            is android.widget.CheckedTextView -> {
                view.setTextColor(color)
            }
            is android.widget.TextView -> {
                view.setTextColor(color)
            }
            is android.view.ViewGroup -> {
                for (i in 0 until view.childCount) {
                    setTextColorRecursive(view.getChildAt(i), color)
                }
            }
        }
    }
    
    private fun setupObservers() {
        viewModel.workPlansState.observe(this) { state ->
            when (state) {
                is WorkPlanState.Loading -> {
                    loadingDialog.show("Loading work plans...")
                    binding.progressIndicator.visibility = View.VISIBLE
                }
                is WorkPlanState.Success -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is WorkPlanState.Error -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                }
            }
        }
        
        viewModel.allPlans.observe(this) { plans ->
            val filteredPlans = filterPlansByWeekIfNeeded(plans)
            allPlansAdapter.submitList(filteredPlans)
            updateEmptyState(0, filteredPlans)
        }
        
        viewModel.userPlans.observe(this) { plans ->
            val filteredPlans = filterPlansByWeekIfNeeded(plans)
            yourPlansAdapter.submitList(filteredPlans)
            updateEmptyState(1, filteredPlans)
        }
        
        viewModel.adminPlans.observe(this) { plans ->
            val filteredPlans = filterPlansByWeekIfNeeded(plans)
            allottedPlansAdapter.submitList(filteredPlans)
            updateEmptyState(2, filteredPlans)
        }
        
        // Observe work plan titles
        viewModel.workPlanTitles.observe(this) { titles ->
            // Titles will be used when creating work plan
            android.util.Log.d("WorkPlansActivity", "Work plan titles loaded: ${titles.size}")
        }
        
        // Observe create plan state
        viewModel.createPlanState.observe(this) { state ->
            when (state) {
                is CreatePlanState.Loading -> {
                    loadingDialog.show("Creating work plan...")
                    binding.progressIndicator.visibility = View.VISIBLE
                }
                is CreatePlanState.Success -> {
                    loadingDialog.updateMessage("Work plan created!")
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Work plan created successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        viewModel.resetCreatePlanState()
                    }, 500)
                }
                is CreatePlanState.Error -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetCreatePlanState()
                }
                else -> {}
            }
        }
        
        // Observe delete plan state
        viewModel.deletePlanState.observe(this) { state ->
            when (state) {
                is DeletePlanState.Loading -> {
                    loadingDialog.show("Deleting work plan...")
                    binding.progressIndicator.visibility = View.VISIBLE
                }
                is DeletePlanState.Success -> {
                    loadingDialog.updateMessage("Work plan deleted!")
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Work plan deleted successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        viewModel.resetDeletePlanState()
                    }, 500)
                }
                is DeletePlanState.Error -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDeletePlanState()
                }
                else -> {}
            }
        }
        
        // Observe update plan state
        viewModel.updatePlanState.observe(this) { state ->
            when (state) {
                is UpdatePlanState.Loading -> {
                    loadingDialog.show("Updating work plan...")
                    binding.progressIndicator.visibility = View.VISIBLE
                }
                is UpdatePlanState.Success -> {
                    loadingDialog.updateMessage("Work plan updated!")
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Work plan updated successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        viewModel.resetUpdatePlanState()
                    }, 500)
                }
                is UpdatePlanState.Error -> {
                    loadingDialog.dismiss()
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetUpdatePlanState()
                }
                else -> {}
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnCreateYourPlan.setOnClickListener {
            showCreatePlanDialog()
        }
        
        binding.fabCreatePlan.setOnClickListener {
            showCreatePlanDialog()
        }
    }
    
    private fun updateContentForTab(position: Int) {
        when (position) {
            0 -> { // All
                binding.recyclerViewWorkDetails.adapter = allPlansAdapter
                binding.btnCreateYourPlan.visibility = View.GONE
                binding.fabCreatePlan.visibility = View.GONE
                
                // Update title based on filter
                binding.tvTableTitle.text = when (currentFilter) {
                    "daily" -> "Daily Work Plans"
                    "weekly" -> "Weekly Work Plans"
                    "monthly" -> "Monthly Work Plans"
                    else -> "All Work Plans"
                }
                
                viewModel.allPlans.value?.let { plans ->
                    val filteredPlans = filterPlansByWeekIfNeeded(plans)
                    allPlansAdapter.submitList(filteredPlans)
                    updateEmptyState(0, filteredPlans)
                }
            }
            1 -> { // Your Plans
                binding.recyclerViewWorkDetails.adapter = yourPlansAdapter
                binding.btnCreateYourPlan.visibility = View.VISIBLE
                binding.fabCreatePlan.visibility = View.VISIBLE
                
                // Update title based on filter
                binding.tvTableTitle.text = when (currentFilter) {
                    "daily" -> "Daily Work Plans"
                    "weekly" -> "Weekly Work Plans"
                    "monthly" -> "Monthly Work Plans"
                    else -> "Your Work Plans"
                }
                
                viewModel.userPlans.value?.let { plans ->
                    val filteredPlans = filterPlansByWeekIfNeeded(plans)
                    yourPlansAdapter.submitList(filteredPlans)
                    updateEmptyState(1, filteredPlans)
                }
            }
            2 -> { // Allotted Plans
                binding.recyclerViewWorkDetails.adapter = allottedPlansAdapter
                binding.btnCreateYourPlan.visibility = View.GONE
                binding.fabCreatePlan.visibility = View.GONE
                
                // Update title based on filter
                binding.tvTableTitle.text = when (currentFilter) {
                    "daily" -> "Daily Work Plans"
                    "weekly" -> "Weekly Work Plans"
                    "monthly" -> "Monthly Work Plans"
                    else -> "Allotted Work Plans"
                }
                
                viewModel.adminPlans.value?.let { plans ->
                    val filteredPlans = filterPlansByWeekIfNeeded(plans)
                    allottedPlansAdapter.submitList(filteredPlans)
                    updateEmptyState(2, filteredPlans)
                }
            }
        }
    }
    
    private fun updateEmptyState(tabPosition: Int, plans: List<WorkPlanItem>) {
        if (currentTab == tabPosition) {
            if (plans.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.contentScrollView.visibility = View.GONE
                
                // Show different messages based on filter type
                if (currentFilter == "weekly" && selectedWeek != null) {
                    // Weekly filter with specific week selected
                    val weekText = binding.weekFilterDropdown.text.toString()
                    binding.tvEmptyStateTitle.text = "No Plans for This Week"
                    when (tabPosition) {
                        0 -> binding.tvEmptyStateSubtitle.text = "No work plans available for $weekText"
                        1 -> binding.tvEmptyStateSubtitle.text = "You haven't created any plans for $weekText"
                        2 -> binding.tvEmptyStateSubtitle.text = "No plans allotted to you for $weekText"
                    }
                } else if (currentDate != null) {
                    // Format current date for display
                    val displayDate = try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val date = inputFormat.parse(currentDate!!)
                        outputFormat.format(date ?: Date())
                    } catch (e: Exception) {
                        "today"
                    }
                    
                    binding.tvEmptyStateTitle.text = "No Work Plans Found"
                    when (tabPosition) {
                        0 -> binding.tvEmptyStateSubtitle.text = "No work plans available for $displayDate"
                        1 -> binding.tvEmptyStateSubtitle.text = "No plans created for $displayDate. Create your first plan!"
                        2 -> binding.tvEmptyStateSubtitle.text = "No plans allotted to you for $displayDate"
                    }
                } else {
                    // Generic messages when filter is applied (no date filter)
                    val filterText = when (currentFilter) {
                        "daily" -> "daily"
                        "weekly" -> "weekly"
                        "monthly" -> "monthly"
                        else -> ""
                    }
                    
                    binding.tvEmptyStateTitle.text = "No Work Plans Found"
                    when (tabPosition) {
                        0 -> binding.tvEmptyStateSubtitle.text = if (filterText.isNotEmpty()) 
                            "No $filterText work plans available" else "No work plans available"
                        1 -> binding.tvEmptyStateSubtitle.text = if (filterText.isNotEmpty()) 
                            "No $filterText plans created. Create your first plan!" else "Create your first plan"
                        2 -> binding.tvEmptyStateSubtitle.text = if (filterText.isNotEmpty()) 
                            "No $filterText plans allotted to you" else "No plans allotted to you"
                    }
                }
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.contentScrollView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showCreatePlanDialog() {
        showCreatePlanBottomSheet()
    }
    
    private fun showCreatePlanBottomSheet() {
        val bottomSheetBinding = BottomSheetCreateWorkPlanBinding.inflate(LayoutInflater.from(this))
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        
        // Set the content view with proper light theme
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        
        // Configure bottom sheet behavior
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetBinding.root.parent as View)
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.isHideable = false // Don't hide on outside click
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        
        // Disable outside touch to close
        bottomSheetDialog.setCanceledOnTouchOutside(false)
        
        // Get available titles from ViewModel (fetched from API)
        val availableTitles = viewModel.workPlanTitles.value ?: emptyList()
        
        if (availableTitles.isEmpty()) {
            // Show error if no titles available
            Toast.makeText(this, "No work plan titles available. Please try again.", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
            return
        }
        
        val selectedTitleIds = mutableSetOf<Int>()
        val selectedCoworkerIds = mutableSetOf<Int>()
        
        // Setup title chips with MaterialComponents theme
        availableTitles.forEach { title ->
            val chip = Chip(this).apply {
                text = title.title
                isCheckable = true
                isChecked = false
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@WorkPlansActivity, R.color.white))
                setTextColor(ContextCompat.getColor(this@WorkPlansActivity, R.color.text_primary))
                chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this@WorkPlansActivity, R.color.gradient_end))
                chipStrokeWidth = 2f
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTitleIds.add(title.id)
                        chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@WorkPlansActivity, R.color.gradient_end))
                        setTextColor(ContextCompat.getColor(this@WorkPlansActivity, R.color.white))
                    } else {
                        selectedTitleIds.remove(title.id)
                        chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@WorkPlansActivity, R.color.white))
                        setTextColor(ContextCompat.getColor(this@WorkPlansActivity, R.color.text_primary))
                    }
                }
            }
            bottomSheetBinding.chipGroupTitles.addView(chip)
        }
        
        // Setup status dropdown
        val statusOptions = listOf("Pending", "In Process", "Completed")
        val statusAdapter = ArrayAdapter(this, R.layout.dropdown_item_status, statusOptions)
        (bottomSheetBinding.tilStatus.editText as? AutoCompleteTextView)?.apply {
            setAdapter(statusAdapter)
            setText("Pending", false)
            dropDownVerticalOffset = 4
            threshold = 1000 // Don't filter on type
        }
        
        // Setup coworkers dropdown (Multi-select)
        val availableCoworkers = viewModel.coworkers.value ?: emptyList()
        android.util.Log.d("WorkPlanBottomSheet", "Available Coworkers: ${availableCoworkers.size}")
        availableCoworkers.forEach { coworker ->
            android.util.Log.d("WorkPlanBottomSheet", "Coworker: ${coworker.getFullName()} (ID: ${coworker.id})")
        }
        
        val coworkerNames = availableCoworkers.map { it.getFullName() }
        val coworkerAdapter = ArrayAdapter(this, R.layout.dropdown_item_status, coworkerNames)
        
        val actvAssignedTo = bottomSheetBinding.actvAssignedTo
        actvAssignedTo.apply {
            setAdapter(coworkerAdapter)
            dropDownVerticalOffset = 4
            threshold = 0 // Show dropdown immediately
            setFocusable(true)
            isFocusableInTouchMode = true
            
            // Handle multi-select for coworkers
            setOnItemClickListener { parent, _, position, _ ->
                if (position >= 0 && position < availableCoworkers.size) {
                    val selectedCoworker = availableCoworkers[position]
                    if (selectedCoworkerIds.contains(selectedCoworker.id)) {
                        selectedCoworkerIds.remove(selectedCoworker.id)
                    } else {
                        selectedCoworkerIds.add(selectedCoworker.id)
                    }
                    
                    // Update the text to show selected coworkers
                    val selectedNames = availableCoworkers
                        .filter { selectedCoworkerIds.contains(it.id) }
                        .map { it.getFullName() }
                    
                    if (selectedNames.isNotEmpty()) {
                        setText(selectedNames.joinToString(", "), false)
                    } else {
                        setText("", false)
                    }
                    
                    // Re-show dropdown
                    showDropDown()
                }
            }
            
            // Show dropdown when focused
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }
        }
        
        // Setup date picker with current date
        val calendar = Calendar.getInstance()
        bottomSheetBinding.etDate.setText(formatDate(calendar.time))
        
        // Date picker click listener
        val datePickerListener = View.OnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    bottomSheetBinding.etDate.setText(formatDate(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        // Set listener for both field and end icon
        bottomSheetBinding.etDate.setOnClickListener(datePickerListener)
        bottomSheetBinding.tilDate.setEndIconOnClickListener(datePickerListener)
        
        // Setup buttons
        bottomSheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetBinding.btnCreate.setOnClickListener {
            // Validate inputs
            if (selectedTitleIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val description = bottomSheetBinding.etDescription.text?.toString()
            val statusText = bottomSheetBinding.actvStatus.text.toString()
            val status = when (statusText) {
                "Pending" -> "pending"
                "In Process" -> "in_process"
                "Completed" -> "completed"
                else -> "pending"
            }
            val date = formatDateForApi(calendar.time)
            
            // Create request with coworkers if selected
            val request = CreateWorkPlanRequest(
                titles = selectedTitleIds.toList(),
                description = description,
                status = status,
                date = date,
                coworkers = if (selectedCoworkerIds.isNotEmpty()) selectedCoworkerIds.toList() else null
            )
            
            // Call API
            viewModel.createWorkPlan(request)
            bottomSheetDialog.dismiss()
        }
        
        // Show bottom sheet
        bottomSheetDialog.show()
    }
    
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(date)
    }
    
    private fun formatDateForApi(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }
    
    private fun handleStatusClick(workPlan: WorkPlanItem) {
        // Check if status is already completed
        if (workPlan.status.equals("completed", ignoreCase = true)) {
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Work Plan Completed")
                .setMessage("This work plan has already been completed and cannot be modified.")
                .setPositiveButton("OK", null)
                .show()
            
            // Set text color for message to ensure visibility
            dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextColor(
                ContextCompat.getColor(this, R.color.text_primary)
            )
            return
        }
        
        // Show edit dialog (allow editing for both user-created and admin-created plans)
        showEditWorkPlanDialog(workPlan)
    }
    
    private fun showEditWorkPlanDialog(workPlan: WorkPlanItem) {
        val dialogBinding = DialogEditWorkPlanBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        
        // Ensure text colors are set programmatically to fix visibility issue
        dialogBinding.rbPending.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        dialogBinding.rbInProcess.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        dialogBinding.rbCompleted.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        
        // Set status radio button based on current work plan status
        when (workPlan.status.lowercase()) {
            "pending" -> dialogBinding.rbPending.isChecked = true
            "in_process" -> dialogBinding.rbInProcess.isChecked = true
            "completed" -> dialogBinding.rbCompleted.isChecked = true
        }
        
        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Update button
        dialogBinding.btnUpdate.setOnClickListener {
            // Get selected status
            val selectedRadioId = dialogBinding.radioGroupStatus.checkedRadioButtonId
            val status = when (selectedRadioId) {
                R.id.rbPending -> "pending"
                R.id.rbInProcess -> "in_process"
                R.id.rbCompleted -> "completed"
                else -> "pending"
            }
            
            // Use current date automatically
            val currentDate = formatDateForApi(Date())
            
            // Create update request (description null, status updated, current date)
            val request = UpdateWorkPlanRequest(
                description = null,
                status = status,
                date = currentDate
            )
            
            // Call API
            viewModel.updateWorkPlan(workPlan.id, request)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showEditDialog(workPlan: WorkPlanItem) {
        // This method is kept for backward compatibility but not used
        showEditWorkPlanDialog(workPlan)
    }
    
    private fun showDeleteConfirmation(workPlan: WorkPlanItem) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Delete Work Plan")
            .setMessage("Are you sure you want to delete this work plan?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteWorkPlan(workPlan.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        // Set text colors for message and buttons to ensure visibility
        dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextColor(
            ContextCompat.getColor(this, R.color.text_primary)
        )
    }
    
    private fun checkAuthenticationAndLoadData() {
        lifecycleScope.launch {
            println("WorkPlansActivity: Checking authentication status...")
            
            // Check if tokens exist (don't validate expiry - TokenAuthenticator will handle)
            val hasTokens = authManager.hasStoredTokens()
            
            if (hasTokens) {
                println("WorkPlansActivity: Tokens found, loading data...")
                // Load work plan titles and coworkers first
                viewModel.loadWorkPlanTitles()
                viewModel.loadCoworkers()
                // Load initial data with current date filter
                currentDate = getCurrentDate()
                viewModel.loadWorkPlans(date = currentDate)
            } else {
                println("WorkPlansActivity: No tokens found, redirecting to login...")
                Toast.makeText(this@WorkPlansActivity, "Please login to continue", Toast.LENGTH_LONG).show()
                performLogout()
            }
        }
    }
    
    private fun performLogout() {
        lifecycleScope.launch {
            authManager.logout()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.destroy()
    }
    
    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_work_plan
    }
    
    override fun onBackPressed() {
        navigateToActivity(DashboardActivity::class.java)
        finish()
    }
}
