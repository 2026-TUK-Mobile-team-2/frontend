package com.example.fixsiheung.model

// 제보 데이터를 정의하는 데이터 클래스
data class Report(
    val id: Int,               // 제보 ID
    val title: String,         // 제목
    val content: String,       // 상세 내용
    val category: String,      // 카테고리 (쓰레기, 파손 등)
    val latitude: Double,      // 위치 위도
    val longitude: Double,     // 위치 경도
    val author: String,        // 작성자 (우수 민원인 체크용)
    val likeCount: Int = 0,    // 공감 수 (기본값 0)
    val status: String = "접수" // 처리 상태 (접수, 처리중, 해결)
)