package com.example.fixsiheung.network

import com.example.fixsiheung.model.ComplaintRequest
import com.example.fixsiheung.model.ComplaintResponse
import com.example.fixsiheung.model.LoginRequest
import com.example.fixsiheung.model.LoginResponse
import com.example.fixsiheung.model.MyPageResponse
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.model.SignupRequest
import com.example.fixsiheung.model.UserUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    // 민원 목록 가져오기
    @GET("api/complaints/")
    fun getAllReports(): Call<List<Report>>

    // 새 민원 등록
    @POST("api/complaints")
    fun postReport(@Body request: ComplaintRequest): Call<ComplaintResponse>


    // 회원가입 (POST /api/users/)
    @POST("api/users/")
    fun signup(@Body request: SignupRequest): Call<Any>

    // 로그인 (POST /api/users/login)
    @POST("api/users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // 닉네임 중복 체크 (성공 여부를 판단하기 위해 응답을 Map이나 별도 데이터 클래스로 받음)
    @POST("api/users/check-nickname")
    fun checkNickname(@Body request: Map<String, String>): Call<Map<String, Any>>

    // 아이디 중복 체크
    @POST("api/users/check-id")
    fun checkId(@Body request: Map<String, String>): Call<Map<String, Any>>

    // 이메일 중복 체크
    @POST("api/users/check-email")
    fun checkEmail(@Body request: Map<String, String>): Call<Map<String, Any>>

    // 이미지 파일 전송을 위한 Multipart 설정
    @Multipart
    @POST("api/complaints/{complaint_id}/image")
    fun uploadComplaintImage(
        @Path("complaint_id") complaintId: Int,
        @Part image: MultipartBody.Part): Call<Map<String, Any>>

    // 지도 마커용 민원조회
    @GET("api/complaints/markers")
    fun getComplaintMarkers(): Call<List<Report>>

    // 민원 상세 조회 (특정 민원 1개 가져오기)
    @GET("api/complaints/{complaint_id}")
    fun getComplaintDetail(@Path("complaint_id") complaintId: Int): Call<Report>

    // 민원 수정 (PUT)
    @PUT("api/complaints/{complaint_id}")
    fun updateComplaint(
        @Path("complaint_id") complaintId: Int,
        @Body request: ComplaintRequest
    ): Call<Map<String, String>>

    // 민원 삭제 (DELETE)
    @HTTP(method = "DELETE", path = "api/complaints/{complaint_id}", hasBody = true)
    fun deleteComplaint(
        @Path("complaint_id") complaintId: Int,
        @Body request: Map<String, String> // user_id만 담아서 보냄
    ): Call<Map<String, String>>

    // 공감 추가 (POST)
    @POST("api/complaints/{complaint_id}/empathy")
    fun addEmpathy(
        @Path("complaint_id") complaintId: Int,
        @Body request: Map<String, String> // user_id만 담아서 보냄
    ): Call<Map<String, String>>

    // 공감 취소 (DELETE)
    @HTTP(method = "DELETE", path = "api/complaints/{complaint_id}/empathy", hasBody = true)
    fun removeEmpathy(
        @Path("complaint_id") complaintId: Int,
        @Body request: Map<String, String> // user_id만 담아서 보냄
    ): Call<Map<String, String>>


    // 마이페이지 통합 조회 (내 정보 + 내 글 + 공감한 글)
    @GET("api/users/{user_id}/mypage")
    fun getMyPage(@Path("user_id") userId: String): Call<MyPageResponse>

    // 회원정보 수정 (비밀번호, 닉네임 등)
    @PUT("api/users/{user_id}")
    fun updateUserInfo(
        @Path("user_id") userId: String,
        @Body request: UserUpdateRequest
    ): Call<Map<String, String>>

    // 회원 탈퇴
    @DELETE("api/users/{user_id}")
    fun deleteUser(@Path("user_id") userId: String): Call<Map<String, String>>

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


}
