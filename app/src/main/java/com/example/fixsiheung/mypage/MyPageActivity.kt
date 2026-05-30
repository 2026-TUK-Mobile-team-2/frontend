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
import com.example.fixsiheung.ReportListActivity
import com.example.fixsiheung.databinding.ActivityMyPageBinding
import com.example.fixsiheung.model.MyPageResponse
import com.example.fixsiheung.network.RetrofitClient
import kotlin.jvm.java

class MyPageActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMyPageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        loadMyPageData()
        updateNotificationBadge()

        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", null)
        val isAdmin = userId == "admin_user"
        binding.menuAdmin.root.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.bottomNavigation.selectedItemId = R.id.nav_mypage

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home   -> { startActivity(Intent(this, MainMapActivity::class.java)); true }
                R.id.nav_list   -> { startActivity(Intent(this, ReportListActivity::class.java)); true}
                R.id.nav_report -> { startActivity(Intent(this, ReportActivity::class.java)); true }
                R.id.nav_mypage -> true
                else            -> false
            }
        }

        // 상단 타이틀 클릭 → 지도
        binding.tvToolbarTitle.setOnClickListener {
            startActivity(Intent(this, MainMapActivity::class.java))
        }

        // 상단 종 아이콘 → 알림
        binding.ivTopBadge.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.menuReportList.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_list)
            tvMenuTitle.text = "제보 내역"
            root.setOnClickListener {
                startActivity(Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "MY_REPORTS")
                })
            }
        }

        binding.menuEmpathyReport.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_favorite)
            tvMenuTitle.text = "공감한 제보"
            root.setOnClickListener {
                startActivity(Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "EMPATHIZED_REPORTS")
                })
            }
        }

        binding.menuNotification.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_notifications)
            tvMenuTitle.text = "알림 설정"
            root.setOnClickListener {
                startActivity(Intent(this@MyPageActivity, NotificationSettingActivity::class.java))
            }
        }

        binding.menuInquiry.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_support)
            tvMenuTitle.text = "문의 하기"
            // TODO: 문의하기 화면 구현
        }

        binding.menuLogout.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_logout)
            ivMenuIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            tvMenuTitle.text = "로그아웃"
            tvMenuTitle.setTextColor("#EF4444".toColorInt())
            root.setOnClickListener {
                LogoutDialogFragment {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    startActivity(
                        Intent(this@MyPageActivity, com.example.fixsiheung.auth.LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }.show(supportFragmentManager, "logout")
            }
        }

        binding.menuAdmin.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_list)
            tvMenuTitle.text = "관리자 페이지"
            root.setOnClickListener {
                startActivity(Intent(this@MyPageActivity, MyListActivity::class.java).apply {
                    putExtra("SCREEN_TYPE", "ADMIN")
                })
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
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
                        runOnUiThread {
                            binding.tvNickname.text         = data.user.nickname
                            binding.tvLikesCount.text       = "❤️ ${data.my_reports.sumOf { it.empathyCount }}"
                            binding.tvStatEmpathyCount.text = data.my_reports.size.toString()
                            binding.tvStatResolvedCount.text = data.my_reports.count { it.status == "처리완료" || it.status == "완료"}.toString()
                        }
                    }
                }
                override fun onFailure(call: retrofit2.Call<MyPageResponse>, t: Throwable) {
                    Log.e("MyPage", "API 실패: ${t.message}")
                }
            })
    }

    private fun updateNotificationBadge() {
        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("user_id", null) ?: return

        RetrofitClient.apiService.getNotifications(userId)
            .enqueue(object : retrofit2.Callback<List<com.example.fixsiheung.model.Notification>> {
                override fun onResponse(call: retrofit2.Call<List<com.example.fixsiheung.model.Notification>>, response: retrofit2.Response<List<com.example.fixsiheung.model.Notification>>) {
                    if (response.isSuccessful) {
                        val readPrefs = getSharedPreferences("notification_read_prefs", MODE_PRIVATE)
                        val unreadCount = response.body()?.count { notif ->
                            !readPrefs.getBoolean("notification_read_${notif.id}", notif.isRead)
                        } ?: 0

                        runOnUiThread {
                            if (unreadCount > 0) {
                                binding.tvNotificationBadge.visibility = View.VISIBLE
                                binding.tvNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                            } else {
                                binding.tvNotificationBadge.visibility = View.GONE
                            }
                        }
                    }
                }
                override fun onFailure(call: retrofit2.Call<List<com.example.fixsiheung.model.Notification>>, t: Throwable) {}
            })
    }
}