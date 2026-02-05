package com.app.str.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.str.data.model.Result
import com.app.str.data.model.SalarySlipResponse
import com.app.str.data.repository.SalarySlipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalarySlipViewModel @Inject constructor(
    private val salarySlipRepository: SalarySlipRepository
) : ViewModel() {
    
    private val _salarySlipResult = MutableLiveData<Result<SalarySlipResponse>>()
    val salarySlipResult: LiveData<Result<SalarySlipResponse>> = _salarySlipResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun fetchSalarySlip(year: Int, month: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _salarySlipResult.value = Result.Loading
            
            try {
                val result = salarySlipRepository.getSalarySlip(year, month)
                _salarySlipResult.value = result
            } catch (e: Exception) {
                _salarySlipResult.value = Result.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}