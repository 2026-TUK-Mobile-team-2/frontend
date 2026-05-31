package com.example.fixsiheung.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 에뮬레이터에서 컴퓨터의 Flask 서버(포트 5000)로 접속하기 위한 특수 주소
    // 실제 폰 연결 테스트 시 같은 WI-FI 사용해야함

    // 핸드폰테스트
    private const val BASE_URL = "http://192.168.0.41:5000/"

    // 에뮬레이터
    //private const val BASE_URL = "http://10.0.2.2:5000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}