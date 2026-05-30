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
    private val reportDataList = arrayListOf<Report>()
    private var currentFilterId: Int? = null

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

                reportDataList.add(0, newReport)

                updateDisplay()
                binding.rvReportList.scrollToPosition(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDummyData()
        initRecyclerView()

        // ───────────── 정렬 버튼들 ─────────────────────
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

        binding.filterRoadbtn.setOnClickListener {
            currentFilterId = 4
            updateDisplay()
        }

        binding.filterNoisebtn.setOnClickListener {
            currentFilterId = 5
            updateDisplay()
        }

        binding.filterOtherbtn.setOnClickListener {
            currentFilterId = 99
            updateDisplay()
        }

        //──────────────────────────────────

        // 메인화면가는 버튼 (메뉴바에 통합해야함)
        binding.Mainbtn.setOnClickListener {
            finish()
        }
    }

    private fun initRecyclerView() {
        reportAdapter = ReportAdapter(reportDataList)
        binding.rvReportList.layoutManager = LinearLayoutManager(this)
        binding.rvReportList.adapter = reportAdapter
        updateDisplay() // 초기 정렬 상태에 맞게 화면 갱신
    }

    private fun updateDisplay() {
        val filteredList = if (currentFilterId == null) {
            reportDataList
        } else {
            reportDataList.filter { it.categoryId == currentFilterId }
        }
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
        reportDataList.sortByDescending { it.complaintId }
    }

    // ─── 정렬 로직 구역 ───────────────────────────────────────

    private fun sortByfastCount() {
        reportDataList.sortByDescending { it.complaintId }
        updateDisplay()
    }

    private fun sortByLikeCount() {
        reportDataList.sortByDescending { it.empathyCount }
        updateDisplay()
    }
}