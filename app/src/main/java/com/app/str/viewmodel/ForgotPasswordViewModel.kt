package com.app.str.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.str.data.model.*
import com.app.str.data.repository.ForgotPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordRepository: ForgotPasswordRepository
) : ViewModel() {
    
    // Current step in the forgot password flow
    private val _currentStep = MutableLiveData(ForgotPasswordStep.REQUEST_OTP)
    val currentStep: LiveData<ForgotPasswordStep> = _currentStep
    
    // Email stored throughout the flow
    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email
    
    // OTP stored for password reset
    private val _otp = MutableLiveData<String>()
    val otp: LiveData<String> = _otp
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Request OTP result
    private val _requestOtpResult = MutableLiveData<Result<ForgotPasswordRequestOtpResponse>>()
    val requestOtpResult: LiveData<Result<ForgotPasswordRequestOtpResponse>> = _requestOtpResult
    
    // Verify OTP result
    private val _verifyOtpResult = MutableLiveData<Result<ForgotPasswordVerifyOtpResponse>>()
    val verifyOtpResult: LiveData<Result<ForgotPasswordVerifyOtpResponse>> = _verifyOtpResult
    
    // Resend OTP result
    private val _resendOtpResult = MutableLiveData<Result<ForgotPasswordResendOtpResponse>>()
    val resendOtpResult: LiveData<Result<ForgotPasswordResendOtpResponse>> = _resendOtpResult
    
    // Reset password result
    private val _resetPasswordResult = MutableLiveData<Result<ForgotPasswordResetResponse>>()
    val resetPasswordResult: LiveData<Result<ForgotPasswordResetResponse>> = _resetPasswordResult
    
    // Validation errors
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    
    private val _otpError = MutableLiveData<String?>()
    val otpError: LiveData<String?> = _otpError
    
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError
    
    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError
    
    /**
     * Step 1: Request OTP for forgot password
     */
    fun requestOtp(email: String) {
        // Clear previous errors
        _emailError.value = null
        
        // Validate email
        if (!validateEmail(email)) {
            return
        }
        
        _email.value = email
        
        viewModelScope.launch {
            _isLoading.value = true
            _requestOtpResult.value = Result.Loading
            
            try {
                val request = ForgotPasswordRequestOtpRequest(email)
                val result = forgotPasswordRepository.requestOtp(request)
                _requestOtpResult.value = result
                
                if (result is Result.Success) {
                    // Move to verify OTP step
                    _currentStep.value = ForgotPasswordStep.VERIFY_OTP
                }
            } catch (e: Exception) {
                _requestOtpResult.value = Result.Error("Request OTP failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Step 2: Verify OTP
     */
    fun verifyOtp(otp: String) {
        // Clear previous errors
        _otpError.value = null
        
        // Validate OTP
        if (!validateOtp(otp)) {
            return
        }
        
        val email = _email.value ?: return
        _otp.value = otp
        
        viewModelScope.launch {
            _isLoading.value = true
            _verifyOtpResult.value = Result.Loading
            
            try {
                val request = ForgotPasswordVerifyOtpRequest(email, otp)
                val result = forgotPasswordRepository.verifyOtp(request)
                _verifyOtpResult.value = result
                
                if (result is Result.Success) {
                    // Move to reset password step
                    _currentStep.value = ForgotPasswordStep.RESET_PASSWORD
                }
            } catch (e: Exception) {
                _verifyOtpResult.value = Result.Error("OTP verification failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Resend OTP
     */
    fun resendOtp() {
        val email = _email.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _resendOtpResult.value = Result.Loading
            
            try {
                val request = ForgotPasswordResendOtpRequest(email)
                val result = forgotPasswordRepository.resendOtp(request)
                _resendOtpResult.value = result
            } catch (e: Exception) {
                _resendOtpResult.value = Result.Error("Resend OTP failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Step 3: Reset password
     */
    fun resetPassword(newPassword: String, confirmPassword: String) {
        // Clear previous errors
        _passwordError.value = null
        _confirmPasswordError.value = null
        
        // Validate passwords
        if (!validatePasswords(newPassword, confirmPassword)) {
            return
        }
        
        val email = _email.value ?: return
        val otp = _otp.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _resetPasswordResult.value = Result.Loading
            
            try {
                val request = ForgotPasswordResetRequest(email, otp, newPassword, confirmPassword)
                val result = forgotPasswordRepository.resetPassword(request)
                _resetPasswordResult.value = result
            } catch (e: Exception) {
                _resetPasswordResult.value = Result.Error("Password reset failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Go back to previous step
     */
    fun goToPreviousStep(): Boolean {
        return when (_currentStep.value) {
            ForgotPasswordStep.VERIFY_OTP -> {
                _currentStep.value = ForgotPasswordStep.REQUEST_OTP
                true
            }
            ForgotPasswordStep.RESET_PASSWORD -> {
                _currentStep.value = ForgotPasswordStep.VERIFY_OTP
                true
            }
            else -> false
        }
    }
    
    /**
     * Reset the entire flow
     */
    fun resetFlow() {
        _currentStep.value = ForgotPasswordStep.REQUEST_OTP
        _email.value = null
        _otp.value = null
        _emailError.value = null
        _otpError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
    }
    
    // Validation methods
    
    private fun validateEmail(email: String): Boolean {
        return when {
            email.isBlank() -> {
                _emailError.value = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _emailError.value = "Invalid email format"
                false
            }
            else -> true
        }
    }
    
    private fun validateOtp(otp: String): Boolean {
        return when {
            otp.isBlank() -> {
                _otpError.value = "OTP is required"
                false
            }
            otp.length != 6 -> {
                _otpError.value = "OTP must be 6 digits"
                false
            }
            !otp.all { it.isDigit() } -> {
                _otpError.value = "OTP must contain only digits"
                false
            }
            else -> true
        }
    }
    
    private fun validatePasswords(password: String, confirmPassword: String): Boolean {
        var isValid = true
        
        // Password strength validation
        when {
            password.isBlank() -> {
                _passwordError.value = "Password is required"
                isValid = false
            }
            password.length < 8 -> {
                _passwordError.value = "Password must be at least 8 characters"
                isValid = false
            }
            !password.any { it.isUpperCase() } -> {
                _passwordError.value = "Password must contain at least one uppercase letter"
                isValid = false
            }
            !password.any { it.isLowerCase() } -> {
                _passwordError.value = "Password must contain at least one lowercase letter"
                isValid = false
            }
            !password.any { it.isDigit() } -> {
                _passwordError.value = "Password must contain at least one digit"
                isValid = false
            }
            !password.any { !it.isLetterOrDigit() } -> {
                _passwordError.value = "Password must contain at least one special character"
                isValid = false
            }
        }
        
        // Confirm password validation
        when {
            confirmPassword.isBlank() -> {
                _confirmPasswordError.value = "Confirm password is required"
                isValid = false
            }
            password != confirmPassword -> {
                _confirmPasswordError.value = "Passwords do not match"
                isValid = false
            }
        }
        
        return isValid
    }
    
    /**
     * Clear email error
     */
    fun clearEmailError() {
        _emailError.value = null
    }
    
    /**
     * Clear OTP error
     */
    fun clearOtpError() {
        _otpError.value = null
    }
    
    /**
     * Clear password errors
     */
    fun clearPasswordErrors() {
        _passwordError.value = null
        _confirmPasswordError.value = null
    }
}
