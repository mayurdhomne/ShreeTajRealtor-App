package com.app.str.data.model

import com.google.gson.annotations.SerializedName

data class WorkType(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?
)

data class WorkTypeOption(
    @SerializedName("id")
    val id: Int,
    @SerializedName("work_type")
    val workType: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?
)
