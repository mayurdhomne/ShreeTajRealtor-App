package com.app.str.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.str.data.model.ProfileResponse
import com.app.str.data.model.ProfileUpdateRequest
import com.app.str.data.model.ProfileUpdateResponse
import com.app.str.data.model.Result
import com.app.str.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _profileState = MutableStateFlow<Result<ProfileResponse>>(Result.Loading)
    val profileState: StateFlow<Result<ProfileResponse>> = _profileState.asStateFlow()
    
    private val _updateProfileState = MutableStateFlow<Result<ProfileUpdateResponse>?>(null)
    val updateProfileState: StateFlow<Result<ProfileUpdateResponse>?> = _updateProfileState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            profileRepository.getProfile().collect { result ->
                _profileState.value = result
                _isLoading.value = result is Result.Loading
            }
        }
    }
    
    fun updateProfile(request: ProfileUpdateRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            profileRepository.updateProfile(request).collect { result ->
                _updateProfileState.value = result
                _isLoading.value = result is Result.Loading
                
                // Refresh profile data after successful update
                if (result is Result.Success) {
                    loadProfile()
                }
            }
        }
    }
    
    fun clearUpdateState() {
        _updateProfileState.value = null
    }
}