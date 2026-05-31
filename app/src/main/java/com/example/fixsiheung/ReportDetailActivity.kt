package com.example.fixsiheung

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.databinding.ActivityReportDetailBinding
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding

    enum class ProcessStatus { RECEIVED, PROCESSING, COMPLETED }

    private var complaintId: Int = -1
    private var userId: String = ""
    private var currentEmpathyCount: Int = 0
    private var isEmpathized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        complaintId = intent.getIntExtra("COMPLAINT_ID", -1)
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (complaintId == -1) {
            Toast.makeText(this, "민원 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReportDetail(complaintId)
        setupEmpathyButton()
    }

    private fun loadReportDetail(complaintId: Int) {
        RetrofitClient.apiService.getComplaintDetail(complaintId)
            .enqueue(object : Callback<Report> {
                override fun onResponse(call: Call<Report>, response: Response<Report>) {
                    if (response.isSuccessful && response.body() != null) {
                        bindReport(response.body()!!)
                    } else {
                        Toast.makeText(
                            this@ReportDetailActivity,
                            "상세 정보를 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Report>, t: Throwable) {
                    Toast.makeText(
                        this@ReportDetailActivity,
                        "서버 통신 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun bindReport(report: Report) {
        binding.ivHeroImage.setImageResource(R.drawable.block)

        val status = report.status ?: "접수중"
        binding.tvStatusBadge.text = status
        binding.tvStatusBadge.setBackgroundResource(
            when (status) {
                "처리완료" -> R.drawable.status_complete_bg
                "처리중" -> R.drawable.status_processing_badge
                else -> R.drawable.status_pending_bg
            }
        )

        binding.tvTitle.text = report.title
        binding.tvDate.text = "📅 ${report.createdAt?.take(10) ?: "-"}"
        binding.tvAuthor.text = "👤 ${maskName(report.nickname ?: report.userId)}"
        binding.tvViewCount.text = ""

        bindProgressTimeline(status)

        binding.tvDescription.text = report.description
        binding.tvAddress.text = "📍 ${report.address?.ifEmpty { "주소 정보 없음" } ?: "주소 정보 없음"}"

        binding.tvDeptTitle.text = "담당 부서 답변"
        binding.tvDeptInfo.text = ""
        binding.tvDeptReply.text = "담당 부서의 답변이 등록되면 여기에 표시됩니다."

        currentEmpathyCount = report.empathyCount
        binding.tvEmpathyCount.text = "💙 $currentEmpathyCount"
    }

    private fun bindProgressTimeline(status: String) {
        val processStatus = when (status) {
            "처리완료" -> ProcessStatus.COMPLETED
            "처리중" -> ProcessStatus.PROCESSING
            else -> ProcessStatus.RECEIVED
        }

        binding.tvStep1Circle.setBackgroundResource(R.drawable.progress_complete_circle)
        binding.tvStep1Circle.setTextColor(Color.WHITE)
        binding.tvStep1Label.setTextColor(Color.parseColor("#00696B"))

        binding.tvStep2Circle.setBackgroundResource(
            when (processStatus) {
                ProcessStatus.RECEIVED -> R.drawable.progress_pending_circle
                ProcessStatus.PROCESSING -> R.drawable.progress_current_circle
                ProcessStatus.COMPLETED -> R.drawable.progress_complete_circle
            }
        )
        binding.tvStep2Label.setTextColor(
            if (isAtLeast(processStatus, ProcessStatus.PROCESSING)) Color.parseColor("#1E293B")
            else Color.parseColor("#64748B")
        )

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

        binding.vLine1.setBackgroundColor(
            if (isAtLeast(processStatus, ProcessStatus.PROCESSING)) Color.parseColor("#65C0C1")
            else Color.parseColor("#CBD5E1")
        )
        binding.vLine2.setBackgroundColor(
            if (processStatus == ProcessStatus.COMPLETED) Color.parseColor("#65C0C1")
            else Color.parseColor("#CBD5E1")
        )
    }

    private fun setupEmpathyButton() {
        updateEmpathyButtonUi()

        binding.btnEmpathy.setOnClickListener {
            if (complaintId == -1) return@setOnClickListener

            if (userId.isBlank()) {
                Toast.makeText(this, "로그인 정보가 없어 공감을 처리할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isEmpathized) {
                RetrofitClient.apiService.addEmpathy(
                    complaintId,
                    mapOf("user_id" to userId)
                ).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(
                        call: Call<Map<String, String>>,
                        response: Response<Map<String, String>>
                    ) {
                        if (response.isSuccessful) {
                            isEmpathized = true
                            currentEmpathyCount += 1
                            updateEmpathyButtonUi()
                        } else {
                            Toast.makeText(this@ReportDetailActivity, "공감 처리 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        Toast.makeText(this@ReportDetailActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                RetrofitClient.apiService.removeEmpathy(
                    complaintId,
                    mapOf("user_id" to userId)
                ).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(
                        call: Call<Map<String, String>>,
                        response: Response<Map<String, String>>
                    ) {
                        if (response.isSuccessful) {
                            isEmpathized = false
                            currentEmpathyCount = maxOf(0, currentEmpathyCount - 1)
                            updateEmpathyButtonUi()
                        } else {
                            Toast.makeText(this@ReportDetailActivity, "공감 취소 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        Toast.makeText(this@ReportDetailActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun updateEmpathyButtonUi() {
        binding.tvEmpathyCount.text = "💙 $currentEmpathyCount"

        if (isEmpathized) {
            binding.btnEmpathy.text = "공감 취소"
            binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_unselected_bg)
        } else {
            binding.btnEmpathy.text = "공감하기"
            binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_selected_bg)
        }
    }

    private fun isAtLeast(current: ProcessStatus, target: ProcessStatus): Boolean {
        return current.ordinal >= target.ordinal
    }

    private fun maskName(name: String): String {
        if (name.length <= 1) return name
        return "${name.first()}*${name.last()}"
    }
}