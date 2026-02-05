package com.app.str.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.str.R
import com.app.str.data.model.ProfileCompletionRequest
import com.app.str.data.model.Result
import com.app.str.databinding.FragmentProfileBinding
import com.app.str.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDropdowns()
        setupDatePicker()
        setupTextWatchers()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupDropdowns() {
        // Gender dropdown
        val genderOptions = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.etGender.setAdapter(genderAdapter)
        
        // Marital Status dropdown
        val maritalOptions = arrayOf("Single", "Married", "Divorced", "Widowed")
        val maritalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, maritalOptions)
        binding.etMaritalStatus.setAdapter(maritalAdapter)
    }
    
    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val maxDate = Calendar.getInstance()
            maxDate.add(Calendar.YEAR, -18) // Minimum 18 years old
            
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.etDateOfBirth.setText(dateFormatter.format(calendar.time))
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
    
    private fun setupObservers() {
        viewModel.profileCompletionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    showSuccessMessage(result.data.message)
                    // Navigate to dashboard or main screen after successful profile completion
                    // findNavController().navigate(R.id.action_profileFragment_to_dashboardActivity)
                }
                is Result.Error -> {
                    showLoading(false)
                    showErrorMessage(result.message)
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            btnSubmit.isEnabled = !isLoading
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            if (isLoading) {
                btnSubmit.text = "Submitting..."
                btnSubmit.icon = null
            } else {
                btnSubmit.text = "Complete Profile"
                btnSubmit.setIconResource(R.drawable.ic_arrow_forward)
            }
        }
    }
    
    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.primary_burgundy))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
    }
    
    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
    }
    
    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            if (validateAllFields()) {
                submitProfile()
            }
        }
    }
    
    private fun submitProfile() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val designation = binding.etDesignation.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        val mobileNumber = binding.etMobileNumber.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val gender = binding.etGender.text.toString().trim().lowercase()
        val maritalStatus = binding.etMaritalStatus.text.toString().trim().lowercase()
        val aadhaarNumber = binding.etAadhaarNumber.text.toString().trim()
        val panNumber = binding.etPanNumber.text.toString().trim()
        val locality = binding.etLocality.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()
        
        val request = ProfileCompletionRequest(
            firstName, lastName, designation, department, mobileNumber,
            dateOfBirth, gender, maritalStatus, aadhaarNumber, panNumber,
            locality, city, state, pincode
        )
        
        viewModel.completeProfile(request)
    }
    
    private fun validateAllFields(): Boolean {
        var isValid = true
        
        // Validate First Name
        if (binding.etFirstName.text.isNullOrBlank()) {
            binding.etFirstName.error = "First name is required"
            isValid = false
        }
        
        // Validate Last Name
        if (binding.etLastName.text.isNullOrBlank()) {
            binding.etLastName.error = "Last name is required"
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
                binding.etMobileNumber.error = "Invalid mobile number"
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
                binding.etAadhaarNumber.error = "Aadhaar must be 12 digits"
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
                binding.etPanNumber.error = "PAN must be 10 characters"
                isValid = false
            }
            !pan.matches(panPattern) -> {
                binding.etPanNumber.error = "Invalid PAN format"
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
                binding.etPincode.error = "Pincode must be 6 digits"
                isValid = false
            }
        }
        
        if (!isValid) {
            showErrorMessage("Please fill all required fields correctly")
        }
        
        return isValid
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}