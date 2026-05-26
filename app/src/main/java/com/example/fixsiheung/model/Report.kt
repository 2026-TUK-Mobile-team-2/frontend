package com.example.fixsiheung.model

import com.google.gson.annotations.SerializedName

// 민웍 목록 가져오기에 사용
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
    val address: String? = "",

    // 공감 수
    @SerializedName("empathy_count")
    val empathyCount: Int = 0
)


// 새 민원 등록용 요청 모델 (서버에 전송용)
data class ComplaintRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("category_id") val categoryId: Int,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

// 새 민원 등록 성공 후 서버 응답 모델 (서버에서 수신용)
data class ComplaintResponse(
    val message: String,
    @SerializedName("complaint_id") val complaintId: Int
)