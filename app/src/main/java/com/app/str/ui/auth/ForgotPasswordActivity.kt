package com.app.str.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.app.str.R
import com.app.str.data.model.ForgotPasswordStep
import com.app.str.data.model.Result
import com.app.str.databinding.ActivityForgotPasswordBinding
import com.app.str.utils.LoadingDialog
import com.app.str.utils.applyTopPadding
import com.app.str.viewmodel.ForgotPasswordViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    
    private var resendTimer: CountDownTimer? = null
    private var isResendEnabled = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Apply edge-to-edge padding
        binding.root.applyTopPadding()
        
        loadingDialog = LoadingDialog(this)
        
        setupBackPressHandler()
        setupObservers()
        setupClickListeners()
        setupOtpInputs()
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }
    
    private fun handleBackPress() {
        // Try to go to previous step, if not possible, finish activity
        if (!viewModel.goToPreviousStep()) {
            finish()
        }
    }
    
    private fun setupObservers() {
        // Observe current step changes
        viewModel.currentStep.observe(this) { step ->
            updateUIForStep(step)
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                loadingDialog.show(getLoadingMessage())
            } else {
                loadingDialog.dismiss()
            }
            updateButtonStates(!isLoading)
        }
        
        // Observe request OTP result
        viewModel.requestOtpResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    showSnackbar(result.data.message, isSuccess = true)
                    startResendTimer()
                }
                is Result.Error -> {
                    showSnackbar(result.message, isSuccess = false)
                }
                is Result.Loading -> {
                    // Loading handled by isLoading observer
                }
            }
        }
        
        // Observe verify OTP result
        viewModel.verifyOtpResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    showSnackbar(result.data.message, isSuccess = true)
                }
                is Result.Error -> {
                    showSnackbar(result.message, isSuccess = false)
                    clearOtpFields()
                }
                is Result.Loading -> {
                    // Loading handled by isLoading observer
                }
            }
        }
        
        // Observe resend OTP result
        viewModel.resendOtpResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    showSnackbar(result.data.message, isSuccess = true)
                    startResendTimer()
                    clearOtpFields()
                }
                is Result.Error -> {
                    showSnackbar(result.message, isSuccess = false)
                    isResendEnabled = true
                    updateResendButtonState()
                }
                is Result.Loading -> {
                    // Loading handled by isLoading observer
                }
            }
        }
        
        // Observe reset password result
        viewModel.resetPasswordResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this, getString(R.string.password_reset_success), Toast.LENGTH_LONG).show()
                    finish() // Navigate back to login
                }
                is Result.Error -> {
                    showSnackbar(result.message, isSuccess = false)
                }
                is Result.Loading -> {
                    // Loading handled by isLoading observer
                }
            }
        }
        
        // Observe validation errors
        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error
        }
        
        viewModel.otpError.observe(this) { error ->
            if (error != null) {
                showSnackbar(error, isSuccess = false)
            }
        }
        
        viewModel.passwordError.observe(this) { error ->
            binding.tilNewPassword.error = error
        }
        
        viewModel.confirmPasswordError.observe(this) { error ->
            binding.tilConfirmPassword.error = error
        }
        
        // Observe email for display in OTP step
        viewModel.email.observe(this) { email ->
            email?.let {
                binding.tvOtpMessage.text = getString(R.string.otp_sent_message, it)
            }
        }
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            handleBackPress()
        }
        
        // Step 1: Submit email
        binding.btnSubmitEmail.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.requestOtp(email)
        }
        
        // Step 2: Verify OTP
        binding.btnVerifyOtp.setOnClickListener {
            val otp = getOtpFromFields()
            viewModel.verifyOtp(otp)
        }
        
        // Resend OTP
        binding.tvResendOtp.setOnClickListener {
            if (isResendEnabled) {
                viewModel.resendOtp()
            }
        }
        
        // Step 3: Reset Password
        binding.btnResetPassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            viewModel.resetPassword(newPassword, confirmPassword)
        }
        
        // Clear errors on text change
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearEmailError()
        }
        
        binding.etNewPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearPasswordErrors()
        }
        
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearPasswordErrors()
        }
    }
    
    private fun setupOtpInputs() {
        // Set up automatic focus change between OTP fields
        val otpFields = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )
        
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.clearOtpError()
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    } else if (s?.isEmpty() == true && i > 0) {
                        otpFields[i - 1].requestFocus()
                    } else if (s?.length == 1 && i == otpFields.size - 1) {
                        // Last digit entered, hide keyboard
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(otpFields[i].windowToken, 0)
                    }
                }
            })
            
            // Handle backspace on empty field
            otpFields[i].setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN && 
                    keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                    otpFields[i].text.isEmpty() && i > 0) {
                    otpFields[i - 1].requestFocus()
                    otpFields[i - 1].text.clear()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }
    
    private fun updateUIForStep(step: ForgotPasswordStep) {
        when (step) {
            ForgotPasswordStep.REQUEST_OTP -> {
                binding.layoutRequestOtp.visibility = View.VISIBLE
                binding.layoutVerifyOtp.visibility = View.GONE
                binding.layoutResetPassword.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.forgot_password_title)
                binding.tvSubtitle.text = getString(R.string.forgot_password_subtitle)
            }
            ForgotPasswordStep.VERIFY_OTP -> {
                binding.layoutRequestOtp.visibility = View.GONE
                binding.layoutVerifyOtp.visibility = View.VISIBLE
                binding.layoutResetPassword.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.verify_otp_title)
                binding.tvSubtitle.text = getString(R.string.verify_otp_subtitle)
                binding.etOtp1.requestFocus()
            }
            ForgotPasswordStep.RESET_PASSWORD -> {
                binding.layoutRequestOtp.visibility = View.GONE
                binding.layoutVerifyOtp.visibility = View.GONE
                binding.layoutResetPassword.visibility = View.VISIBLE
                binding.tvTitle.text = getString(R.string.reset_password_title)
                binding.tvSubtitle.text = getString(R.string.reset_password_subtitle)
                binding.etNewPassword.requestFocus()
                cancelResendTimer()
            }
        }
    }
    
    private fun updateButtonStates(enabled: Boolean) {
        binding.btnSubmitEmail.isEnabled = enabled
        binding.btnVerifyOtp.isEnabled = enabled
        binding.btnResetPassword.isEnabled = enabled
    }
    
    private fun getLoadingMessage(): String {
        return when (viewModel.currentStep.value) {
            ForgotPasswordStep.REQUEST_OTP -> getString(R.string.sending_otp)
            ForgotPasswordStep.VERIFY_OTP -> getString(R.string.verifying_otp)
            ForgotPasswordStep.RESET_PASSWORD -> getString(R.string.resetting_password)
            else -> getString(R.string.please_wait)
        }
    }
    
    private fun getOtpFromFields(): String {
        return buildString {
            append(binding.etOtp1.text.toString())
            append(binding.etOtp2.text.toString())
            append(binding.etOtp3.text.toString())
            append(binding.etOtp4.text.toString())
            append(binding.etOtp5.text.toString())
            append(binding.etOtp6.text.toString())
        }
    }
    
    private fun clearOtpFields() {
        binding.etOtp1.text.clear()
        binding.etOtp2.text.clear()
        binding.etOtp3.text.clear()
        binding.etOtp4.text.clear()
        binding.etOtp5.text.clear()
        binding.etOtp6.text.clear()
        binding.etOtp1.requestFocus()
    }
    
    private fun startResendTimer() {
        isResendEnabled = false
        updateResendButtonState()
        
        resendTimer = object : CountDownTimer(RESEND_TIMER_DURATION, RESEND_TIMER_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.tvResendOtp.text = getString(R.string.resend_otp_timer, seconds)
            }
            
            override fun onFinish() {
                isResendEnabled = true
                updateResendButtonState()
                binding.tvResendOtp.text = getString(R.string.resend_otp)
            }
        }.start()
    }
    
    private fun cancelResendTimer() {
        resendTimer?.cancel()
        resendTimer = null
    }
    
    private fun updateResendButtonState() {
        binding.tvResendOtp.isEnabled = isResendEnabled
        binding.tvResendOtp.alpha = if (isResendEnabled) 1.0f else 0.5f
    }
    
    private fun showSnackbar(message: String, isSuccess: Boolean) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isSuccess) {
            snackbar.setBackgroundTint(getColor(R.color.success_green))
        } else {
            snackbar.setBackgroundTint(getColor(R.color.error_red))
        }
        snackbar.setTextColor(getColor(R.color.white))
        snackbar.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelResendTimer()
        loadingDialog.destroy()
    }
    
    companion object {
        private const val RESEND_TIMER_DURATION = 60000L // 60 seconds
        private const val RESEND_TIMER_INTERVAL = 1000L // 1 second
    }
}
