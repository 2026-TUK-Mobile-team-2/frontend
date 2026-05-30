package com.example.fixsiheung

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.databinding.ActivityReportDetailBinding
import com.example.fixsiheung.model.Report

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding

    enum class ProcessStatus { RECEIVED, PROCESSING, COMPLETED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val complaintId = intent.getIntExtra("COMPLAINT_ID", -1)
        val report = getReportById(complaintId)

        if (report != null) {
            bindReport(report)
        }

        setupEmpathyButton()
    }

    // ───────────────────────────────────────────────
    // 데이터 바인딩
    // ───────────────────────────────────────────────

    private fun bindReport(report: Report) {
        // 히어로 이미지 (현재 로컬 drawable 사용, 추후 Glide/Coil 연동 시 교체)
        binding.ivHeroImage.setImageResource(R.drawable.block)

        // 상태 뱃지
        val status = report.status ?: "접수중"
        binding.tvStatusBadge.text = status
        binding.tvStatusBadge.setBackgroundResource(
            when (status) {
                "처리완료" -> R.drawable.status_complete_bg
                "처리중"   -> R.drawable.status_processing_badge
                else       -> R.drawable.status_pending_bg
            }
        )

        // 제목
        binding.tvTitle.text = report.title

        // 메타데이터
        binding.tvDate.text   = "📅 ${report.createdAt?.take(10) ?: "-"}"
        binding.tvAuthor.text = "👤 ${maskName(report.nickname ?: report.userId)}"
        binding.tvViewCount.text = ""   // 서버에 viewCount 없으므로 빈칸 처리

        // 처리 현황 타임라인
        bindProgressTimeline(status)

        // 민원 내용
        binding.tvDescription.text = report.description

        // 위치 주소
        binding.tvAddress.text = "📍 ${report.address?.ifEmpty { "주소 정보 없음" } ?: "주소 정보 없음"}"

        // 담당 부서 답변 — 서버 미지원 필드이므로 고정 안내 문구
        binding.tvDeptTitle.text = "담당 부서 답변"
        binding.tvDeptInfo.text  = ""
        binding.tvDeptReply.text = "담당 부서의 답변이 등록되면 여기에 표시됩니다."

        // 공감 수
        binding.tvEmpathyCount.text = "💙 ${report.empathyCount}"
    }

    // ───────────────────────────────────────────────
    // 처리 현황 타임라인
    // ───────────────────────────────────────────────

    private fun bindProgressTimeline(status: String) {
        val processStatus = when (status) {
            "처리완료" -> ProcessStatus.COMPLETED
            "처리중"   -> ProcessStatus.PROCESSING
            else       -> ProcessStatus.RECEIVED
        }

        // 접수완료 스텝 (항상 완료 상태)
        binding.tvStep1Circle.setBackgroundResource(R.drawable.progress_complete_circle)
        binding.tvStep1Circle.setTextColor(Color.WHITE)
        binding.tvStep1Label.setTextColor(Color.parseColor("#00696B"))

        // 처리중 스텝
        binding.tvStep2Circle.setBackgroundResource(
            when (processStatus) {
                ProcessStatus.RECEIVED   -> R.drawable.progress_pending_circle
                ProcessStatus.PROCESSING -> R.drawable.progress_current_circle
                ProcessStatus.COMPLETED  -> R.drawable.progress_complete_circle
            }
        )
        binding.tvStep2Label.setTextColor(
            if (processStatus >= ProcessStatus.PROCESSING) Color.parseColor("#1E293B")
            else Color.parseColor("#64748B")
        )

        // 처리완료 스텝
        binding.tvStep3Circle.setBackgroundResource(
            if (processStatus == ProcessStatus.COMPLETED) R.drawable.progress_complete_circle
            else R.drawable.progress_pending_circle
        )
        binding.tvStep3Circle.setTextColor(
            if (processStatus == ProcessStatus.COMPLETED) Color.WHITE
            else Color.parseColor("#64748B")
        )
        binding.tvStep3Label.setTextColor(
            if (processStatus == ProcessStatus.COMPLETED) Color.parseColor("#00696B")
            else Color.parseColor("#64748B")
        )

        // 연결선 색상
        binding.vLine1.setBackgroundColor(
            if (processStatus >= ProcessStatus.PROCESSING) Color.parseColor("#65C0C1")
            else Color.parseColor("#CBD5E1")
        )
        binding.vLine2.setBackgroundColor(
            if (processStatus == ProcessStatus.COMPLETED) Color.parseColor("#65C0C1")
            else Color.parseColor("#CBD5E1")
        )
    }

    // ───────────────────────────────────────────────
    // 공감 버튼
    // ───────────────────────────────────────────────

    private var isEmpathized = false

    private fun setupEmpathyButton() {
        binding.btnEmpathy.setOnClickListener {
            isEmpathized = !isEmpathized
            if (isEmpathized) {
                binding.btnEmpathy.text = "공감 취소"
                binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_unselected_bg)
                val current = extractEmpathyCount(binding.tvEmpathyCount.text.toString())
                binding.tvEmpathyCount.text = "💙 ${current + 1}"
            } else {
                binding.btnEmpathy.text = "공감하기"
                binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_selected_bg)
                val current = extractEmpathyCount(binding.tvEmpathyCount.text.toString())
                binding.tvEmpathyCount.text = "💙 ${maxOf(0, current - 1)}"
            }
        }
    }

    // ───────────────────────────────────────────────
    // 유틸
    // ───────────────────────────────────────────────

    private fun extractEmpathyCount(text: String): Int =
        text.replace("💙", "").trim().toIntOrNull() ?: 0

    // 닉네임/이름 마스킹: "홍길동" → "홍*동"
    private fun maskName(name: String): String {
        if (name.length <= 1) return name
        return "${name.first()}*${name.last()}"
    }

    // 더미 데이터 — 실제 앱에서는 Intent Parcelable 또는 ViewModel/Repository로 교체
    private fun getReportById(id: Int): Report? {
        return Report(
            complaintId  = id,
            userId       = "user01",
            categoryId   = 2,
            title        = "파손된 보도블록 수리 요청",
            description  = "보도블록 일부가 심하게 파손되어 보행자 안전사고 위험이 큽니다. 특히 야간에는 시야 확보가 어려워 빠른 조치가 필요합니다.",
            latitude     = 37.3514,
            longitude    = 126.7424,
            address      = "경기도 시흥시 정왕동 배곧생명공원 입구",
            empathyCount = 128,
            status       = "처리중",
            createdAt    = "2024.05.20",
            nickname     = "김*은",
            imageUrl     = null
        )
    }
}