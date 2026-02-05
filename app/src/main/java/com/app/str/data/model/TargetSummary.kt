package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class TargetSummary(
    @SerializedName("total_target")
    val totalTarget: Double,
    
    @SerializedName("total_sale")
    val totalSale: Double,
    
    @SerializedName("remaining_target")
    val remainingTarget: Double
) {
    val achievementPercentage: Double
        get() = if (totalTarget > 0) (totalSale / totalTarget) * 100 else 0.0
    
    val progressPercentage: Double
        get() = if (totalTarget > 0) kotlin.math.min(100.0, (totalSale / totalTarget) * 100) else 0.0
}