package com.app.str

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.app.str.data.model.ProfileResponse
import com.app.str.data.model.Result
import com.app.str.utils.EdgeToEdgeHelper
import com.app.str.utils.ErrorHandler
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.AuthViewModel
import com.app.str.viewmodel.ProfileViewModel
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var bottomNavigation: BottomNavigationView
    
    // UI Components
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var btnBack: ImageView
    private lateinit var fabEditProfile: ExtendedFloatingActionButton
    private lateinit var loadingOverlay: View
    
    // Profile Info Views
    private lateinit var tvFullName: TextView
    private lateinit var tvDesignation: TextView
    private lateinit var tvDepartment: TextView
    
    // Personal Information
    private lateinit var mobileInfo: LinearLayout
    private lateinit var dobInfo: LinearLayout
    private lateinit var genderInfo: LinearLayout
    private lateinit var maritalStatusInfo: LinearLayout
    
    // Documents
    private lateinit var aadhaarInfo: LinearLayout
    private lateinit var panInfo: LinearLayout
    
    // Address
    private lateinit var localityInfo: LinearLayout
    private lateinit var cityInfo: LinearLayout
    private lateinit var stateInfo: LinearLayout
    private lateinit var pincodeInfo: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        // Initialize LoadingDialog
        loadingDialog = LoadingDialog(this)
        
        // Initialize bottomNavigation first before setupWindowInsets
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        initializeViews()
        setupToolbar()
        setupClickListeners()
        setupBottomNavigation()
        setupWindowInsets()  // Called after bottomNavigation is initialized
        observeViewModel()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_profile
        
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
                    navigateToActivity(DailyReportActivity::class.java)
                    true
                }
                R.id.nav_incentive -> {
                    navigateToActivity(IncentiveActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    // Already on Profile
                    true
                }
                else -> false
            }
        }
    }
    
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
    
    private fun setupWindowInsets() {
        val mainContainer = findViewById<View>(R.id.main)
        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        val contentScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.contentScrollView)
        
        // Use EdgeToEdgeHelper for consistent behavior across all devices
        EdgeToEdgeHelper.setupEdgeToEdge(
            rootView = mainContainer,
            bottomNav = bottomNavigation,
            contentView = contentScrollView,
            appBarLayout = appBarLayout,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
        
        // Position FAB above BottomNavigationView dynamically
        bottomNavigation.post {
            val fabMargin = bottomNavigation.height + resources.getDimensionPixelSize(R.dimen.padding_medium)
            (fabEditProfile.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)?.let { params ->
                params.bottomMargin = fabMargin
                fabEditProfile.layoutParams = params
            }
        }
        
        // Set transparent status/navigation bar colors for edge-to-edge
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
    
    private fun initializeViews() {
        // Collapsing Toolbar
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        btnBack = findViewById(R.id.btnBack)
        fabEditProfile = findViewById(R.id.fabEditProfile)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        // Profile Header
        tvFullName = findViewById(R.id.tvFullName)
        tvDesignation = findViewById(R.id.tvDesignation)
        tvDepartment = findViewById(R.id.tvDepartment)
        
        // Personal Information
        mobileInfo = findViewById(R.id.mobileInfo)
        dobInfo = findViewById(R.id.dobInfo)
        genderInfo = findViewById(R.id.genderInfo)
        maritalStatusInfo = findViewById(R.id.maritalStatusInfo)
        
        // Documents
        aadhaarInfo = findViewById(R.id.aadhaarInfo)
        panInfo = findViewById(R.id.panInfo)
        
        // Address
        localityInfo = findViewById(R.id.localityInfo)
        cityInfo = findViewById(R.id.cityInfo)
        stateInfo = findViewById(R.id.stateInfo)
        pincodeInfo = findViewById(R.id.pincodeInfo)
    }
    
    private fun setupToolbar() {
        // Find toolbar if it exists in the layout and set it up
        try {
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                title = "Profile"
                setDisplayShowTitleEnabled(false) // Hide title as we use collapsing toolbar
            }
        } catch (e: Exception) {
            // Toolbar not found in layout, skip setup
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        
        // Set overflow menu icon color to white
        try {
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            toolbar?.let {
                // Set overflow icon color to white
                it.overflowIcon?.setTint(Color.WHITE)
            }
        } catch (e: Exception) {
            // Handle if toolbar not found
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun performLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                authViewModel.logout()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            navigateToActivity(DashboardActivity::class.java)
            finish()
        }
        
        fabEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.profileState.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        loadingDialog.show("Loading profile...")
                        showLoading(true)
                    }
                    is Result.Success -> {
                        loadingDialog.dismiss()
                        showLoading(false)
                        populateProfileData(result.data)
                    }
                    is Result.Error -> {
                        loadingDialog.dismiss()
                        showLoading(false)
                        handleError(result.message)
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    loadingDialog.show("Loading profile...")
                } else {
                    loadingDialog.dismiss()
                }
                showLoading(isLoading)
            }
        }
    }
    
    private fun populateProfileData(profile: ProfileResponse) {
        try {
            // Update header info
            tvFullName.text = "${profile.firstName} ${profile.lastName}"
            tvDesignation.text = profile.designation
            tvDepartment.text = profile.department
            
            // Update collapsing toolbar title
            collapsingToolbar.title = "${profile.firstName} ${profile.lastName}"
            
            // Personal Information
            setupInfoItem(mobileInfo, "Mobile Number", profile.mobileNumber, R.drawable.ic_phonecard)
            setupInfoItem(dobInfo, "Date of Birth", formatDate(profile.dateOfBirth), R.drawable.ic_calendar)
            setupInfoItem(genderInfo, "Gender", formatGender(profile.gender), R.drawable.ic_person)
            setupInfoItem(maritalStatusInfo, "Marital Status", formatMaritalStatus(profile.maritalStatus), R.drawable.ic_person_outline)
            
            // Documents
            setupInfoItem(aadhaarInfo, "Aadhaar Number", maskAadhaar(profile.aadhaarNumber), R.drawable.ic_document)
            setupInfoItem(panInfo, "PAN Number", profile.panNumber, R.drawable.ic_document)
            
            // Address
            setupInfoItem(localityInfo, "Locality", profile.locality, R.drawable.ic_location)
            setupInfoItem(cityInfo, "City", profile.city, R.drawable.ic_location)
            setupInfoItem(stateInfo, "State", profile.state, R.drawable.ic_location)
            setupInfoItem(pincodeInfo, "Pincode", profile.pincode, R.drawable.ic_location)
            
        } catch (e: Exception) {
            ErrorHandler.logError("ProfileActivity", "Error populating profile data", e)
            handleError("Error displaying profile information")
        }
    }
    
    private fun setupInfoItem(container: LinearLayout, label: String, value: String, iconRes: Int) {
        val infoIcon = container.findViewById<ImageView>(R.id.infoIcon)
        val infoLabel = container.findViewById<TextView>(R.id.infoLabel)
        val infoValue = container.findViewById<TextView>(R.id.infoValue)
        
        infoIcon.setImageResource(iconRes)
        infoLabel.text = label
        infoValue.text = value
    }
    
    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
    
    private fun formatGender(gender: String): String {
        return gender.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
    
    private fun formatMaritalStatus(status: String): String {
        return status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
    
    private fun maskAadhaar(aadhaar: String): String {
        return if (aadhaar.length >= 8) {
            "XXXX-XXXX-${aadhaar.takeLast(4)}"
        } else {
            aadhaar
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        fabEditProfile.isEnabled = !show
    }
    
    private fun handleError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        ErrorHandler.logError("ProfileActivity", message, null)
    }
    
    override fun onResume() {
        super.onResume()
        // Keep bottom navigation in sync
        bottomNavigation.selectedItemId = R.id.nav_profile
        // Refresh profile data when returning from edit screen
        viewModel.loadProfile()
    }
    
    override fun onBackPressed() {
        navigateToActivity(DashboardActivity::class.java)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.destroy()
    }
}