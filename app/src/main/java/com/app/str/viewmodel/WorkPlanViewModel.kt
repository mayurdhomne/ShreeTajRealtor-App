package com.app.str.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.str.data.model.*
import com.app.str.data.repository.WorkPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkPlanViewModel @Inject constructor(
    private val workPlanRepository: WorkPlanRepository,
    private val authManager: com.app.str.utils.AuthManager
) : ViewModel() {
    
    private val _workPlansState = MutableLiveData<WorkPlanState>()
    val workPlansState: LiveData<WorkPlanState> = _workPlansState
    
    private val _createPlanState = MutableLiveData<CreatePlanState>()
    val createPlanState: LiveData<CreatePlanState> = _createPlanState
    
    private val _updatePlanState = MutableLiveData<UpdatePlanState>()
    val updatePlanState: LiveData<UpdatePlanState> = _updatePlanState
    
    private val _deletePlanState = MutableLiveData<DeletePlanState>()
    val deletePlanState: LiveData<DeletePlanState> = _deletePlanState
    
    private val _allPlans = MutableLiveData<List<WorkPlanItem>>()
    val allPlans: LiveData<List<WorkPlanItem>> = _allPlans
    
    private val _userPlans = MutableLiveData<List<WorkPlanItem>>()
    val userPlans: LiveData<List<WorkPlanItem>> = _userPlans
    
    private val _adminPlans = MutableLiveData<List<WorkPlanItem>>()
    val adminPlans: LiveData<List<WorkPlanItem>> = _adminPlans
    
    private val _workPlanTitles = MutableLiveData<List<AvailableWorkTitle>>()
    val workPlanTitles: LiveData<List<AvailableWorkTitle>> = _workPlanTitles
    
    private val _coworkers = MutableLiveData<List<Coworker>>()
    val coworkers: LiveData<List<Coworker>> = _coworkers
    
    // Current filter state
    private var currentFilter: String? = null
    private var currentDate: String? = null
    
    fun loadWorkPlans(filter: String? = null, date: String? = null) {
        currentFilter = filter
        currentDate = date
        viewModelScope.launch {
            _workPlansState.value = WorkPlanState.Loading
            
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _workPlansState.value = WorkPlanState.Error("Authentication required. Please login.")
                return@launch
            }
            
            when (val result = workPlanRepository.getAllWorkPlans(filter, date)) {
                is Result.Success -> {
                    val response = result.data
                    val allPlansList = response.data.userCreated + response.data.adminCreated
                    
                    // Debug logging
                    android.util.Log.d("WorkPlanViewModel", "Filter applied: $filter, Date: $date")
                    android.util.Log.d("WorkPlanViewModel", "Total plans: ${allPlansList.size}")
                    android.util.Log.d("WorkPlanViewModel", "User created: ${response.data.userCreated.size}")
                    android.util.Log.d("WorkPlanViewModel", "Admin created: ${response.data.adminCreated.size}")
                    
                    response.data.adminCreated.forEach { plan ->
                        android.util.Log.d("WorkPlanViewModel", "Admin Plan ID: ${plan.id}, Coworkers: ${plan.coworkers.size}")
                        plan.coworkers.forEach { coworker ->
                            android.util.Log.d("WorkPlanViewModel", "  - Coworker: ${coworker.username}, Email: ${coworker.email}")
                        }
                    }
                    
                    _allPlans.value = allPlansList
                    _userPlans.value = response.data.userCreated
                    _adminPlans.value = response.data.adminCreated
                    
                    _workPlansState.value = WorkPlanState.Success(response)
                }
                is Result.Error -> {
                    android.util.Log.e("WorkPlanViewModel", "Error loading work plans: ${result.message}")
                    _workPlansState.value = WorkPlanState.Error(result.message)
                }
                else -> {
                    // Handle other cases if needed
                }
            }
        }
    }
    
    fun getCurrentFilter(): String? = currentFilter
    fun getCurrentDate(): String? = currentDate
    
    fun createWorkPlan(request: CreateWorkPlanRequest) {
        viewModelScope.launch {
            _createPlanState.value = CreatePlanState.Loading
            
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _createPlanState.value = CreatePlanState.Error("Authentication required. Please login.")
                return@launch
            }
            
            when (val result = workPlanRepository.createWorkPlan(request)) {
                is Result.Success -> {
                    _createPlanState.value = CreatePlanState.Success(result.data)
                    // Reload work plans after creation
                    loadWorkPlans()
                }
                is Result.Error -> {
                    _createPlanState.value = CreatePlanState.Error(result.message)
                }
                else -> {
                    // Handle other cases if needed
                }
            }
        }
    }
    
    fun updateWorkPlan(id: Int, request: UpdateWorkPlanRequest) {
        viewModelScope.launch {
            _updatePlanState.value = UpdatePlanState.Loading
            
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _updatePlanState.value = UpdatePlanState.Error("Authentication required. Please login.")
                return@launch
            }
            
            when (val result = workPlanRepository.updateWorkPlan(id, request)) {
                is Result.Success -> {
                    _updatePlanState.value = UpdatePlanState.Success(result.data)
                    // Reload work plans after update
                    loadWorkPlans()
                }
                is Result.Error -> {
                    _updatePlanState.value = UpdatePlanState.Error(result.message)
                }
                else -> {
                    // Handle other cases if needed
                }
            }
        }
    }
    
    fun deleteWorkPlan(id: Int) {
        viewModelScope.launch {
            _deletePlanState.value = DeletePlanState.Loading
            
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _deletePlanState.value = DeletePlanState.Error("Authentication required. Please login.")
                return@launch
            }
            
            when (val result = workPlanRepository.deleteWorkPlan(id)) {
                is Result.Success -> {
                    _deletePlanState.value = DeletePlanState.Success
                    // Reload work plans after deletion
                    loadWorkPlans()
                }
                is Result.Error -> {
                    _deletePlanState.value = DeletePlanState.Error(result.message)
                }
                else -> {
                    // Handle other cases if needed
                }
            }
        }
    }
    
    fun resetCreatePlanState() {
        _createPlanState.value = CreatePlanState.Idle
    }
    
    fun resetUpdatePlanState() {
        _updatePlanState.value = UpdatePlanState.Idle
    }
    
    fun resetDeletePlanState() {
        _deletePlanState.value = DeletePlanState.Idle
    }
    
    fun loadWorkPlanTitles() {
        viewModelScope.launch {
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _workPlanTitles.value = emptyList()
                return@launch
            }
            
            when (val result = workPlanRepository.getWorkPlanTitles()) {
                is Result.Success -> {
                    val titles = result.data
                    _workPlanTitles.value = titles
                    android.util.Log.d("WorkPlanViewModel", "Loaded ${titles.size} work plan titles")
                }
                is Result.Error -> {
                    android.util.Log.e("WorkPlanViewModel", "Error loading work plan titles: ${result.message}")
                    _workPlanTitles.value = emptyList()
                }
                else -> {
                    _workPlanTitles.value = emptyList()
                }
            }
        }
    }
    
    fun loadCoworkers() {
        viewModelScope.launch {
            // Check authentication before making API call
            if (!authManager.hasStoredTokens()) {
                _coworkers.value = emptyList()
                return@launch
            }
            
            when (val result = workPlanRepository.getCoworkers()) {
                is Result.Success -> {
                    val coworkersList = result.data
                    _coworkers.value = coworkersList
                    android.util.Log.d("WorkPlanViewModel", "Loaded ${coworkersList.size} coworkers")
                }
                is Result.Error -> {
                    android.util.Log.e("WorkPlanViewModel", "Error loading coworkers: ${result.message}")
                    _coworkers.value = emptyList()
                }
                else -> {
                    _coworkers.value = emptyList()
                }
            }
        }
    }
}

sealed class WorkPlanState {
    object Idle : WorkPlanState()
    object Loading : WorkPlanState()
    data class Success(val response: WorkPlansAllResponse) : WorkPlanState()
    data class Error(val message: String) : WorkPlanState()
}

sealed class CreatePlanState {
    object Idle : CreatePlanState()
    object Loading : CreatePlanState()
    data class Success(val response: WorkPlanResponse) : CreatePlanState()
    data class Error(val message: String) : CreatePlanState()
}

sealed class UpdatePlanState {
    object Idle : UpdatePlanState()
    object Loading : UpdatePlanState()
    data class Success(val response: WorkPlanResponse) : UpdatePlanState()
    data class Error(val message: String) : UpdatePlanState()
}

sealed class DeletePlanState {
    object Idle : DeletePlanState()
    object Loading : DeletePlanState()
    object Success : DeletePlanState()
    data class Error(val message: String) : DeletePlanState()
}
