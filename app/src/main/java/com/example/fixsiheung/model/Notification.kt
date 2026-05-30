package com.example.fixsiheung.model

import com.google.gson.annotations.SerializedName

data class Notification(
    val id: Int,                         // 알림 고유 ID (notif_id)
    val type: String,                    // 알림 종류 ("status" / "empathy" / "new_complaint")
    val title: String,                   // 알림 제목
    val message: String,                 // 알림 내용
    @SerializedName("complaint_id") val complaintId: Int? = null, // 연관된 민원 ID (필요시 이동용)
    @SerializedName("is_read") val isRead: Boolean = false,       // 읽음 여부
    @SerializedName("created_at") val createdAt: String? = ""     // 생성 시간 문자열
)