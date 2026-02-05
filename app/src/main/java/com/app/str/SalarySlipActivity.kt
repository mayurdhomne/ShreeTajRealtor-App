package com.app.str

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.app.str.adapter.AttendanceCalendarAdapter
import com.app.str.data.model.MonthYear
import com.app.str.data.model.Result
import com.app.str.data.model.SalarySlipResponse
import com.app.str.databinding.ActivitySalarySlipBinding
import com.app.str.utils.PdfGenerator
import com.app.str.viewmodel.SalarySlipViewModel
import com.app.str.data.repository.ProfileRepository
import com.app.str.data.model.ProfileResponse
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import java.text.SimpleDateFormat

@AndroidEntryPoint
class SalarySlipActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalarySlipBinding
    private val viewModel: SalarySlipViewModel by viewModels()
    
    @Inject
    lateinit var profileRepository: ProfileRepository
    
    private lateinit var monthYearList: List<MonthYear>
    private var selectedMonthYear: MonthYear? = null
    private var currentSalarySlip: SalarySlipResponse? = null
    private var employeeName: String = "Employee Name"
    private var designation: String = "Associate Editor"
    private var department: String = "Editorial"
    private var city: String = "Gujarat"
    private var state: String = "India"
    private var dateOfJoining: String = "30/06/2020"
    private var employeeId: String = "43521"
    
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Storage permission launcher for Android 10 and below
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadPdf()
        } else {
            showSnackbar("Storage permission required to download PDF")
        }
    }

    // Manage external storage permission launcher for Android 11+
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            downloadPdf()
        } else {
            showSnackbar("Storage permission required to download PDF")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalarySlipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMonthYearFilter()
        setupClickListeners()
        setupObservers()
        fetchEmployeeProfile()
        
        showEmptyState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupMonthYearFilter() {
        // Setup month dropdown
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        val monthAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            months
        )
        
        binding.actvMonth.setAdapter(monthAdapter)
        
        // Setup year dropdown
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (2020..currentYear + 1).map { it.toString() }.toTypedArray()
        
        val yearAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            years
        )
        
        binding.actvYear.setAdapter(yearAdapter)
        
        // Set current month and year as default
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYearValue = calendar.get(Calendar.YEAR)
        
        binding.actvMonth.setText(months[currentMonth], false)
        binding.actvYear.setText(currentYearValue.toString(), false)
    }

    private fun setupClickListeners() {
        binding.btnFetchSlip.setOnClickListener {
            val selectedMonth = binding.actvMonth.text.toString()
            val selectedYear = binding.actvYear.text.toString()
            
            if (selectedMonth.isNotBlank() && selectedYear.isNotBlank()) {
                val monthNumber = getMonthNumber(selectedMonth)
                val yearNumber = selectedYear.toIntOrNull()
                
                if (monthNumber > 0 && yearNumber != null) {
                    // Check if current month is selected and month hasn't ended
                    if (isCurrentMonthAndNotEnded(yearNumber, monthNumber)) {
                        showCurrentMonthNotAvailableMessage(selectedMonth, yearNumber)
                    } else {
                        viewModel.fetchSalarySlip(yearNumber, monthNumber)
                    }
                } else {
                    showSnackbar("Please select valid month and year")
                }
            } else {
                showSnackbar("Please select month and year")
            }
        }

        binding.btnRetry.setOnClickListener {
            val selectedMonth = binding.actvMonth.text.toString()
            val selectedYear = binding.actvYear.text.toString()
            
            if (selectedMonth.isNotBlank() && selectedYear.isNotBlank()) {
                val monthNumber = getMonthNumber(selectedMonth)
                val yearNumber = selectedYear.toIntOrNull()
                
                if (monthNumber > 0 && yearNumber != null) {
                    // Check if current month is selected and month hasn't ended
                    if (isCurrentMonthAndNotEnded(yearNumber, monthNumber)) {
                        showCurrentMonthNotAvailableMessage(selectedMonth, yearNumber)
                    } else {
                        viewModel.fetchSalarySlip(yearNumber, monthNumber)
                    }
                }
            }
        }

        binding.btnDownloadPdf.setOnClickListener {
            checkStoragePermissionAndDownload()
        }
    }
    
    /**
     * Check if the selected month is current month and hasn't ended yet
     */
    private fun isCurrentMonthAndNotEnded(year: Int, month: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Check if it's the current month and year
        if (year == currentYear && month == currentMonth) {
            // Check if the month hasn't ended yet
            return currentDay < lastDayOfMonth
        }
        
        // Check if it's a future month
        if (year > currentYear || (year == currentYear && month > currentMonth)) {
            return true
        }
        
        return false
    }
    
    /**
     * Show message that current month salary slip is not available yet
     */
    private fun showCurrentMonthNotAvailableMessage(monthName: String, year: Int) {
        val calendar = Calendar.getInstance()
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Format the last day of month
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val currentMonthName = monthFormat.format(calendar.time)
        
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        binding.contentView.visibility = View.GONE
        
        binding.tvEmptyTitle.text = "Salary Slip Not Available"
        binding.tvEmptyMessage.text = "$currentMonthName $year salary slip will be available after month end ($lastDayOfMonth $currentMonthName $year).\n\nPlease check back after the month ends."
    }

    private fun setupObservers() {
        viewModel.salarySlipResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoadingState()
                }
                is Result.Success -> {
                    currentSalarySlip = result.data
                    showSalarySlipData(result.data)
                }
                is Result.Error -> {
                    showErrorState(result.message)
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.loadingView.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.contentView.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.contentView.visibility = View.GONE
        binding.tvErrorMessage.text = message
    }

    private fun showEmptyState() {
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        binding.contentView.visibility = View.GONE
        
        // Reset to default message
        binding.tvEmptyTitle.text = "Select Month & Year"
        binding.tvEmptyMessage.text = "Select a previous month and year to view your salary slip.\n\nNote: Current month's salary slip will be available after month end."
    }

    private fun showSalarySlipData(salarySlip: SalarySlipResponse) {
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.contentView.visibility = View.VISIBLE

        // Update header
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = monthNames[salarySlip.month - 1]
        binding.tvSlipTitle.text = "Salary Slip - $monthName ${salarySlip.year}"
        binding.tvEmployeeName.text = employeeName

        // Update header stats
        binding.tvWorkingDays.text = salarySlip.workingDays.toString()
        binding.tvPresentDays.text = salarySlip.presentDays.toString()
        binding.tvAbsentDays.text = salarySlip.absentDays.toString()

        // Update salary details
        binding.tvMonthlySalary.text = formatCurrency(salarySlip.monthlySalary)
        binding.tvDailySalary.text = formatCurrency(salarySlip.dailySalary)
        binding.tvHalfDays.text = salarySlip.halfDayCount.toString()
        binding.tvAllowedLeaves.text = salarySlip.allowedLeaves.toString()
        binding.tvUnpaidAbsences.text = salarySlip.unpaidAbsences.toString()
        
        // Update target and sales info
        binding.tvTargetArea.text = formatCurrency(salarySlip.targetArea)
        binding.tvSalesSum.text = formatCurrency(salarySlip.salesSum)

        // Update deductions
        binding.tvHalfDayDeduction.text = formatCurrency(salarySlip.halfDayDeduction)
        binding.tvAbsenceDeduction.text = formatCurrency(salarySlip.absenceDeduction)
        binding.tvTargetPenalty.text = formatCurrency(salarySlip.targetPenalty)
        binding.tvTotalDeduction.text = formatCurrency(salarySlip.totalDeduction)
        
        // Update gross salary
        binding.tvGrossSalary.text = formatCurrency(salarySlip.grossSalary)

        // Update net salary
        binding.tvNetSalary.text = formatCurrency(salarySlip.netSalary)

        // Change net salary color based on value
        val netSalaryColor = if (salarySlip.netSalary > 0) {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        }
        binding.tvNetSalary.setTextColor(netSalaryColor)
        
        // Setup attendance calendar
        setupAttendanceCalendar(salarySlip.attendanceCalendar)
    }

    private fun formatCurrency(amount: Double): String {
        return numberFormat.format(amount)
    }
    
    private fun setupAttendanceCalendar(attendanceData: List<String>) {
        val adapter = AttendanceCalendarAdapter(this, attendanceData)
        binding.rvAttendanceCalendar.adapter = adapter
        binding.rvAttendanceCalendar.layoutManager = GridLayoutManager(this, 7) // 7 days per week
    }

    private fun checkStoragePermissionAndDownload() {
        currentSalarySlip?.let { salarySlip ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11 and above - check MANAGE_EXTERNAL_STORAGE
                    if (Environment.isExternalStorageManager()) {
                        downloadPdf()
                    } else {
                        requestManageStoragePermission()
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6-10 - check WRITE_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        downloadPdf()
                    } else {
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                else -> {
                    // Below Android 6 - no runtime permission needed
                    downloadPdf()
                }
            }
        } ?: run {
            showSnackbar("No salary slip data available to download")
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${applicationContext.packageName}")
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        }
    }

    private fun downloadPdf() {
        currentSalarySlip?.let { salarySlip ->
            lifecycleScope.launch {
                try {
                    showSnackbar("Generating PDF...")
                    
                    // Fetch fresh profile data for PDF generation
                    var pdfEmployeeName = employeeName
                    var pdfDesignation = designation
                    var pdfCity = city
                    var pdfState = state
                    var pdfDateOfJoining = dateOfJoining
                    var pdfEmployeeId = employeeId
                    
                    // Get latest profile data
                    profileRepository.getProfile().collect { profileResult ->
                        when (profileResult) {
                            is Result.Success -> {
                                val profile = profileResult.data
                                pdfEmployeeName = "${profile.firstName} ${profile.lastName}".trim().ifBlank { "Employee Name" }
                                pdfDesignation = profile.designation.ifBlank { "Associate Editor" }
                                pdfCity = profile.city.ifBlank { "Gujarat" }
                                pdfState = profile.state.ifBlank { "India" }
                                pdfEmployeeId = profile.id.toString()
                                
                                // Parse date of birth as joining date
                                pdfDateOfJoining = if (profile.dateOfBirth.isNotBlank()) {
                                    try {
                                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        val date = inputFormat.parse(profile.dateOfBirth)
                                        date?.let { outputFormat.format(it) } ?: "30/06/2020"
                                    } catch (e: Exception) {
                                        "30/06/2020"
                                    }
                                } else {
                                    "30/06/2020"
                                }
                            }
                            else -> {
                                // Use existing values or defaults
                                pdfEmployeeName = employeeName.ifBlank { "Employee Name" }
                                pdfDesignation = designation.ifBlank { "Associate Editor" }
                                pdfCity = city.ifBlank { "Gujarat" }
                                pdfState = state.ifBlank { "India" }
                                pdfDateOfJoining = dateOfJoining.ifBlank { "30/06/2020" }
                                pdfEmployeeId = employeeId.ifBlank { "43521" }
                            }
                        }
                        
                        // Generate PDF with profile data
                        val result = PdfGenerator.generateSalarySlipPdf(
                            context = this@SalarySlipActivity,
                            salarySlip = salarySlip,
                            employeeName = pdfEmployeeName,
                            designation = pdfDesignation,
                            city = pdfCity,
                            state = pdfState,
                            dateOfJoining = pdfDateOfJoining,
                            employeeId = pdfEmployeeId
                        )
                        
                        when (result) {
                            is Result.Success -> {
                                showSnackbar("PDF saved to Downloads: ${result.data.name}")
                                
                                // Open the PDF file
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        androidx.core.content.FileProvider.getUriForFile(
                                            this@SalarySlipActivity,
                                            "${applicationContext.packageName}.provider",
                                            result.data
                                        ),
                                        "application/pdf"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                
                                try {
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    showSnackbar("PDF saved successfully but no app found to open it")
                                }
                            }
                            is Result.Error -> {
                                showSnackbar("Failed to generate PDF: ${result.message}")
                            }
                            else -> {}
                        }
                        
                        return@collect // Exit the collect loop after processing
                    }
                    
                } catch (e: Exception) {
                    showSnackbar("Error generating PDF: ${e.message}")
                }
            }
        }
    }

    private fun fetchEmployeeProfile() {
        lifecycleScope.launch {
            profileRepository.getProfile().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val profile = result.data
                        employeeName = "${profile.firstName} ${profile.lastName}".trim()
                        if (employeeName.isBlank()) {
                            employeeName = "Employee Name"
                        }
                        designation = profile.designation.ifBlank { "Associate Editor" }
                        department = profile.department.ifBlank { "Editorial" }
                        city = profile.city.ifBlank { "Gujarat" }
                        state = profile.state.ifBlank { "India" }
                        employeeId = profile.id.toString()
                        
                        // Parse and format date of birth as joining date (or use a default)
                        dateOfJoining = if (profile.dateOfBirth.isNotBlank()) {
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val date = inputFormat.parse(profile.dateOfBirth)
                                date?.let { outputFormat.format(it) } ?: "30/06/2020"
                            } catch (e: Exception) {
                                "30/06/2020"
                            }
                        } else {
                            "30/06/2020"
                        }
                        
                        // Update UI with employee name
                        binding.tvEmployeeName.text = employeeName
                    }
                    is Result.Error -> {
                        // Keep default values if profile fetch fails
                        employeeName = "Employee Name"
                        designation = "Associate Editor"
                        department = "Editorial"
                        city = "Gujarat"
                        state = "India"
                        dateOfJoining = "30/06/2020"
                        employeeId = "43521"
                        binding.tvEmployeeName.text = employeeName
                    }
                    else -> {}
                }
            }
        }
    }

    private fun getMonthNumber(monthName: String): Int {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return months.indexOf(monthName) + 1
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}