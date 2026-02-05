package com.app.str.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for forgot password - Step 1: Request OTP
 */
data class ForgotPasswordRequestOtpRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Response model for forgot password OTP request
 */
data class ForgotPasswordRequestOtpResponse(
    @SerializedName("message")
    val message: String
)

/**
 * Request model for forgot password - Step 2: Verify OTP
 */
data class ForgotPasswordVerifyOtpRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("otp")
    val otp: String
)

/**
 * Response model for forgot password OTP verification
 */
data class ForgotPasswordVerifyOtpResponse(
    @SerializedName("message")
    val message: String
)

/**
 * Request model for forgot password - Step 3: Resend OTP
 */
data class ForgotPasswordResendOtpRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Response model for forgot password resend OTP
 */
data class ForgotPasswordResendOtpResponse(
    @SerializedName("message")
    val message: String
)

/**
 * Request model for forgot password - Step 4: Reset Password
 */
data class ForgotPasswordResetRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("otp")
    val otp: String,
    @SerializedName("new_password")
    val newPassword: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

/**
 * Response model for password reset
 */
data class ForgotPasswordResetResponse(
    @SerializedName("message")
    val message: String
)

/**
 * Enum representing the current step in the forgot password flow
 */
enum class ForgotPasswordStep {
    REQUEST_OTP,
    VERIFY_OTP,
    RESET_PASSWORD
}
