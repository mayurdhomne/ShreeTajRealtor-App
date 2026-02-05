package com.app.str.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.app.str.data.model.*
import com.app.str.data.repository.AuthRepository
import com.app.str.utils.AuthManager
import com.app.str.utils.AuthState
import com.app.str.utils.NavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authManager: AuthManager,
    private val profileRepository: com.app.str.data.repository.ProfileRepository
) : ViewModel() {
    
    // Auth state from AuthManager
    val authState: StateFlow<AuthState> = authManager.authState
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    val navigationEvents: SharedFlow<NavigationEvent> = authManager.navigationEvents
    
    // Convert StateFlow to LiveData for legacy components
    val authStateLiveData: LiveData<AuthState> = authState.asLiveData()
    val isLoggedInLiveData: LiveData<Boolean> = isLoggedIn.asLiveData()
    
    private val _signUpResult = MutableLiveData<Result<SignUpResponse>>()
    val signUpResult: LiveData<Result<SignUpResponse>> = _signUpResult
    
    private val _otpVerificationResult = MutableLiveData<Result<SignUpResponse>>()
    val otpVerificationResult: LiveData<Result<SignUpResponse>> = _otpVerificationResult
    
    private val _resendOtpResult = MutableLiveData<Result<ResendOtpResponse>>()
    val resendOtpResult: LiveData<Result<ResendOtpResponse>> = _resendOtpResult
    
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult
    
    private val _loginNavigationResult = MutableLiveData<Result<LoginNavigationResult>>()
    val loginNavigationResult: LiveData<Result<LoginNavigationResult>> = _loginNavigationResult
    
    private val _profileCompletionResult = MutableLiveData<Result<ProfileCompletionResponse>>()
    val profileCompletionResult: LiveData<Result<ProfileCompletionResponse>> = _profileCompletionResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        // Check authentication status when ViewModel is created
        checkAuthenticationStatus()
    }
    
    fun signUp(request: SignUpRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _signUpResult.value = Result.Loading
            
            try {
                val result = authRepository.signUp(request)
                _signUpResult.value = result
            } catch (e: Exception) {
                _signUpResult.value = Result.Error("Sign up failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun verifyOtp(request: OtpRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _otpVerificationResult.value = Result.Loading
            
            try {
                val result = authRepository.verifyOtp(request)
                _otpVerificationResult.value = result
            } catch (e: Exception) {
                _otpVerificationResult.value = Result.Error("OTP verification failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resendOtp(request: ResendOtpRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _resendOtpResult.value = Result.Loading
            
            try {
                val result = authRepository.resendOtp(request)
                _resendOtpResult.value = result
            } catch (e: Exception) {
                _resendOtpResult.value = Result.Error("Resend OTP failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = Result.Loading
            _loginNavigationResult.value = Result.Loading
            
            try {
                val result = authRepository.login(request)
                _loginResult.value = result
                println("AuthViewModel: Login result: $result")
                
                when (result) {
                    is Result.Success -> {
                        println("AuthViewModel: Login successful, checking profile status...")
                        
                        // Check profile completion status
                        val profileComplete = try {
                            profileRepository.isProfileComplete()
                        } catch (e: Exception) {
                            println("AuthViewModel: Error checking profile status: ${e.message}")
                            false
                        }
                        
                        println("AuthViewModel: Profile complete status: $profileComplete")
                        
                        val navigationResult = LoginNavigationResult(
                            loginResponse = result.data,
                            shouldNavigateToDashboard = profileComplete,
                            profileComplete = profileComplete
                        )
                        
                        _loginNavigationResult.value = Result.Success(navigationResult)
                    }
                    is Result.Error -> {
                        _loginNavigationResult.value = Result.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Keep loading state
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Login failed: ${e.message}"
                _loginResult.value = Result.Error(errorMsg)
                _loginNavigationResult.value = Result.Error(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun completeProfile(request: ProfileCompletionRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _profileCompletionResult.value = Result.Loading
            
            try {
                val result = authRepository.completeProfile(request)
                _profileCompletionResult.value = result
            } catch (e: Exception) {
                _profileCompletionResult.value = Result.Error("Profile completion failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                authManager.logout()
            } catch (e: Exception) {
                // Handle logout error if needed
            }
        }
    }
    
    fun checkAuthenticationStatus() {
        viewModelScope.launch {
            try {
                authManager.checkAuthenticationStatus()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    fun clearTokens() {
        viewModelScope.launch {
            try {
                authRepository.clearTokens()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return authManager.getCurrentUserId()
    }
    
    /**
     * Check if current session is valid (synchronous for splash screen)
     */
    fun isSessionValid(): Boolean {
        return runBlocking {
            authManager.isSessionValid()
        }
    }
    
    /**
     * Check if current session is valid (suspend version for coroutines)
     */
    suspend fun checkSessionValid(): Boolean {
        return authManager.isSessionValid()
    }
    
    /**
     * Check if user profile is complete by fetching profile and checking fields
     * Returns true if complete, false if incomplete
     */
    suspend fun checkProfileStatus(): Boolean {
        return try {
            profileRepository.isProfileComplete()
        } catch (e: Exception) {
            println("AuthViewModel: Error checking profile status: ${e.message}")
            false
        }
    }
}