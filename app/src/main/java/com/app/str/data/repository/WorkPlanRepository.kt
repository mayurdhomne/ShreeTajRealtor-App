package com.app.str.data.repository

import com.app.str.data.api.WorkPlanApiService
import com.app.str.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkPlanRepository @Inject constructor(
    private val apiService: WorkPlanApiService
) {
    
    private val gson = Gson()
    
    private fun parseErrorResponse(errorBody: String?): String {
        return try {
            if (errorBody.isNullOrEmpty()) {
                return "An unknown error occurred"
            }
            
            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
            
            when {
                !errorResponse.nonFieldErrors.isNullOrEmpty() -> 
                    errorResponse.nonFieldErrors.first()
                errorResponse.message != null -> 
                    errorResponse.message
                errorResponse.error != null -> 
                    errorResponse.error
                errorResponse.detail != null -> 
                    errorResponse.detail
                else -> 
                    "An unknown error occurred"
            }
        } catch (e: Exception) {
            "An error occurred while processing the response"
        }
    }
    
    suspend fun getAllWorkPlans(filter: String? = null, date: String? = null): Result<WorkPlansAllResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Always use the same endpoint which returns logged-in user's plans only
                val response = apiService.getAllWorkPlans(filter, date)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun createWorkPlan(request: CreateWorkPlanRequest): Result<WorkPlanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createWorkPlan(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun updateWorkPlan(id: Int, request: UpdateWorkPlanRequest): Result<WorkPlanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateWorkPlan(id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun deleteWorkPlan(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteWorkPlan(id)
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun getWorkPlanTitles(): Result<List<AvailableWorkTitle>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkPlanTitles()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    suspend fun getCoworkers(): Result<List<Coworker>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCoworkers()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.coworkers)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.Error(errorMessage)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}
