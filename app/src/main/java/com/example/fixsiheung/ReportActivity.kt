package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ReportActivity : AppCompatActivity() {

    // ───────────────────────────────────────────
    // View references
    // ───────────────────────────────────────────
    private lateinit var closeButton: ImageButton
    private lateinit var photoFrame: FrameLayout
    private lateinit var reportTitle: EditText
    private lateinit var reportDescription: EditText
    private lateinit var submitButton: Button

    // 카테고리 버튼 목록 (GridLayout 내 LinearLayout들)
    private lateinit var categoryLayouts: List<LinearLayout>

    // ───────────────────────────────────────────
    // State
    // ───────────────────────────────────────────
    private var selectedCategory: String? = null
    private var selectedPhotoUri: Uri? = null

    private val categories = listOf(
        "쓰레기", "시설파손", "안전위험", "도로", "소음", "기타"
    )

    // ───────────────────────────────────────────
    // Activity result launchers
    // ───────────────────────────────────────────

    /** 갤러리에서 사진 선택 */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handlePhotoSelected(it) }
        }

    /** 카메라 권한 요청 */
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pickImageLauncher.launch("image/*")
            else Toast.makeText(this, "사진 첨부를 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    // ───────────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        bindViews()
        setupCategoryButtons()
        setupPhotoUpload()
        setupLocationButton()
        setupSubmitButton()
        setupCloseButton()
    }

    // ───────────────────────────────────────────
    // View binding
    // ───────────────────────────────────────────

    private fun bindViews() {
        closeButton       = findViewById(R.id.closeButton)
        photoFrame        = findViewById(R.id.photoFrame)        // FrameLayout id 추가 필요
        reportTitle       = findViewById(R.id.reportTitle)
        reportDescription = findViewById(R.id.reportDescription)
        submitButton      = findViewById(R.id.submitButton)

        // GridLayout 자식들을 인덱스로 가져옵니다.
        // XML에 id가 없으므로 GridLayout id를 추가한 뒤 아래처럼 접근합니다.
        val grid = findViewById<GridLayout>(R.id.categoryGrid)  // XML에 id 추가 필요
        categoryLayouts = (0 until grid.childCount).map { grid.getChildAt(it) as LinearLayout }
    }

    // ───────────────────────────────────────────
    // 카테고리 선택
    // ───────────────────────────────────────────

    private fun setupCategoryButtons() {
        categoryLayouts.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                selectedCategory = categories[index]
                updateCategoryUI(index)
            }
        }
        // 초기 선택: 첫 번째(쓰레기) – XML 디자인과 일치
        selectedCategory = categories[0]
        updateCategoryUI(0)
    }

    private fun updateCategoryUI(selectedIndex: Int) {
        categoryLayouts.forEachIndexed { index, layout ->
            if (index == selectedIndex) {
                layout.setBackgroundResource(R.drawable.re_category_selected_bg)
                // 텍스트 색상 흰색으로
                layout.findViewWithTag<TextView>("label")?.setTextColor(
                    ContextCompat.getColor(this, android.R.color.white)
                )
            } else {
                layout.setBackgroundResource(R.drawable.re_category_unselected_bg)
                layout.findViewWithTag<TextView>("label")?.setTextColor(
                    ContextCompat.getColor(this, R.color.text_hint) // #94A3B8
                )
            }
        }
    }

    // ───────────────────────────────────────────
    // 사진 업로드
    // ───────────────────────────────────────────

    private fun setupPhotoUpload() {
        photoFrame.setOnClickListener {
            val permission = Manifest.permission.READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                pickImageLauncher.launch("image/*")
            } else {
                requestCameraPermission.launch(permission)
            }
        }
    }

    private fun handlePhotoSelected(uri: Uri) {
        selectedPhotoUri = uri
        // 선택한 이미지를 FrameLayout 안에 보여줍니다.
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
        }
        photoFrame.removeAllViews()
        photoFrame.addView(imageView)
    }

    // ───────────────────────────────────────────
    // 위치 – 지도 버튼
    // ───────────────────────────────────────────

    private fun setupLocationButton() {
        // XML에서 "지도" TextView에 id(mapButton)를 추가한 뒤 사용하세요.
        val mapButton = findViewById<TextView>(R.id.mapButton)
        mapButton?.setOnClickListener {
            // TODO: MapActivity 또는 지도 Fragment로 이동
            Toast.makeText(this, "지도를 불러옵니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────────────────────────────────────
    // 제출 버튼
    // ───────────────────────────────────────────

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            if (validateInputs()) submitReport()
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedCategory == null) {
            Toast.makeText(this, "카테고리를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (reportTitle.text.isBlank()) {
            reportTitle.error = "제목을 입력해주세요."
            reportTitle.requestFocus()
            return false
        }
        if (reportDescription.text.isBlank()) {
            reportDescription.error = "상세 내용을 입력해주세요."
            reportDescription.requestFocus()
            return false
        }
        return true
    }

    private fun submitReport() {
        val report = ReportData(
            category    = selectedCategory!!,
            title       = reportTitle.text.toString().trim(),
            description = reportDescription.text.toString().trim(),
            photoUri    = selectedPhotoUri,
            location    = "시흥시 정왕동 부근"   // TODO: 실제 GPS 좌표로 교체
        )

        // TODO: ViewModel / Repository 를 통해 서버에 전송
        Toast.makeText(this, "제보가 접수되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ───────────────────────────────────────────
    // 닫기 버튼
    // ───────────────────────────────────────────

    private fun setupCloseButton() {
        closeButton.setOnClickListener { finish() }
    }
}

// ───────────────────────────────────────────────
// 데이터 모델
// ───────────────────────────────────────────────

data class ReportData(
    val category   : String,
    val title      : String,
    val description: String,
    val photoUri   : Uri?,
    val location   : String
)