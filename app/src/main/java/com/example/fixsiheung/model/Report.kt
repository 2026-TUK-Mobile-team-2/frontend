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

// 마이페이지 프로필 정보 모델
data class UserProfile(
    val user_id: String,
    val nickname: String,
    val email: String
)

// 마이페이지 통합 응답 모델 (공감한 글 목록 추가)
data class MyPageResponse(
    val user: UserProfile,
    val my_reports: List<Report>,
    val empathized_reports: List<Report>
)

// 회원정보 수정 요청 모델
data class UserUpdateRequest(
    val password: String,
    val nickname: String
)

// =========================================================
// 알림 및 관리자 전용 API 추가
// =========================================================

// [유저] 내 기기의 푸시 알림 토큰(FCM)을 서버에 저장
@PUT("api/users/{user_id}/fcm-token")
fun updateFcmToken(
    @Path("user_id") userId: String,
    @Body request: FcmTokenRequest
): Call<Map<String, String>>

// 특정 민원의 상태(접수/처리중/완료) 변경 및 알림 발송
@PUT("api/complaints/{complaint_id}/status")
fun updateComplaintStatus(
    @Path("complaint_id") complaintId: Int,
    @Body request: StatusUpdateRequest
): Call<Map<String, String>>
