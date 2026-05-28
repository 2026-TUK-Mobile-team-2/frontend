package com.example.fixsiheung

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.databinding.ActivityReportListBinding
import com.example.fixsiheung.model.Report

class ReportListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportListBinding
    private lateinit var reportAdapter: ReportAdapter

    // 💡 [단일화] 바구니는 딱 이거 하나만 씁니다! 모든 데이터는 여기에만 담깁니다.
    private val reportDataList = arrayListOf<Report>()

    private lateinit var addReportLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. [런처 등록] 새 제보 화면에서 들고 온 데이터를 처리하는 배달원
        addReportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val title = data.getStringExtra("intent_title") ?: ""
                    val description = data.getStringExtra("intent_content") ?: ""
                    val categoryId = data.getIntExtra("intent_category", 1)
                    val userId = data.getStringExtra("intent_writer") ?: "익명"

                    val newReport = Report(
                        complaintId = (reportDataList.size + 1),
                        userId = userId,
                        categoryId = categoryId,
                        title = title,
                        description = description,
                        latitude = 37.3,
                        longitude = 126.7
                    )

                    // 통합된 진짜 바구니(reportDataList) 맨 앞에 추가합니다.
                    reportDataList.add(0, newReport)

                    // 화면 새로고침 및 스크롤 맨 위로 이동
                    reportAdapter.notifyDataSetChanged()
                    binding.rvReportList.scrollToPosition(0)
                }
            }
        }

        // 2. [초기 더미 데이터 수혈] 중복 선언을 없애고 진짜 바구니에 곧바로 데이터를 채웁니다.
        reportDataList.addAll(
            listOf(
                Report(
                    complaintId = 1,
                    userId = "user01",
                    categoryId = 1,
                    title = "정왕역 앞 쓰레기 무단 투기",
                    description = "정왕역 1번 출구 앞에 쓰레기가 너무 많이 쌓여있어서 악취가 심합니다. 빨리 치워주세요.",
                    latitude = 37.3514,
                    longitude = 126.7424,
                    address = "경기도 시흥시 정왕동 정왕역",
                    empathyCount = 12,
                    createdAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
                ),
                Report(
                    complaintId = 2,
                    userId = "user02",
                    categoryId = 2,
                    title = "중앙공원 벤치 파손",
                    description = "공원 중앙 분수대 옆에 있는 나무 벤치 다리가 부러져 있어서 위험합니다.",
                    latitude = 37.3550,
                    longitude = 126.7380,
                    address = "경기도 시흥시 정왕동 중앙공원",
                    empathyCount = 5,
                    createdAt = System.currentTimeMillis() - (30 * 60 * 60 * 1000)
                )
            )
        )

        // 최신 아이디 순으로 초기 정렬
        reportDataList.sortByDescending { it.complaintId }

        // 3. [어댑터 딱 한 번만 연결] 진짜 바구니를 쥐어주고 리사이클러뷰에 세팅합니다.
        reportAdapter = ReportAdapter(reportDataList)
        binding.rvReportList.adapter = reportAdapter
        binding.rvReportList.layoutManager = LinearLayoutManager(this)

        // 4. [버튼 이벤트 리스너 세팅]
        binding.Mainbtn.setOnClickListener {
            finish()
        }

        binding.itemaddbtn.setOnClickListener {
            val intent = Intent(this, ReportAddActivity::class.java)
            addReportLauncher.launch(intent)
        }

        binding.sortfastbtn.setOnClickListener {
            sortByfastCount()
        }

        binding.sortlikebtn.setOnClickListener {
            sortByLikeCount()
        }
    }

    // 5. [정렬 함수 기능 복구] 진짜 바구니인 reportDataList를 정렬하도록 수정했습니다.
    private fun sortByfastCount() {
        val sortedList = reportDataList.sortedByDescending { it.complaintId }
        reportDataList.clear()
        reportDataList.addAll(sortedList)
        reportAdapter.notifyDataSetChanged()
    }

    private fun sortByLikeCount() {
        val sortedList = reportDataList.sortedByDescending { it.empathyCount }
        reportDataList.clear()
        reportDataList.addAll(sortedList)
        reportAdapter.notifyDataSetChanged()
    }
}