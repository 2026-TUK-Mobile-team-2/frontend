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

    // 💡 [마스터 리스트] 모든 원본 데이터는 여기에 보관됩니다.
    private val reportDataList = arrayListOf<Report>()
    
    // 💡 [상태 변수] 현재 선택된 필터 카테고리를 저장합니다 (null이면 전체)
    private var currentFilterId: Int? = null

    // 💡 [런처] 새 제보 화면에서 데이터를 받아오는 배달원 (클래스 레벨 선언)
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

                // 마스터 리스트 맨 앞에 추가
                reportDataList.add(0, newReport)
                
                // 화면 상태에 맞춰 갱신
                updateDisplay()
                binding.nestedScroll.smoothScrollTo(0, 0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 초기 더미 데이터 채우기
        initDummyData()

        // 2. 어댑터 초기화 (초기 리스트는 전체 데이터를 복사해서 사용)
        reportAdapter = ReportAdapter(ArrayList(reportDataList))
        binding.rvReportList.adapter = reportAdapter
        binding.rvReportList.layoutManager = LinearLayoutManager(this)

        // 3. 버튼 클릭 리스너 설정
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 메인으로 돌아가기
        binding.Mainbtn.setOnClickListener { finish() }

        // 민원 추가 버튼
        binding.itemaddbtn.setOnClickListener {
            val intent = Intent(this, ReportAddActivity::class.java)
            addReportLauncher.launch(intent)
        }

        // 최신순 정렬
        binding.sortfastbtn.setOnClickListener {
            sortByfastCount()
        }

        // 공감순 정렬
        binding.sortlikebtn.setOnClickListener {
            sortByLikeCount()
        }

        // 필터: 전체
        binding.filterAllbtn.setOnClickListener {
            currentFilterId = null
            updateDisplay()
        }

        // 필터: 쓰레기
        binding.filterTrashbtn.setOnClickListener {
            currentFilterId = 1
            updateDisplay()
        }

        // 필터: 시설파손
        binding.filterDamagedbtn.setOnClickListener {
            currentFilterId = 2
            updateDisplay()
        }

        // 필터: 안전위험
        binding.filterDangerbtn.setOnClickListener {
            currentFilterId = 3
            updateDisplay()
        }

        // 필터: 도로위험
        binding.filterRoadDangerbtn.setOnClickListener {
            currentFilterId = 4
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val filteredList = if (currentFilterId == null) {
            reportDataList // 전체보기
        } else {
            reportDataList.filter { it.categoryId == currentFilterId }
        }
        
        // 어댑터에 필터링된 새로운 리스트를 전달하여 화면을 다시 그리게 함
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
                    description = "공원 중앙 분수대 옆에 있는 나무 벤치 다리가 부러져 있습니다.",
                    latitude = 37.3550,
                    longitude = 126.7380,
                    address = "경기도 시흥시 정왕동 중앙공원",
                    empathyCount = 5,
                    createdAt = System.currentTimeMillis() - (30 * 60 * 60 * 1000)
                )
            )
        )
        // 기본 정렬: 최신순
        reportDataList.sortByDescending { it.complaintId }
    }

    private fun sortByfastCount() {
        // 원본 데이터를 정렬한 뒤
        reportDataList.sortByDescending { it.complaintId }
        // 필터를 유지한 채 화면 갱신
        updateDisplay()
    }

    private fun sortByLikeCount() {
        // 원본 데이터를 정렬한 뒤
        reportDataList.sortByDescending { it.empathyCount }
        // 필터를 유지한 채 화면 갱신
        updateDisplay()
    }
}