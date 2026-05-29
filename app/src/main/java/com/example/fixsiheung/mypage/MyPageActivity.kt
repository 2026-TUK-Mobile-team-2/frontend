package com.example.fixsiheung.mypage

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.R
import com.example.fixsiheung.ReportActivity
import com.example.fixsiheung.databinding.ActivityMyPageBinding
import com.example.fixsiheung.model.MyPageResponse
import com.example.fixsiheung.network.RetrofitClient

class MyPageActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMyPageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        loadMyPageData()

        /*************************************************************************************************************/
        // TODO: 실제 로그인 API 연동 후 관리자 여부를 서버에서 받아와야 합니다.
        val isAdmin = true // 임시값, 추후 로그인 유저 정보로 교체
        binding.menuAdmin.root.visibility = if (isAdmin) View.VISIBLE else View.GONE
        /*************************************************************************************************************/


        binding.bottomNavigation.selectedItemId = R.id.nav_mypage

        // 하단 네비 클릭 이벤트
        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    val intent = Intent(this, MainMapActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_list -> {

                    /*val intent = Intent(this, ListActivity::class.java)
                    startActivity(intent)*/

                    true
                }

                R.id.nav_report -> {

                    val intent = Intent(this, ReportActivity::class.java)
                    startActivity(intent)

                    true
                }

                R.id.nav_mypage -> {
                    true
                }

                else -> false
            }
        }

        binding.tvToolbarTitle.setOnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            startActivity(intent)
        }

        binding.menuReportList.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_list)
            tvMenuTitle.text = "제보 내역"
            binding.menuReportList.root.setOnClickListener {
                val intent = Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "MY_REPORTS")
                }
                startActivity(intent)
            }
        }

        binding.menuEmpathyReport.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_favorite)
            tvMenuTitle.text = "공감한 제보"
            root.setOnClickListener {
                val intent = Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "EMPATHIZED_REPORTS")
                }
                startActivity(intent)
            }
        }

        binding.menuNotification.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_notifications)
            tvMenuTitle.text = "알림 설정"
            // 터치 시 구동할 코드 작성
        }

        binding.menuInquiry.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_support)
            tvMenuTitle.text = "문의 하기"
            // 터치 시 구동할 코드 작성
        }

        binding.menuLogout.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_logout)
            ivMenuIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            tvMenuTitle.text = "로그아웃"
            tvMenuTitle.setTextColor("#EF4444".toColorInt())
            binding.menuLogout.root.setOnClickListener {
                LogoutDialogFragment {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    val intent = Intent(
                        this@MyPageActivity,
                        com.example.fixsiheung.auth.LoginActivity::class.java
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.show(supportFragmentManager, "logout")

            }
        }

        binding.menuAdmin.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_list)
            tvMenuTitle.text = "관리자 페이지"
            root.setOnClickListener {
                val intent = Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "ADMIN")
                }
                startActivity(intent)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadMyPageData() {
        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("user_id", null) ?: return

        RetrofitClient.apiService.getMyPage(userId)
            .enqueue(object : retrofit2.Callback<MyPageResponse> {
                override fun onResponse(
                    call: retrofit2.Call<MyPageResponse>,
                    response: retrofit2.Response<MyPageResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: return
                        binding.tvNickname.text = data.user.nickname
                        binding.tvLikesCount.text =
                            "❤️ ${data.my_reports.sumOf { it.empathyCount }}"
                        binding.tvStatEmpathyCount.text = data.my_reports.size.toString()
                        // TODO: Report 모델에 status 추가되면 아래 주석 해제
                        // binding.tvStatResolvedCount.text = data.my_reports.count { it.status == "처리완료" }.toString()
                    }
                }

                override fun onFailure(call: retrofit2.Call<MyPageResponse>, t: Throwable) {
                    Log.e("MyPage", "API 실패: ${t.message}")
                }
            })
    }
}