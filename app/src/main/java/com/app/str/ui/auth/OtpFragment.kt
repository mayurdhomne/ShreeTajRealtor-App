package com.app.str.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.str.R
import com.app.str.data.model.OtpRequest
import com.app.str.data.model.ResendOtpRequest
import com.app.str.data.model.Result
import com.app.str.databinding.FragmentOtpBinding
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtpFragment : Fragment() {
    
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private val args: OtpFragmentArgs by navArgs()
    private lateinit var loadingDialog: LoadingDialog
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadingDialog = LoadingDialog(requireContext())
        
        // Store the email in the hidden field and update message
        args.email?.let { email ->
            binding.etEmail.text = email
            binding.tvOtpMessage.text = "An code has been sent to your email address: $email"
        }
        
        setupOtpInputs()
        setupOtpCountdown()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupOtpInputs() {
        // Set up automatic focus change between OTP fields
        binding.etOtp1.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    binding.etOtp2.requestFocus()
                }
            }
        })
        
        binding.etOtp2.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    binding.etOtp3.requestFocus()
                } else if (s?.isEmpty() == true) {
                    binding.etOtp1.requestFocus()
                }
            }
        })
        
        binding.etOtp3.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    binding.etOtp4.requestFocus()
                } else if (s?.isEmpty() == true) {
                    binding.etOtp2.requestFocus()
                }
            }
        })
        
        binding.etOtp4.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    binding.etOtp5.requestFocus()
                } else if (s?.isEmpty() == true) {
                    binding.etOtp3.requestFocus()
                }
            }
        })
        
        binding.etOtp5.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    binding.etOtp6.requestFocus()
                } else if (s?.isEmpty() == true) {
                    binding.etOtp4.requestFocus()
                }
            }
        })
        
        binding.etOtp6.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isEmpty() == true) {
                    binding.etOtp5.requestFocus()
                } else if (s?.length == 1) {
                    // All OTP digits entered, hide keyboard
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etOtp6.windowToken, 0)
                }
            }
        })
        
        // Set up key listener for backspace
        val onKeyListener = View.OnKeyListener { v, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                val editText = v as android.widget.EditText
                if (editText.text.isEmpty()) {
                    when (editText.id) {
                        R.id.etOtp2 -> binding.etOtp1.requestFocus()
                        R.id.etOtp3 -> binding.etOtp2.requestFocus()
                        R.id.etOtp4 -> binding.etOtp3.requestFocus()
                        R.id.etOtp5 -> binding.etOtp4.requestFocus()
                        R.id.etOtp6 -> binding.etOtp5.requestFocus()
                    }
                    return@OnKeyListener true
                }
            }
            false
        }
        
        binding.etOtp2.setOnKeyListener(onKeyListener)
        binding.etOtp3.setOnKeyListener(onKeyListener)
        binding.etOtp4.setOnKeyListener(onKeyListener)
        binding.etOtp5.setOnKeyListener(onKeyListener)
        binding.etOtp6.setOnKeyListener(onKeyListener)
    }
    
    private fun setupOtpCountdown() {
        // In a real implementation, this would start a countdown timer from 59 seconds
        // For demonstration purposes, we'll implement a simple countdown timer
        
        val countdownTextView = binding.btnResend
        
        // Using Handler and Runnable for a simple countdown
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var timeRemaining = 59 // 59 seconds
        
        val runnable = object : Runnable {
            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun run() {
                if (timeRemaining > 0) {
                    countdownTextView.text = String.format("%02d:%02ds", 0, timeRemaining)
                    timeRemaining--
                    handler.postDelayed(this, 1000)
                } else {
                    // Timer finished, enable resend button
                    countdownTextView.text = "Resend OTP"
                    countdownTextView.isEnabled = true
                }
            }
        }
        // Start the countdown
        handler.post(runnable)
    }
    
    private fun setupObservers() {
        viewModel.otpVerificationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    loadingDialog.show("Verifying OTP...")
                    binding.btnVerify.isEnabled = false
                }
                is Result.Success -> {
                    loadingDialog.updateMessage("Verification successful!")
                    binding.btnVerify.isEnabled = true
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        // Navigate to login screen after successful verification
                        findNavController().popBackStack(R.id.loginFragment, false)
                    }, 500)
                }
                is Result.Error -> {
                    loadingDialog.dismiss()
                    binding.btnVerify.isEnabled = true
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.resendOtpResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    loadingDialog.show("Resending OTP...")
                    binding.btnResend.isEnabled = false
                }
                is Result.Success -> {
                    loadingDialog.dismiss()
                    binding.btnResend.isEnabled = true
                    Toast.makeText(context, result.data.message, Toast.LENGTH_SHORT).show()
                    // Restart countdown timer after resending
                    setupOtpCountdown()
                }
                is Result.Error -> {
                    loadingDialog.dismiss()
                    binding.btnResend.isEnabled = true
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnVerify.setOnClickListener {
            // Collect the OTP from the 6 separate boxes
            val email = args.email ?: ""
            val otp1 = binding.etOtp1.text.toString()
            val otp2 = binding.etOtp2.text.toString()
            val otp3 = binding.etOtp3.text.toString()
            val otp4 = binding.etOtp4.text.toString()
            val otp5 = binding.etOtp5.text.toString()
            val otp6 = binding.etOtp6.text.toString()
            val otp = otp1 + otp2 + otp3 + otp4 + otp5 + otp6
            
            if (validateOtpInput(email, otp)) {
                val request = OtpRequest(email, otp)
                viewModel.verifyOtp(request)
            }
        }
        
        binding.btnResend.setOnClickListener {
            val email = args.email ?: ""
            if (email.isNotEmpty()) {
                val request = ResendOtpRequest(email)
                viewModel.resendOtp(request)
            } else {
                Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun validateOtpInput(email: String, otp: String): Boolean {
        return when {
            email.isEmpty() -> {
                Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                false
            }
            otp.isEmpty() -> {
                Toast.makeText(context, "OTP is required", Toast.LENGTH_SHORT).show()
                false
            }
            otp.length != 6 -> {
                Toast.makeText(context, "Please enter all 6 digits of the OTP", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.destroy()
        _binding = null
    }
}