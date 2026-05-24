package com.example.fixsiheung.model

// 제보 데이터를 정의하는 데이터 클래스
data class Report(
    val complaintId: Int,
    val userId: String,
    val categoryId: Int,
    val title: String,
    val description: String,
    val status: String = "접수",
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val empathyCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)