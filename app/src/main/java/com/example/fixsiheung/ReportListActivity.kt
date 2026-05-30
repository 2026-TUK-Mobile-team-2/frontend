package com.example.fixsiheung

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.databinding.ActivityReportListBinding
import com.example.fixsiheung.model.Report

class ReportListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportListBinding
    private lateinit var reportAdapter: ReportAdapter

    // 💡 [원본 데이터] 모든 데이터는 여기에만 담깁니다.
    private val reportDataList = arrayListOf<Report>()
    
    // 💡 [필터 상태] 현재 어떤 필터가 켜져 있는지 기억합니다.
    private var currentFilterId: Int? = null

    // 💡 [런처 등록] 새 제보 화면에서 들고 온 데이터를 처리하는 배달원 (클래스 레벨 선언)
    private val addReportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

                // 마스터 바구니 맨 앞에 추가
                reportDataList.add(0, newReport)

                // 화면 갱신 및 스크롤 이동
                updateDisplay()
                binding.nestedScroll.smoothScrollTo(0, 0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. [초기 더미 데이터 수혈]
        initDummyData()

        // 2. [어댑터 연결] 
        // 처음에 보여줄 리스트를 생성하여 넘깁니다.
        reportAdapter = ReportAdapter(ArrayList(reportDataList))
        binding.rvReportList.adapter = reportAdapter
        binding.rvReportList.layoutManager = LinearLayoutManager(this)

        // 3. [버튼 이벤트 리스너]
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

        // 필터 버튼들
        binding.filterAllbtn.setOnClickListener {
            currentFilterId = null
            updateDisplay()
        }

        binding.filterTrashbtn.setOnClickListener {
            currentFilterId = 1
            updateDisplay()
        }

        binding.filterDamagedbtn.setOnClickListener {
            currentFilterId = 2
            updateDisplay()
        }

        binding.filterDangerbtn.setOnClickListener {
            currentFilterId = 3
            updateDisplay()
        }

        binding.filterRoadDangerbtn.setOnClickListener {
            currentFilterId = 4
            updateDisplay()
        }
    }

    //화면 정렬

    private fun updateDisplay() {
        val filteredList = if (currentFilterId == null) {
            reportDataList // 전체
        } else {
            reportDataList.filter { it.categoryId == currentFilterId }
        }
        
        // 어댑터에 새로운 리스트 전달
        reportAdapter.updateData(filteredList)
    }

    private fun initDummyData() {
        reportDataList.addAll(
            listOf(
                Report(
                    complaintId = 1,
                    userId = "user01",
                    categoryId = 1,
                    title = "정왕역 앞 쓰레기 무단 투기",
                    description = "정왕역 1번 출구 앞에 쓰레기가 너무 많이 쌓여있어서 악취가 심합니다.",
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
        // 기본 최신순 정렬
        reportDataList.sortByDescending { it.complaintId }
    }

    private fun sortByfastCount() {
        reportDataList.sortByDescending { it.complaintId }
        updateDisplay()
    }

    private fun sortByLikeCount() {
        reportDataList.sortByDescending { it.empathyCount }
        updateDisplay()
    }
}
