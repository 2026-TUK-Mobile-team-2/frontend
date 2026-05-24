package com.example.fixsiheung.network

import com.example.fixsiheung.model.LoginRequest
import com.example.fixsiheung.model.LoginResponse
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.model.SignupRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // 민원 목록 가져오기
    @GET("api/complaints/")
    fun getAllReports(): Call<List<Report>>

    // 새 민원 등록하기 (서버에서 성공 메시지를 반환하므로 우선 Any 처리)
    @POST("api/complaints/")
    fun postReport(@Body report: Report): Call<Any>

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

}