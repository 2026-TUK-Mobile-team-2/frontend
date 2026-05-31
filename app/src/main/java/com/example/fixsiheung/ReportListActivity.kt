package com.example.fixsiheung

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.databinding.ActivityListBinding
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.fixsiheung.mypage.MyPageActivity
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
            // 등록 완료 후 서버에서 목록을 다시 불러와서 최신화합니다.
            loadReportListFromServer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비 기본 선택
        binding.bottomNavigation.selectedItemId = R.id.nav_list

        // 하단 네비 클릭 이벤트
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainMapActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                R.id.nav_list -> {
                    true
                }

                R.id.nav_report -> {
                    startActivity(Intent(this, ReportAddActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                R.id.nav_mypage -> {
                    startActivity(Intent(this, MyPageActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                else -> false
            }
        }


        initRecyclerView()
        setupListeners()

        // 💡 더미 데이터 대신 서버에서 진짜 데이터를 불러옵니다.
        //loadReportListFromServer()
    }

    // 💡 화면이 다시 보일 때마다 무조건 실행되는 생명주기 함수입니다.
    override fun onResume() {
        super.onResume()

        // 상세 페이지에서 뒤로가기를 눌러서 돌아왔을 때, 서버에서 최신 목록을 다시 싹 불러옵니다!
        loadReportListFromServer()
    }

    private fun initRecyclerView() {
        reportAdapter = ReportAdapter(emptyList())
        binding.rvReportList.layoutManager = LinearLayoutManager(this)
        binding.rvReportList.adapter = reportAdapter
        applyFilterAndSort()
    }

    private fun setupListeners() {
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

    // 💡 서버에서 데이터를 가져오는 통신 함수를 추가했습니다.
    private fun loadReportListFromServer() {
        RetrofitClient.apiService.getAllReports().enqueue(object : Callback<List<Report>> {
            override fun onResponse(call: Call<List<Report>>, response: Response<List<Report>>) {
                if (response.isSuccessful) {
                    val reportList = response.body()
                    if (reportList != null) {
                        // 기존 리스트를 비우고 서버 데이터로 채움
                        masterReportList.clear()
                        masterReportList.addAll(reportList)

                        // 필터 및 정렬 적용하여 화면 업데이트
                        applyFilterAndSort()

                        updateHotIssues(reportList)
                        Log.d("API_SUCCESS", "목록 불러오기 성공: ${reportList.size}개")
                    }
                } else {
                    Toast.makeText(this@ReportListActivity, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "에러 코드: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Report>>, t: Throwable) {
                Toast.makeText(this@ReportListActivity, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                Log.e("API_FAIL", "통신 실패: ${t.message}")
            }
        })
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

    private fun updateHotIssues(reports: List<Report>) {
        // 공감순(empathyCount)으로 내림차순 정렬 후 상위 3개만 자르기
        val hotReports = reports.sortedByDescending { it.empathyCount }.take(3)

        // HOT 카드 1 업데이트
        if (hotReports.isNotEmpty()) {
            binding.hotCard1.visibility = android.view.View.VISIBLE
            binding.tvHotLike1.text = "💛 ${hotReports[0].empathyCount}"
            binding.tvHotTitle1.text = hotReports[0].title
            binding.tvHotAddress1.text = "📍 ${hotReports[0].address}"
        } else {
            binding.hotCard1.visibility = android.view.View.GONE
        }

        // HOT 카드 2 업데이트
        if (hotReports.size > 1) {
            binding.hotCard2.visibility = android.view.View.VISIBLE
            binding.tvHotLike2.text = "💛 ${hotReports[1].empathyCount}"
            binding.tvHotTitle2.text = hotReports[1].title
            binding.tvHotAddress2.text = "📍 ${hotReports[1].address}"
        } else {
            binding.hotCard2.visibility = android.view.View.GONE
        }

        // HOT 카드 3 업데이트
        if (hotReports.size > 2) {
            binding.hotCard3.visibility = android.view.View.VISIBLE
            binding.tvHotLike3.text = "💛 ${hotReports[2].empathyCount}"
            binding.tvHotTitle3.text = hotReports[2].title
            binding.tvHotAddress3.text = "📍 ${hotReports[2].address}"
        } else {
            binding.hotCard3.visibility = android.view.View.GONE
        }
    }


}