package com.example.fixsiheung.mypage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.R
import com.example.fixsiheung.ReportActivity
import com.example.fixsiheung.databinding.ActivityMyListBinding
import com.example.fixsiheung.model.MyPageResponse
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.network.RetrofitClient

class MyListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMyListBinding.inflate(layoutInflater) }
    private var isSortedByEmpathy = false
    private var currentData = listOf<Report>()
    private lateinit var adapter: MyListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val screenType = intent.getStringExtra("SCREEN_TYPE") ?: "MY_REPORTS"

        binding.tvListToolbarTitle.text = when (screenType) {
            "MY_REPORTS"         -> "제보 내역"
            "EMPATHIZED_REPORTS" -> "공감한 제보"
            "ADMIN"              -> "전체 제보 관리"
            else                 -> "목록"
        }

        // 관리자는 항상 공감순이므로 정렬 버튼 숨기기
        binding.tvSortOrder.visibility = if (screenType == "ADMIN") View.GONE else View.VISIBLE

        binding.btnBack.setOnClickListener { finish() }
        binding.tvSortOrder.text            = "최신순"
        binding.rvReportList.visibility     = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        // 어댑터 초기화
        adapter = MyListAdapter(emptyList()) { report ->
            // TODO: 민원 상세 페이지로 이동
        }
        binding.rvReportList.layoutManager = LinearLayoutManager(this)
        binding.rvReportList.adapter = adapter

        loadReportData(screenType)

        binding.tvSortOrder.setOnClickListener {
            isSortedByEmpathy = !isSortedByEmpathy
            binding.tvSortOrder.text = if (isSortedByEmpathy) "공감순" else "최신순"
            showData(currentData, screenType)
        }
    }

    private fun loadReportData(screenType: String) {
        if (screenType == "ADMIN") {
            RetrofitClient.apiService.getAllReports()
                .enqueue(object : retrofit2.Callback<List<Report>> {
                    override fun onResponse(call: retrofit2.Call<List<Report>>, response: retrofit2.Response<List<Report>>) {
                        if (response.isSuccessful) {
                            currentData = response.body() ?: emptyList()
                            runOnUiThread {
                                showData(currentData, screenType)
                                initChipFilter(screenType)
                            }
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<List<Report>>, t: Throwable) {
                        Log.e("Admin", "API 실패: ${t.message}")
                    }
                })
            return
        }

        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("user_id", null)

        if (userId == null) {
            showData(emptyList(), screenType)
            initChipFilter(screenType)
            return
        }

        RetrofitClient.apiService.getMyPage(userId)
            .enqueue(object : retrofit2.Callback<MyPageResponse> {
                override fun onResponse(call: retrofit2.Call<MyPageResponse>, response: retrofit2.Response<MyPageResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: return
                        currentData = when (screenType) {
                            "MY_REPORTS"         -> data.my_reports
                            "EMPATHIZED_REPORTS" -> data.empathized_reports
                            else                 -> emptyList()
                        }
                        runOnUiThread {
                            showData(currentData, screenType)
                            initChipFilter(screenType)
                        }
                    }
                }
                override fun onFailure(call: retrofit2.Call<MyPageResponse>, t: Throwable) {
                    Log.e("ReportList", "API 실패: ${t.message}")
                }
            })
    }

    private fun initChipFilter(screenType: String) {
        binding.chipGroupList.setOnCheckedStateChangeListener { _, checkedIds ->
            val categoryId: Int? = when (checkedIds.firstOrNull()) {
                R.id.chip_road       -> 1
                R.id.chip_trash      -> 2
                R.id.chip_facilities -> 3
                R.id.chip_any        -> 4
                else                 -> null
            }
            val filtered = if (categoryId == null) currentData
            else currentData.filter { it.categoryId == categoryId }
            showData(filtered, screenType)
        }
    }

    private fun showData(items: List<Report>, screenType: String) {
        val sorted = when {
            screenType == "ADMIN" -> items.sortedByDescending { it.empathyCount }
            isSortedByEmpathy     -> items.sortedByDescending { it.empathyCount }
            else                  -> items.sortedByDescending { it.createdAt ?: "" }
        }

        binding.tvTotalCount.text = "전체 ${sorted.size}"

        if (sorted.isEmpty()) {
            binding.rvReportList.visibility     = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = when (screenType) {
                "MY_REPORTS"         -> "아직 등록한 제보가 없어요."
                "EMPATHIZED_REPORTS" -> "아직 공감한 제보가 없어요."
                "ADMIN"              -> "등록된 제보가 없어요."
                else                 -> ""
            }
            // ADMIN은 빈 상태 버튼 숨기기
            binding.btnEmptyAction.visibility = if (screenType == "ADMIN") View.GONE else View.VISIBLE
            binding.btnEmptyAction.text = when (screenType) {
                "MY_REPORTS"         -> "제보하러 가기"
                "EMPATHIZED_REPORTS" -> "지도 보러 가기"
                else                 -> "이동"
            }
            binding.btnEmptyAction.setOnClickListener {
                when (screenType) {
                    "MY_REPORTS"         -> startActivity(Intent(this, ReportActivity::class.java))
                    "EMPATHIZED_REPORTS" -> startActivity(Intent(this, MainMapActivity::class.java))
                }
                finish()
            }
        } else {
            binding.rvReportList.visibility     = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            adapter.updateList(sorted)
        }
    }
}