package com.app.str.data.repository

import com.app.str.data.api.ProfileApiService
import com.app.str.data.model.ProfileResponse
import com.app.str.data.model.ProfileUpdateRequest
import com.app.str.data.model.ProfileUpdateResponse
import com.app.str.data.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileApiService: ProfileApiService
) {
    
    fun getProfile(): Flow<Result<ProfileResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = profileApiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!))
            } else {
                emit(Result.Error("Failed to fetch profile: ${response.message()}"))
            }
        } catch (e: HttpException) {
            emit(Result.Error("Network error: ${e.message()}", e))
        } catch (e: Exception) {
            emit(Result.Error("Unexpected error: ${e.message}", e))
        }
    }
    
    fun updateProfile(request: ProfileUpdateRequest): Flow<Result<ProfileUpdateResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = profileApiService.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!))
            } else {
                emit(Result.Error("Failed to update profile: ${response.message()}"))
            }
        } catch (e: HttpException) {
            emit(Result.Error("Network error: ${e.message()}", e))
        } catch (e: Exception) {
            emit(Result.Error("Unexpected error: ${e.message}", e))
        }
    }
    
    /**
     * Check if user profile is complete by fetching profile and checking if all fields are filled
     * Returns true if complete, false if incomplete or error
     */
    suspend fun isProfileComplete(): Boolean {
        return try {
            val response = profileApiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                // Check if all required fields are filled using extension function
                profile.firstName.isNotBlank() &&
                        profile.lastName.isNotBlank() &&
                        profile.designation.isNotBlank() &&
                        profile.department.isNotBlank() &&
                        profile.mobileNumber.isNotBlank() &&
                        profile.dateOfBirth.isNotBlank() &&
                        profile.gender.isNotBlank() &&
                        profile.maritalStatus.isNotBlank() &&
                        profile.aadhaarNumber.isNotBlank() &&
                        profile.panNumber.isNotBlank() &&
                        profile.locality.isNotBlank() &&
                        profile.city.isNotBlank() &&
                        profile.state.isNotBlank() &&
                        profile.pincode.isNotBlank()
            } else {
                // If profile doesn't exist or API fails, consider incomplete
                false
            }
        } catch (e: Exception) {
            // On error, assume profile is incomplete
            println("ProfileRepository: Error checking profile completion: ${e.message}")
            false
        }
    }
}