package com.example.fixsiheung.model

import com.google.gson.annotations.SerializedName

// 1. 회원가입할 때 서버로 보낼 데이터
data class SignupRequest(
    @SerializedName("user_id") val userId: String,
    val password: String,
    val email: String,
    val nickname: String,
    val name: String? = null // 실명 미입력 -> null 처리
)

// 2. 로그인할 때 서버로 보낼 데이터
data class LoginRequest(
    @SerializedName("user_id") val userId: String,
    val password: String
)

// 3. 로그인 성공 시 서버에서 받아올 응답 데이터
data class LoginResponse(
    val message: String,
    @SerializedName("user_id") val userId: String,
    val name: String
)