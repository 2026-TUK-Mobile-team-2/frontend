package com.example.fixsiheung

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.databinding.ActivityListBinding
import com.example.fixsiheung.model.Report
import java.text.SimpleDateFormat
import java.util.Locale

class ReportListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private lateinit var reportAdapter: ReportAdapter

    private val masterReportList = arrayListOf<Report>()

    private var currentFilterId: Int? = null
    private var currentSortType: String = "FAST"

    private val categoryButtons by lazy {
        listOf(
            binding.filterAllbtn,
            binding.filterTrashbtn,
            binding.filterDamagedbtn,
            binding.filterDangerbtn,
            binding.filterRoadbtn,
            binding.filterNoisebtn,
            binding.filterOtherbtn
        )
    }

    private val addReportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDateTimeString = sdf.format(java.util.Date())

            val newReport = Report(
                complaintId = (masterReportList.size + 1),
                userId = data.getStringExtra("intent_writer") ?: "익명",
                categoryId = data.getIntExtra("intent_category", 1),
                title = data.getStringExtra("intent_title") ?: "",
                description = data.getStringExtra("intent_content") ?: "",
                latitude = 37.3,
                longitude = 126.7,
                address = "시흥시 정왕동",
                empathyCount = 0,
                status = "접수",

                createdAt = currentDateTimeString,
                updatedAt = currentDateTimeString,
                nickname = data.getStringExtra("intent_writer") ?: "익명",
                imageUrl = null
            )

            masterReportList.add(0, newReport)
            applyFilterAndSort()
            binding.rvReportList.scrollToPosition(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDummyData()
        initRecyclerView()
        setupListeners()
    }

    private fun initRecyclerView() {
        reportAdapter = ReportAdapter(emptyList())
        binding.rvReportList.layoutManager = LinearLayoutManager(this)
        binding.rvReportList.adapter = reportAdapter
        applyFilterAndSort()
    }

    private fun setupListeners() {
        binding.itemaddbtn.setOnClickListener {
            addReportLauncher.launch(Intent(this, ReportAddActivity::class.java))
        }

        binding.sortfastbtn.setOnClickListener {
            currentSortType = "FAST"
            applyFilterAndSort()
        }
        binding.sortlikebtn.setOnClickListener {
            currentSortType = "LIKE"
            applyFilterAndSort()
        }

        binding.filterAllbtn.setOnClickListener { updateFilter(null) }
        binding.filterTrashbtn.setOnClickListener { updateFilter(1) }
        binding.filterDamagedbtn.setOnClickListener { updateFilter(2) }
        binding.filterDangerbtn.setOnClickListener { updateFilter(3) }
        binding.filterRoadbtn.setOnClickListener { updateFilter(4) }
        binding.filterNoisebtn.setOnClickListener { updateFilter(5) }
        binding.filterOtherbtn.setOnClickListener { updateFilter(6) }
    }

    private fun updateFilter(categoryId: Int?) {
        currentFilterId = categoryId
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var filteredList = if (currentFilterId == null) {
            masterReportList.toList()
        } else {
            masterReportList.filter { it.categoryId == currentFilterId }
        }

        filteredList = when (currentSortType) {
            "FAST" -> filteredList.sortedByDescending { it.complaintId }
            "LIKE" -> filteredList.sortedByDescending { it.empathyCount }
            else -> filteredList
        }

        reportAdapter.updateData(filteredList)
        updateUIStates()
    }

    private fun updateUIStates() {
        val targetIndex = currentFilterId ?: 0
        categoryButtons.forEachIndexed { index, button ->
            if (index == targetIndex) {
                button.setBackgroundResource(R.drawable.re_category_selected_bg)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundResource(R.drawable.re_category_unselected_bg)
                button.setTextColor(Color.parseColor("#4B5563"))
            }
        }

        if (currentSortType == "FAST") {
            binding.sortfastbtn.setBackgroundResource(R.drawable.re_category_selected_bg)
            binding.sortfastbtn.setTextColor(Color.WHITE)
            binding.sortlikebtn.setBackgroundResource(R.drawable.re_category_unselected_bg)
            binding.sortlikebtn.setTextColor(Color.parseColor("#4B5563"))
        } else {
            binding.sortfastbtn.setBackgroundResource(R.drawable.re_category_unselected_bg)
            binding.sortfastbtn.setTextColor(Color.parseColor("#4B5563"))
            binding.sortlikebtn.setBackgroundResource(R.drawable.re_category_selected_bg)
            binding.sortlikebtn.setTextColor(Color.WHITE)
        }
    }

    private fun initDummyData() {
        masterReportList.addAll(listOf(
            Report(complaintId = 1, userId = "user01", categoryId = 1, title = "정왕역 앞 쓰레기 무단 투기", description = "악취가 심합니다.", latitude = 37.3514, longitude = 126.7424, address = "경기도 시흥시 정왕동 정왕역", empathyCount = 12, status = "접수", createdAt = "2026-05-31 01:00:00", updatedAt = "2026-05-31 01:00:00", nickname = "익명1", imageUrl = null),
            Report(complaintId = 2, userId = "user02", categoryId = 2, title = "중앙공원 벤치 파손", description = "나무 벤치 다리가 부러져 있습니다.", latitude = 37.3550, longitude = 126.7380, address = "경기도 시흥시 정왕동 중앙공원", empathyCount = 5, status = "접수", createdAt = "2026-05-31 03:00:00", updatedAt = "2026-05-31 03:00:00", nickname = "익명2", imageUrl = null)
        ))
    }
}