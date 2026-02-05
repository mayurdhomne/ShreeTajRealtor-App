package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

data class SignUpResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("email")
    val email: String
)

data class OtpRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("otp")
    val otp: String
)

data class ResendOtpRequest(
    @SerializedName("email")
    val email: String
)

data class ResendOtpResponse(
    @SerializedName("message")
    val message: String
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("profile_completed")
    val profileCompleted: Boolean = false
)

data class RefreshTokenRequest(
    @SerializedName("refresh")
    val refresh: String
)

data class RefreshTokenResponse(
    @SerializedName("access")
    val access: String,
    @SerializedName("refresh")
    val refresh: String? = null  // Refresh token is optional in response
)

data class LogoutRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class LogoutResponse(
    @SerializedName("message")
    val message: String
)

data class ProfileCompletionRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("marital_status")
    val maritalStatus: String,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String,
    @SerializedName("pan_number")
    val panNumber: String,
    @SerializedName("locality")
    val locality: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("pincode")
    val pincode: String
)

data class ProfileCompletionResponse(
    @SerializedName("message")
    val message: String
)

data class CheckInRequest(
    @SerializedName("latitude")
    val latitude: String,
    @SerializedName("longitude")
    val longitude: String
)

data class CheckOutRequest(
    @SerializedName("latitude")
    val latitude: String,
    @SerializedName("longitude")
    val longitude: String
)

data class AttendanceResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("check_in_time")
    val checkInTime: String? = null,
    @SerializedName("check_out_time")
    val checkOutTime: String? = null
)

data class ErrorResponse(
    @SerializedName("non_field_errors")
    val nonFieldErrors: List<String>? = null,
    @SerializedName("email")
    val email: List<String>? = null,
    @SerializedName("password")
    val password: List<String>? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("detail")
    val detail: String? = null
)

// Profile Data Models
data class ProfileResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("marital_status")
    val maritalStatus: String,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String,
    @SerializedName("pan_number")
    val panNumber: String,
    @SerializedName("locality")
    val locality: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("pincode")
    val pincode: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class ProfileUpdateRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("marital_status")
    val maritalStatus: String,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String,
    @SerializedName("pan_number")
    val panNumber: String,
    @SerializedName("locality")
    val locality: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("pincode")
    val pincode: String
)

data class ProfileUpdateResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("marital_status")
    val maritalStatus: String,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String,
    @SerializedName("pan_number")
    val panNumber: String,
    @SerializedName("locality")
    val locality: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("pincode")
    val pincode: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Result for login navigation decision
 */
data class LoginNavigationResult(
    val loginResponse: LoginResponse,
    val shouldNavigateToDashboard: Boolean,
    val profileComplete: Boolean
)

/**
 * Extension function to check if profile is complete
 * Returns true if all required fields are filled, false if any field is null/empty
 */
fun ProfileResponse.isProfileComplete(): Boolean {
    return firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            designation.isNotBlank() &&
            department.isNotBlank() &&
            mobileNumber.isNotBlank() &&
            dateOfBirth.isNotBlank() &&
            gender.isNotBlank() &&
            maritalStatus.isNotBlank() &&
            aadhaarNumber.isNotBlank() &&
            panNumber.isNotBlank() &&
            locality.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank() &&
            pincode.isNotBlank()
}