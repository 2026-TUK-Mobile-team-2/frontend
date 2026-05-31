package com.example.fixsiheung

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide // 💡 Glide 임포트 추가!
import com.example.fixsiheung.databinding.ActivityReportDetailBinding
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.model.StatusUpdateRequest
import com.example.fixsiheung.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding

    enum class ProcessStatus { RECEIVED, PROCESSING, COMPLETED }


    private var reportCategoryId: Int = 1
    private var complaintId: Int = -1
    private var userId: String = ""
    private var isAdmin: Boolean = false
    private var currentEmpathyCount: Int = 0
    private var isEmpathized: Boolean = false

    private var kakaoMap: KakaoMap? = null
    private var reportLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KakaoMapSdk.init(this, "f0f61ee911139544fa9381c376c79833")
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception?) {}
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                // 리포트 로드 후 위치 있으면 마커 표시
                reportLatLng?.let { showMarkerOnMap(it) }
            }
        })

        binding.mapView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.btnBack.setOnClickListener { finish() }

        complaintId = intent.getIntExtra("COMPLAINT_ID", -1)

        // 💡 [수정] AppPrefs를 최우선으로 사용하고, 없으면 인텐트에서 가져옵니다.
        userId = AppPrefs.getUserId(this).ifBlank {
            intent.getStringExtra("USER_ID") ?: ""
        }

        // 관리자 여부 판단
        isAdmin = userId == "admin_user"

        if (complaintId == -1) {
            Toast.makeText(this, "민원 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 💡 [유지] 팀원분이 만드신 관리자면 상태 변경 버튼 표시 로직
        if (isAdmin) {
            binding.layoutAdminStatus.visibility = View.VISIBLE
            binding.btnEmpathy.visibility = View.GONE
        } else {
            binding.layoutAdminStatus.visibility = View.GONE
            binding.btnEmpathy.visibility = View.VISIBLE
        }

        loadReportDetail(complaintId)
        setupEmpathyButton()
        setupAdminStatusButtons()
    }

    private fun loadReportDetail(complaintId: Int) {
        // 💡 [수정] DB 외래키 에러 방지를 위해, 빈 아이디일 경우 실제 존재하는 아이디로 시연합니다.
        val currentUserId = userId.ifBlank { "minjae404" }

        RetrofitClient.apiService.getComplaintDetail(complaintId, currentUserId)
            .enqueue(object : Callback<Report> {
                override fun onResponse(call: Call<Report>, response: Response<Report>) {
                    if (response.isSuccessful && response.body() != null) {
                        val report = response.body()!!
                        isEmpathized = report.isEmpathized
                        bindReport(report)
                        updateEmpathyButtonUi()
                    } else {
                        Toast.makeText(this@ReportDetailActivity, "상세 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Report>, t: Throwable) {
                    Toast.makeText(this@ReportDetailActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    private fun showMarkerOnMap(latLng: LatLng) {
        val map = kakaoMap ?: return
        map.moveCamera(CameraUpdateFactory.newCenterPosition(latLng, 16))

        val markerDrawableId = when (reportCategoryId) {
            1 -> R.drawable.ic_marker_trash
            2 -> R.drawable.ic_marker_facilities
            3 -> R.drawable.ic_marker_danger
            4 -> R.drawable.ic_marker_parking
            5 -> R.drawable.ic_marker_noise
            else -> R.drawable.ic_marker_any
        }

        // bitmap으로 변환
        val bitmap = vectorToBitmap(markerDrawableId)

        val labelManager = map.labelManager ?: return
        val style = labelManager.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)
            )
        ) ?: return

        labelManager.layer?.addLabel(
            LabelOptions.from(latLng).setStyles(style)
        )
    }

    private fun vectorToBitmap(drawableId: Int): android.graphics.Bitmap {
        val drawable = androidx.core.content.ContextCompat.getDrawable(this, drawableId)!!
        val maxPx = (48 * resources.displayMetrics.density).toInt()
        var w = drawable.intrinsicWidth
        var h = drawable.intrinsicHeight
        if (w > maxPx || h > maxPx) {
            if (w > h) { h = (h * (maxPx.toFloat() / w)).toInt(); w = maxPx }
            else { w = (w * (maxPx.toFloat() / h)).toInt(); h = maxPx }
        }
        return android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(android.graphics.Canvas(it))
        }
    }

    private fun bindReport(report: Report) {
        // 💡 [수정] 하드코딩된 이미지 대신 Glide를 사용하여 서버의 진짜 이미지를 로드합니다.
        if (!report.imageUrl.isNullOrEmpty()) {
            val fixedUrl = report.imageUrl.replace("127.0.0.1", "192.168.0.41")
            Glide.with(this)
                .load(fixedUrl)
                .placeholder(R.drawable.block)
                .error(R.drawable.block)
                .centerCrop()
                .into(binding.ivHeroImage)
        } else {
            binding.ivHeroImage.setImageResource(R.drawable.block)
        }

        reportCategoryId = report.categoryId
        reportLatLng = LatLng.from(report.latitude, report.longitude)
        kakaoMap?.let { showMarkerOnMap(reportLatLng!!) }

        val status = report.status ?: "접수중"
        binding.tvStatusBadge.text = status
        binding.tvStatusBadge.setBackgroundResource(
            when (status) {
                "처리완료", "완료" -> R.drawable.status_complete_bg
                "처리중"   -> R.drawable.status_processing_badge
                else       -> R.drawable.status_pending_bg
            }
        )

        binding.tvTitle.text   = report.title
        binding.tvDate.text    = "📅 ${report.createdAt?.take(10) ?: "-"}"
        binding.tvAuthor.text  = "👤 ${maskName(report.nickname ?: report.userId)}"
        binding.tvViewCount.text = ""

        bindProgressTimeline(status)

        binding.tvDescription.text = report.description
        binding.tvAddress.text = "📍 ${report.address?.ifEmpty { "주소 정보 없음" } ?: "주소 정보 없음"}"

        binding.tvDeptTitle.text  = "담당 부서 답변"
        binding.tvDeptInfo.text   = ""
        binding.tvDeptReply.text  = "담당 부서의 답변이 등록되면 여기에 표시됩니다."

        currentEmpathyCount = report.empathyCount
        binding.tvEmpathyCount.text = "💙 $currentEmpathyCount"
    }

    private fun bindProgressTimeline(status: String) {
        val processStatus = when (status) {
            "처리완료", "완료" -> ProcessStatus.COMPLETED
            "처리중"   -> ProcessStatus.PROCESSING
            else       -> ProcessStatus.RECEIVED
        }

        binding.tvStep1Circle.setBackgroundResource(R.drawable.progress_complete_circle)
        binding.tvStep1Circle.setTextColor(Color.WHITE)
        binding.tvStep1Label.setTextColor(Color.parseColor("#00696B"))

        binding.tvStep2Circle.setBackgroundResource(
            when (processStatus) {
                ProcessStatus.RECEIVED   -> R.drawable.progress_pending_circle
                ProcessStatus.PROCESSING -> R.drawable.progress_current_circle
                ProcessStatus.COMPLETED  -> R.drawable.progress_complete_circle
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

    // 💡 [유지] 팀원분이 만드신 관리자 상태 변경 로직
    private fun setupAdminStatusButtons() {
        binding.btnStatusProcessing.setOnClickListener {
            updateStatus("처리중")
        }
        binding.btnStatusComplete.setOnClickListener {
            updateStatus("처리완료")
        }
    }

    private fun updateStatus(newStatus: String) {
        // 관리자가 상태를 변경할 때는 자신의 아이디(admin_user)를 보냅니다.
        val targetUserId = userId.ifBlank { "admin_user" }

        RetrofitClient.apiService.updateComplaintStatus(
            complaintId,
            StatusUpdateRequest(userId = targetUserId, status = newStatus)
        ).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@ReportDetailActivity, "'$newStatus'으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        loadReportDetail(complaintId)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ReportDetailActivity, "상태 변경 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@ReportDetailActivity, "서버 통신 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupEmpathyButton() {
        updateEmpathyButtonUi()

        binding.btnEmpathy.setOnClickListener {
            if (complaintId == -1) return@setOnClickListener

            // 💡 [수정] 공감 요청 시에도 실재하는 아이디를 기본값으로 사용하여 DB 충돌을 막습니다.
            val targetUserId = userId.ifBlank { "minjae404" }

            if (!isEmpathized) {
                isEmpathized = true
                currentEmpathyCount += 1
                updateEmpathyButtonUi()

                RetrofitClient.apiService.addEmpathy(
                    complaintId,
                    mapOf("user_id" to targetUserId)
                ).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (!response.isSuccessful) rollbackEmpathyUi(false)
                    }
                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        rollbackEmpathyUi(false)
                    }
                })
            } else {
                isEmpathized = false
                currentEmpathyCount = maxOf(0, currentEmpathyCount - 1)
                updateEmpathyButtonUi()

                RetrofitClient.apiService.removeEmpathy(
                    complaintId,
                    mapOf("user_id" to targetUserId)
                ).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (!response.isSuccessful) rollbackEmpathyUi(true)
                    }
                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        rollbackEmpathyUi(true)
                    }
                })
            }
        }
    }

    private fun rollbackEmpathyUi(toEmpathized: Boolean) {
        Toast.makeText(this, "서버와의 통신에 실패하여 상태가 복구되었습니다.", Toast.LENGTH_SHORT).show()
        isEmpathized = toEmpathized
        if (toEmpathized) currentEmpathyCount += 1
        else currentEmpathyCount = maxOf(0, currentEmpathyCount - 1)
        updateEmpathyButtonUi()
    }

    private fun updateEmpathyButtonUi() {
        binding.tvEmpathyCount.text = "💙 $currentEmpathyCount"

        if (isEmpathized) {
            binding.btnEmpathy.text = "공감 취소"
            binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_unselected_bg)
            binding.btnEmpathy.setTextColor(Color.parseColor("#64748B"))
        } else {
            binding.btnEmpathy.text = "공감하기"
            binding.btnEmpathy.setBackgroundResource(R.drawable.re_category_selected_bg)
            binding.btnEmpathy.setTextColor(Color.WHITE)
        }
    }

    private fun isAtLeast(current: ProcessStatus, target: ProcessStatus) =
        current.ordinal >= target.ordinal

    private fun maskName(name: String): String {
        if (name.length <= 1) return name
        return "${name.first()}*${name.last()}"
    }
}