package com.example.fixsiheung

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fixsiheung.databinding.ActivityReportDetailBinding

class ReportDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 📦 어댑터가 보낸 택배 데이터 꺼내기
        val title = intent.getStringExtra("detail_title")
        val content = intent.getStringExtra("detail_content")
        val category = intent.getStringExtra("detail_category")
        val status = intent.getStringExtra("detail_status")

        val writer = intent.getStringExtra("detail_writer") ?: "시민님" //작성자
        var currentViewCount = intent.getIntExtra("detail_view", 0) // 어댑터에서 +1 된 조회수를 받아옴
        var currentLikeCount = intent.getIntExtra("detail_like", 0) //공감수

        // 📱 화면 레이아웃(XML)의 뷰들에 데이터 쏙쏙 넣어주기
        binding.detailTitle.text = title
        binding.detailContent.text = content
        binding.detailCategory.text = category
        binding.detailStatus.text = status

        //currentViewCount++ 이렇게 하면 현재화면에서만 유지, 접속으로 하려면 어댑터.kt DB들어오면 삭제
        binding.detailInfo.text = "조회수 $currentViewCount | 작성자: $writer" //| 공감 ${currentLikeCount}개"


        binding.likebtn.text = "공감하기 $currentLikeCount"
        binding.likebtn.setOnClickListener {
            currentLikeCount++
            binding.likebtn.text = "공감하기 $currentLikeCount"
        }
    }
}
