package com.example.fixsiheung.mypage

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityNotificationBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // TODO: 알림 API 연동 후 데이터 불러오기
        // 지금은 빈 화면
        binding.tvEmptyNotification.visibility = View.VISIBLE
        binding.rvNotifications.visibility     = View.GONE
    }
}