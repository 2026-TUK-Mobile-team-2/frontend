package com.example.fixsiheung

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fixsiheung.databinding.ActivityMyPageBinding

class MyPageActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMyPageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

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

                    /*val intent = Intent(this, ReportActivity::class.java)
                    startActivity(intent)*/

                    true
                }

                R.id.nav_mypage -> {
                    true
                }

                else -> false
            }
        }

        binding.menuReportList.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_list)
            tvMenuTitle.text = "제보 내역"
            // 터치 시 구동할 코드 작성
        }

        binding.menuEmpathyReport.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_favorite)
            tvMenuTitle.text = "공감한 제보"
                // 터치 시 구동할 코드 작성
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
            tvMenuTitle.text = "로그아웃"
            // 터치 시 구동할 코드 작성
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}