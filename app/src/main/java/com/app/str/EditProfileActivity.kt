package com.app.str

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.app.str.data.model.ProfileResponse
import com.app.str.data.model.ProfileUpdateRequest
import com.app.str.data.model.Result
import com.app.str.databinding.ActivityEditProfileBinding
import com.app.str.utils.ErrorHandler
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    
    private lateinit var loadingDialog: LoadingDialog
    
    // Flags to prevent infinite loops in text watchers
    private var isUpdatingAadhaar = false
    private var isUpdatingPan = false
    
    // Current profile data
    private var currentProfile: ProfileResponse? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize LoadingDialog
        loadingDialog = LoadingDialog(this)
        
        setupDropdowns()
        setupDatePicker()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
        
        // Load current profile data
        viewModel.loadProfile()
    }
    
    private fun setupDropdowns() {
        // Gender dropdown
        val genderOptions = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.etGender.setAdapter(genderAdapter)

        // Marital Status dropdown
        val maritalOptions = arrayOf("Single", "Married", "Divorced", "Widowed")
        val maritalAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, maritalOptions)
        binding.etMaritalStatus.setAdapter(maritalAdapter)
    }

    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val maxDate = Calendar.getInstance()
            maxDate.add(Calendar.YEAR, -18) // Minimum 18 years old

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    binding.etDateOfBirth.setText(dateFormatter.format(selectedDate.time))
                    binding.etDateOfBirth.error = null
                },
                maxDate.get(Calendar.YEAR),
                maxDate.get(Calendar.MONTH),
                maxDate.get(Calendar.DAY_OF_MONTH)
            )

            // Set max date to 18 years ago
            datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

            // Set min date to 100 years ago
            val minDate = Calendar.getInstance()
            minDate.add(Calendar.YEAR, -100)
            datePickerDialog.datePicker.minDate = minDate.timeInMillis

            datePickerDialog.show()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        binding.btnSave.setOnClickListener {
            if (validateAllFields()) {
                saveProfile()
            }
        }
    }
    
    private fun setupTextWatchers() {
        // Auto-format PAN number to uppercase
        binding.etPanNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text != text.uppercase()) {
                    binding.etPanNumber.setText(text.uppercase())
                    binding.etPanNumber.setSelection(text.length)
                }
            }
        })

        // Real-time validation for Aadhaar
        binding.etAadhaarNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.length == 12) {
                    binding.etAadhaarNumber.error = null
                }
            }
        })

        // Real-time validation for Mobile
        binding.etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.length == 10 && text[0] in '6'..'9') {
                    binding.etMobileNumber.error = null
                }
            }
        })

        // Real-time validation for Pincode
        binding.etPincode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.length == 6) {
                    binding.etPincode.error = null
                }
            }
        })
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            showLoading(true)
                        }
                        is Result.Success -> {
                            showLoading(false)
                            currentProfile = result.data
                            populateFields(result.data)
                        }
                        is Result.Error -> {
                            showLoading(false)
                            showErrorMessage("Failed to load profile: ${result.message}")
                        }
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateProfileState.collect { result ->
                    result?.let {
                        when (it) {
                            is Result.Loading -> {
                                showLoading(true)
                            }
                            is Result.Success -> {
                                showLoading(false)
                                Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Navigate back after successful update
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 500)
                            }
                            is Result.Error -> {
                                showLoading(false)
                                showErrorMessage("Failed to update profile: ${it.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(android.R.color.holo_red_dark))
            .setTextColor(getColor(android.R.color.white))
            .show()
    }
    
    private fun populateFields(profile: ProfileResponse) {
        try {
            binding.etFirstName.setText(profile.firstName)
            binding.etLastName.setText(profile.lastName)
            binding.etDesignation.setText(profile.designation)
            binding.etDepartment.setText(profile.department)
            binding.etMobileNumber.setText(profile.mobileNumber)
            binding.etDateOfBirth.setText(formatDisplayDate(profile.dateOfBirth))
            
            // Set gender in dropdown
            binding.etGender.setText(profile.gender.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }, false)
            
            // Set marital status in dropdown
            binding.etMaritalStatus.setText(profile.maritalStatus.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }, false)
            
            binding.etAadhaarNumber.setText(profile.aadhaarNumber)
            binding.etPanNumber.setText(profile.panNumber)
            binding.etLocality.setText(profile.locality)
            binding.etCity.setText(profile.city)
            binding.etState.setText(profile.state)
            binding.etPincode.setText(profile.pincode)
            
            // Set selected date for date picker
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(profile.dateOfBirth)
                date?.let {
                    selectedDate.time = it
                }
            } catch (e: Exception) {
                ErrorHandler.logError("EditProfileActivity", "Error parsing date: ${profile.dateOfBirth}", e)
            }
            
        } catch (e: Exception) {
            ErrorHandler.logError("EditProfileActivity", "Error populating fields", e)
            showErrorMessage("Error loading profile data")
        }
    }
    

    
    private fun saveProfile() {
        try {
            val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateOfBirth = serverDateFormat.format(selectedDate.time)
            
            val updateRequest = ProfileUpdateRequest(
                firstName = binding.etFirstName.text.toString().trim(),
                lastName = binding.etLastName.text.toString().trim(),
                designation = binding.etDesignation.text.toString().trim(),
                department = binding.etDepartment.text.toString().trim(),
                mobileNumber = binding.etMobileNumber.text.toString().trim(),
                dateOfBirth = dateOfBirth,
                gender = binding.etGender.text.toString().trim().lowercase(),
                maritalStatus = binding.etMaritalStatus.text.toString().trim().lowercase(),
                aadhaarNumber = binding.etAadhaarNumber.text.toString().trim(),
                panNumber = binding.etPanNumber.text.toString().trim().uppercase(),
                locality = binding.etLocality.text.toString().trim(),
                city = binding.etCity.text.toString().trim(),
                state = binding.etState.text.toString().trim(),
                pincode = binding.etPincode.text.toString().trim()
            )
            
            viewModel.updateProfile(updateRequest)
            
        } catch (e: Exception) {
            ErrorHandler.logError("EditProfileActivity", "Error creating update request", e)
            showErrorMessage("Error preparing profile data")
        }
    }
    
    private fun validateAllFields(): Boolean {
        var isValid = true

        // Validate First Name
        if (binding.etFirstName.text.isNullOrBlank()) {
            binding.etFirstName.error = "First name is required"
            isValid = false
        } else if (binding.etFirstName.text.toString().length < 2) {
            binding.etFirstName.error = "Name must be at least 2 characters"
            isValid = false
        }

        // Validate Last Name
        if (binding.etLastName.text.isNullOrBlank()) {
            binding.etLastName.error = "Last name is required"
            isValid = false
        } else if (binding.etLastName.text.toString().length < 2) {
            binding.etLastName.error = "Name must be at least 2 characters"
            isValid = false
        }

        // Validate Mobile Number
        val mobile = binding.etMobileNumber.text.toString().trim()
        when {
            mobile.isBlank() -> {
                binding.etMobileNumber.error = "Mobile number is required"
                isValid = false
            }
            mobile.length != 10 -> {
                binding.etMobileNumber.error = "Mobile number must be 10 digits"
                isValid = false
            }
            mobile[0] !in '6'..'9' -> {
                binding.etMobileNumber.error = "Invalid mobile number (must start with 6-9)"
                isValid = false
            }
            !mobile.all { it.isDigit() } -> {
                binding.etMobileNumber.error = "Mobile number must contain only digits"
                isValid = false
            }
        }

        // Validate Date of Birth
        if (binding.etDateOfBirth.text.isNullOrBlank()) {
            binding.etDateOfBirth.error = "Date of birth is required"
            isValid = false
        }

        // Validate Gender
        if (binding.etGender.text.isNullOrBlank()) {
            binding.etGender.error = "Gender is required"
            isValid = false
        }

        // Validate Marital Status
        if (binding.etMaritalStatus.text.isNullOrBlank()) {
            binding.etMaritalStatus.error = "Marital status is required"
            isValid = false
        }

        // Validate Designation
        if (binding.etDesignation.text.isNullOrBlank()) {
            binding.etDesignation.error = "Designation is required"
            isValid = false
        }

        // Validate Department
        if (binding.etDepartment.text.isNullOrBlank()) {
            binding.etDepartment.error = "Department is required"
            isValid = false
        }

        // Validate Aadhaar Number
        val aadhaar = binding.etAadhaarNumber.text.toString().trim()
        when {
            aadhaar.isBlank() -> {
                binding.etAadhaarNumber.error = "Aadhaar number is required"
                isValid = false
            }
            aadhaar.length != 12 -> {
                binding.etAadhaarNumber.error = "Aadhaar must be exactly 12 digits"
                isValid = false
            }
            !aadhaar.all { it.isDigit() } -> {
                binding.etAadhaarNumber.error = "Aadhaar must contain only digits"
                isValid = false
            }
        }

        // Validate PAN Number
        val pan = binding.etPanNumber.text.toString().trim()
        val panPattern = Regex("[A-Z]{5}[0-9]{4}[A-Z]{1}")
        when {
            pan.isBlank() -> {
                binding.etPanNumber.error = "PAN number is required"
                isValid = false
            }
            pan.length != 10 -> {
                binding.etPanNumber.error = "PAN must be exactly 10 characters"
                isValid = false
            }
            !pan.matches(panPattern) -> {
                binding.etPanNumber.error = "Invalid PAN format (e.g., ABCDE1234F)"
                isValid = false
            }
        }

        // Validate Locality
        if (binding.etLocality.text.isNullOrBlank()) {
            binding.etLocality.error = "Locality is required"
            isValid = false
        }

        // Validate City
        if (binding.etCity.text.isNullOrBlank()) {
            binding.etCity.error = "City is required"
            isValid = false
        }

        // Validate State
        if (binding.etState.text.isNullOrBlank()) {
            binding.etState.error = "State is required"
            isValid = false
        }

        // Validate Pincode
        val pincode = binding.etPincode.text.toString().trim()
        when {
            pincode.isBlank() -> {
                binding.etPincode.error = "Pincode is required"
                isValid = false
            }
            pincode.length != 6 -> {
                binding.etPincode.error = "Pincode must be exactly 6 digits"
                isValid = false
            }
            !pincode.all { it.isDigit() } -> {
                binding.etPincode.error = "Pincode must contain only digits"
                isValid = false
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Please fix all errors before submitting", Toast.LENGTH_SHORT).show()
            
            // Scroll to top to show first error
            binding.root.scrollTo(0, 0)
        }

        return isValid
    }
    

    
    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::loadingDialog.isInitialized) {
            loadingDialog.destroy()
        }
        viewModel.clearUpdateState()
    }
}