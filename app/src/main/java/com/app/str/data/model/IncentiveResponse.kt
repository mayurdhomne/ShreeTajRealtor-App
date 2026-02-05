package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class IncentiveResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("plot_number")
    val plotNumber: String,
    
    @SerializedName("mouza")
    val mouza: String,
    
    @SerializedName("total_price")
    val totalPrice: String,
    
    @SerializedName("commission_price")
    val commissionPrice: String,
    
    @SerializedName("advance_commission")
    val advanceCommission: String,
    
    @SerializedName("total_paid_commission")
    val totalPaidCommission: String,
    
    @SerializedName("balance_commission")
    val balanceCommission: String,
    
    @SerializedName("deal_date")
    val dealDate: String,
    
    @SerializedName("customer_name")
    val customerName: String,
    
    @SerializedName("customer_mobile")
    val customerMobile: String,
    
    @SerializedName("remarks")
    val remarks: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String,
    
    @SerializedName("user")
    val userId: Int,
    
    @SerializedName("project")
    val projectId: Int
)
