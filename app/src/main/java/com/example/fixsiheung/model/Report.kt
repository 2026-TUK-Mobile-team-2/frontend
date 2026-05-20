package com.example.fixsiheung.model

import com.google.gson.annotations.SerializedName

data class Report(
    @SerializedName("complaint_id")
    val complaintId: Int = 0, // 서버에서 자동 생성

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("category_id")
    val categoryId: Int,

    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = ""
)