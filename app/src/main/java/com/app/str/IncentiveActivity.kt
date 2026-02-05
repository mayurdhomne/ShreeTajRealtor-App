package com.app.str

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.str.adapter.IncentiveAdapter
import com.app.str.data.model.IncentiveResponse
import com.app.str.data.model.Result
import com.app.str.data.repository.IncentiveRepository
import com.app.str.ui.base.BaseActivity
import com.app.str.utils.EdgeToEdgeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class IncentiveActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var rvIncentives: RecyclerView
    private lateinit var cardEmptyState: MaterialCardView
    private lateinit var tvTotalEarnings: TextView
    private lateinit var tvCommission: TextView
    private lateinit var tvBonus: TextView
    private lateinit var progressSales: ProgressBar
    private lateinit var tvSalesProgress: TextView
    private lateinit var tvDealsCount: TextView
    private lateinit var tvAreaSold: TextView
    
    private val incentiveAdapter = IncentiveAdapter()
    
    @Inject
    lateinit var incentiveRepository: IncentiveRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_incentive)
        
        initViews()
        setupEdgeToEdgeWithBottomNav()
        setupViews()
        setupBottomNavigation()
        setupRecyclerView()
        loadIncentiveData()
    }
    
    /**
     * Setup edge-to-edge display with proper handling for bottom navigation bar.
     * Uses EdgeToEdgeHelper for consistent behavior across all devices.
     */
    private fun setupEdgeToEdgeWithBottomNav() {
        val mainContainer = findViewById<View>(R.id.main)
        val contentScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.contentScrollView)
        
        // Use EdgeToEdgeHelper for consistent behavior across all devices
        EdgeToEdgeHelper.setupEdgeToEdge(
            rootView = mainContainer,
            bottomNav = bottomNavigation,
            contentView = contentScrollView,
            additionalBottomPadding = resources.getDimensionPixelSize(R.dimen.padding_medium)
        )
    }
    
    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        rvIncentives = findViewById(R.id.rvIncentives)
        cardEmptyState = findViewById(R.id.cardEmptyState)
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings)
        tvCommission = findViewById(R.id.tvCommission)
        tvBonus = findViewById(R.id.tvBonus)
        progressSales = findViewById(R.id.progressSales)
        tvSalesProgress = findViewById(R.id.tvSalesProgress)
        tvDealsCount = findViewById(R.id.tvDealsCount)
        tvAreaSold = findViewById(R.id.tvAreaSold)
    }
    
    private fun setupRecyclerView() {
        rvIncentives.apply {
            layoutManager = LinearLayoutManager(this@IncentiveActivity)
            adapter = incentiveAdapter
        }
    }

    private fun setupViews() {
        // Set current month
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.tvPerformanceMonth).text = monthFormat.format(Date())
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_incentive
        
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
                    // Already on Incentive
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
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun loadIncentiveData() {
        lifecycleScope.launch {
            try {
                when (val result = incentiveRepository.getMyIncentives()) {
                    is Result.Success -> {
                        val incentives = result.data
                        if (incentives.isNotEmpty()) {
                            // Show RecyclerView, hide empty state
                            rvIncentives.visibility = View.VISIBLE
                            cardEmptyState.visibility = View.GONE
                            
                            // Submit data to adapter
                            incentiveAdapter.submitList(incentives)
                            
                            // Calculate and display statistics
                            updateStatistics(incentives)
                        } else {
                            // Show empty state
                            rvIncentives.visibility = View.GONE
                            cardEmptyState.visibility = View.VISIBLE
                            setDefaultValues()
                        }
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@IncentiveActivity,
                            "Failed to load incentives: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        setDefaultValues()
                    }
                    is Result.Loading -> {
                        // Show loading state if needed
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@IncentiveActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setDefaultValues()
            }
        }
    }
    
    private fun updateStatistics(incentives: List<IncentiveResponse>) {
        // Calculate total earnings (sum of all commissions)
        val totalCommission = incentives.sumOf { it.commissionPrice.toDoubleOrNull() ?: 0.0 }
        val totalPaid = incentives.sumOf { it.totalPaidCommission.toDoubleOrNull() ?: 0.0 }
        val totalBalance = incentives.sumOf { it.balanceCommission.toDoubleOrNull() ?: 0.0 }
        
        // Update UI
        tvTotalEarnings.text = formatCurrency(totalPaid)
        tvCommission.text = formatCurrency(totalCommission)
        tvBonus.text = formatCurrency(totalBalance)
        
        // Update deals count
        tvDealsCount.text = incentives.size.toString()
        
        // Calculate progress (paid vs total commission)
        val progressPercentage = if (totalCommission > 0) {
            ((totalPaid / totalCommission) * 100).toInt()
        } else {
            0
        }
        tvSalesProgress.text = "$progressPercentage%"
        progressSales.progress = progressPercentage
        
        // Area sold - You can update this if you have the data
        tvAreaSold.text = "N/A"
    }
    
    private fun setDefaultValues() {
        tvTotalEarnings.text = "₹0.00"
        tvCommission.text = "₹0"
        tvBonus.text = "₹0"
        tvSalesProgress.text = "0%"
        progressSales.progress = 0
        tvDealsCount.text = "0"
        tvAreaSold.text = "0 sqft"
    }
    
    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(amount)
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_incentive
    }

    override fun onBackPressed() {
        // Navigate to Dashboard when back is pressed
        navigateToActivity(DashboardActivity::class.java)
        finish()
    }
}
