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

    // 💡 [런처 등록] 새 제보 화면에서 들고 온 데이터를 처리하는 배달원
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

                // 필터 상태 유지하면서 화면 새로고침
                updateDisplay()
                binding.rvReportList.scrollToPosition(0)
            }
        }
    }

    // 🛠️ [긴급 수리] 사라졌던 onCreate 선언부를 다시 만들고 중괄호 짝을 완벽히 맞췄습니다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 데이터 세팅 및 리사이클러뷰 준비
        initDummyData()
        initRecyclerView()

        // 버튼 클릭 이벤트 리스너들 등록
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

        // 필터 버튼 클릭 리스너들
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

        // 메인 지도 화면으로 돌아가는 버튼 리스너
        binding.Mainbtn.setOnClickListener {
            finish()
        }
    }

    /**
     * 리사이클러뷰 초기 연결 함수
     */
    private fun initRecyclerView() {
        reportAdapter = ReportAdapter(reportDataList)
        binding.rvReportList.layoutManager = LinearLayoutManager(this)
        binding.rvReportList.adapter = reportAdapter
        updateDisplay() // 초기 정렬 상태에 맞게 화면 갱신
    }

    /**
     * 필터 상태를 유지한 채 어댑터에 데이터를 갱신하는 핵심 함수
     */
    private fun updateDisplay() {
        val filteredList = if (currentFilterId == null) {
            reportDataList // 전체 보기
        } else {
            reportDataList.filter { it.categoryId == currentFilterId } // 카테고리 필터링
        }

        // 어댑터에 정렬/필터링된 새로운 리스트만 쏙 전달
        reportAdapter.updateData(filteredList)
    }

    /**
     * 초기 더미 데이터 생성
     */
    private fun initDummyData() {
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
                    empathyCount = 12
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
                    empathyCount = 5
                )
            )
        )
        // 기본 정렬: 최신순 (complaintId가 높은 순서가 맨 위로)
        reportDataList.sortByDescending { it.complaintId }
    }

    // ─── 정렬 로직 구역 (중복 코드 완벽 컷!) ───────────────────────────────────────

    private fun sortByfastCount() {
        // 원본 데이터를 최신 id순으로 정렬한 뒤 필터 유지한 채 화면 갱신
        reportDataList.sortByDescending { it.complaintId }
        updateDisplay()
    }

    private fun sortByLikeCount() {
        // 원본 데이터를 공감 많은 순으로 정렬한 뒤 필터 유지한 채 화면 갱신
        reportDataList.sortByDescending { it.empathyCount }
        updateDisplay()
    }
}