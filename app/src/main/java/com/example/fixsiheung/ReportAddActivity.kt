package com.example.fixsiheung

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.databinding.ActivityReportAddBinding

class ReportAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportAddBinding
    private var selectedCategoryId: Int = 1 // 기본값: 쓰레기

    private val categoryViews by lazy {
        listOf(
            binding.categoryTrash,    // 1
            binding.categoryDamaged,  // 2
            binding.categoryDanger,   // 3
            binding.categoryRoad,     // 4
            binding.categoryNoise,    // 5
            binding.categoryOther     // 6
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateCategoryUI(1)

        // 닫기 버튼
        binding.closeButton.setOnClickListener {
            finish()
        }

        // ─── 카테고리 클릭 리스너 설정 ───
        binding.categoryTrash.setOnClickListener { selectCategory(1) }
        binding.categoryDamaged.setOnClickListener { selectCategory(2) }
        binding.categoryDanger.setOnClickListener { selectCategory(3) }
        binding.categoryRoad.setOnClickListener { selectCategory(4) }
        binding.categoryNoise.setOnClickListener { selectCategory(5) }
        binding.categoryOther.setOnClickListener { selectCategory(6) }

        // 제보하기 버튼
        binding.submitButton.setOnClickListener {
            val title = binding.reportTitle.text.toString().trim()
            val content = binding.reportDescription.text.toString().trim()
            val location = "시흥시 정왕동 부근"

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 상세 내용을 모두 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putExtra("intent_title", title)
                putExtra("intent_category", selectedCategoryId)
                putExtra("intent_location", location)
                putExtra("intent_content", content)
                putExtra("intent_writer", "익명") // 작성자 정보 추가
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun selectCategory(id: Int) {
        selectedCategoryId = id
        updateCategoryUI(id)
    }

    private fun updateCategoryUI(selectedIndex: Int) {
        categoryViews.forEachIndexed { index, view ->
            if (index == selectedIndex - 1) {
                view.setBackgroundResource(R.drawable.re_category_selected_bg)
            } else {
                view.setBackgroundResource(R.drawable.re_category_unselected_bg)
            }
        }
    }
}