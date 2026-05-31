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
    val empathyCount: Int = 0,

     // 추가 : 서버에서 보내주는 진행 상태(접수, 처리중, 완료)
    val status: String? = "",

    // 추가 : 작성시간, 수정시간 
    @SerializedName("created_at")
    val createdAt: String? = "",
    @SerializedName("updated_at")
    val updatedAt: String? = "",

    // 추가 : 닉네임
    val nickname: String? = "",
    // 추가 : 이미지
    @SerializedName("image_url")
    val imageUrl: String? = null,
    // 추가 : 공감여부
    val isEmpathized: Boolean = false
)



// 새 민원 등록용 요청 모델 (서버에 전송용)
data class ComplaintRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("category_id") val categoryId: Int,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
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

// 관리자가 민원 상태를 변경할 때 서버로 보낼 데이터
data class StatusUpdateRequest(
    @SerializedName("user_id") val userId: String, // 변경을 요청하는 관리자의 ID
    val status: String                             // "처리중", "완료" 등
)

// 로그인/앱 실행 시 내 폰의 푸시 알림 주소(FCM 토큰)를 서버로 보낼 데이터
data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)


// 알림 상태
data class Notification(
    val id: Int,
    val type: String,        // "status" / "empathy" / "new_complaint"
    val title: String,
    val message: String,
    @SerializedName("complaint_id") val complaintId: Int? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = ""
)
