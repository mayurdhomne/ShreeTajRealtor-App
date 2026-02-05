package com.app.str.data.repository

import com.app.str.data.api.SalarySlipApiService
import com.app.str.data.model.Result
import com.app.str.data.model.SalarySlipResponse
import com.google.gson.Gson
import com.app.str.data.model.ErrorResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalarySlipRepository @Inject constructor(
    private val apiService: SalarySlipApiService
) {
    
    private val gson = Gson()
    
    suspend fun getSalarySlip(year: Int, month: Int): Result<SalarySlipResponse> {
        return try {
            val response = apiService.getSalarySlip(year, month)
            if (response.isSuccessful) {
                response.body()?.let { salarySlip ->
                    Result.Success(salarySlip)
                } ?: Result.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.message())
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }
    
    private fun parseErrorMessage(errorBody: String?, defaultMessage: String?): String {
        return try {
            if (!errorBody.isNullOrEmpty()) {
                // Try to parse as a simple error object first (most common in this API)
                val errorMap = gson.fromJson(errorBody, Map::class.java)
                
                // Check for "error" field (common format)
                val errorField = errorMap?.get("error")
                if (errorField != null && errorField.toString().isNotEmpty()) {
                    return errorField.toString()
                }
                
                // Check for "message" field
                val messageField = errorMap?.get("message")
                if (messageField != null && messageField.toString().isNotEmpty()) {
                    return messageField.toString()
                }
                
                // Try parsing as ErrorResponse object
                val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                if (errorResponse?.message != null && errorResponse.message.isNotEmpty()) {
                    return errorResponse.message
                }
                if (errorResponse?.error != null && errorResponse.error.isNotEmpty()) {
                    return errorResponse.error
                }
            }
            
            // Fallback to HTTP response message
            defaultMessage ?: "An error occurred"
        } catch (e: Exception) {
            defaultMessage ?: "An error occurred"
        }
    }
}