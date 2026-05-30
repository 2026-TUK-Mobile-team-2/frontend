package com.example.fixsiheung.mypage

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixsiheung.databinding.ActivityNotificationBinding
import com.example.fixsiheung.model.Notification
import com.example.fixsiheung.network.RetrofitClient

class NotificationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityNotificationBinding.inflate(layoutInflater) }
    private lateinit var adapter: NotificationAdapter
    private val readPrefs by lazy { getSharedPreferences("notification_read_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = NotificationAdapter(emptyList(), readPrefs)
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("user_id", null)

        if (userId == null) {
            // 로그인 안 된 상태 → 빈 화면
            showData(emptyList())
            return
        }

        RetrofitClient.apiService.getNotifications(userId)
            .enqueue(object : retrofit2.Callback<List<Notification>> {
                override fun onResponse(
                    call: retrofit2.Call<List<Notification>>,
                    response: retrofit2.Response<List<Notification>>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: emptyList()
                        runOnUiThread { showData(data) }
                    }
                }
                override fun onFailure(call: retrofit2.Call<List<Notification>>, t: Throwable) {
                    Log.e("Notification", "API 실패: ${t.message}")
                    // API 실패 시 mock 데이터로 대체
                    runOnUiThread { showData(getMockData()) }
                }
            })
    }

    private fun showData(data: List<Notification>) {
        if (data.isEmpty()) {
            binding.tvEmptyNotification.visibility = View.VISIBLE
            binding.rvNotifications.visibility     = View.GONE
        } else {
            binding.tvEmptyNotification.visibility = View.GONE
            binding.rvNotifications.visibility     = View.VISIBLE
            adapter.updateList(data)
        }
    }

    // TODO: API 완성되면 제거
    private fun getMockData() = listOf(
        Notification(1, "status",        "제보 상태 변경", "쓰레기 무단투기가 처리중으로 변경되었습니다.", null, false, "2026-05-30 10:00:00"),
        Notification(2, "empathy",       "공감 알림",      "내 제보에 누군가 공감했습니다.",             null, false, "2026-05-30 08:00:00"),
        Notification(3, "new_complaint", "새로운 민원",    "주변에 새로운 민원이 등록되었습니다.",        null, true,  "2026-05-29 12:00:00"),
    )
}