package com.example.fixsiheung.network

import com.example.fixsiheung.model.Report
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // 1. 민원 목록 가져오기
    @GET("api/complaints/")
    fun getAllReports(): Call<List<Report>>

    // 2. 새 민원 등록하기 (서버에서 성공 메시지를 반환하므로 우선 Any 처리)
    @POST("api/complaints/")
    fun postReport(@Body report: Report): Call<Any>
}