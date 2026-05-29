package com.example.fixsiheung.mypage

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.NearReportAdapter
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.ActivityReportListBinding
import com.example.fixsiheung.model.Report

class ReportListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityReportListBinding.inflate(layoutInflater) }
    private var isSortedByEmpathy = false  // true: 공감순, false: 최신순
    private var currentData = listOf<Report>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val screenType = intent.getStringExtra("SCREEN_TYPE") ?: "MY_REPORTS"

        binding.tvListToolbarTitle.text = when (screenType) {
            "MY_REPORTS"         -> "제보 내역"
            "EMPATHIZED_REPORTS" -> "공감한 제보"
            else                 -> "목록"
        }

        binding.btnBack.setOnClickListener { finish() }

        // TODO: API 연동 후 실제 데이터로 교체
        val mockData = listOf<Report>()
        showData(mockData, screenType)

        // 정렬 버튼 클릭 시 토글
        binding.tvSortOrder.setOnClickListener {
            isSortedByEmpathy = !isSortedByEmpathy
            binding.tvSortOrder.text = if (isSortedByEmpathy) "공감순" else "최신순"
            showData(currentData, screenType)
        }

        // 칩 필터
        binding.chipGroupList.setOnCheckedStateChangeListener { _, checkedIds ->
            val categoryId: Int? = when (checkedIds.firstOrNull()) {
                R.id.chip_trash      -> 1
                R.id.chip_facilities -> 2
                R.id.chip_road       -> 3
                R.id.chip_any        -> 4
                else                 -> null
            }
            val filtered = if (categoryId == null) currentData
            else currentData.filter { it.categoryId == categoryId }
            showData(filtered, screenType)
        }
    }

    private fun showData(items: List<Report>, screenType: String) {
        // 정렬 적용
        val sorted = if (isSortedByEmpathy) {
            items.sortedByDescending { it.empathyCount }
        } else {
            items.sortedByDescending { it.complaintId } // 최신순은 추후 createdAt으로 교체
        }

        binding.tvTotalCount.text = "전체 ${sorted.size}"

        if (sorted.isEmpty()) {
            binding.rvReportList.visibility     = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = when (screenType) {
                "MY_REPORTS"         -> "아직 등록한 제보가 없어요."
                "EMPATHIZED_REPORTS" -> "아직 공감한 제보가 없어요."
                else -> "설명"
            }
            binding.btnEmptyAction.text = when (screenType) {
                "MY_REPORTS"         -> "제보하러 가기"
                "EMPATHIZED_REPORTS" -> "지도 보러 가기"
                else -> "이동"
            }
        } else {
            binding.rvReportList.visibility     = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE

            val adapter = NearReportAdapter(sorted) { report ->
                // TODO: 민원 상세 페이지로 이동
            }
            adapter.isVertical = true
            binding.rvReportList.layoutManager = LinearLayoutManager(this)
            binding.rvReportList.adapter = adapter
        }
    }
}