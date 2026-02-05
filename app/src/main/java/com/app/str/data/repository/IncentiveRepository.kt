package com.app.str.data.repository

import com.app.str.data.api.IncentiveApiService
import com.app.str.data.model.IncentiveResponse
import com.app.str.data.model.Result
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncentiveRepository @Inject constructor(
    private val apiService: IncentiveApiService
) {
    
    private val gson = Gson()
    
    suspend fun getMyIncentives(): Result<List<IncentiveResponse>> {
        return try {
            val response = apiService.getMyIncentives()
            if (response.isSuccessful) {
                response.body()?.let { incentives ->
                    Result.Success(incentives)
                } ?: Result.Error("Empty response body")
            } else {
                val errorMessage = "Failed to fetch incentives: ${response.message()}"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}", e)
        }
    }
}
