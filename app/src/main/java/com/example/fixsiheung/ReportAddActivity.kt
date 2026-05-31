package com.example.fixsiheung

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.example.fixsiheung.databinding.ActivityReportAddBinding
import com.example.fixsiheung.network.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

class ReportAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportAddBinding
    private var selectedImageUri: Uri? = null
    private var selectedCategoryId: Int = 1
    private var currentAddress: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 37.340174  // 기본값: 학교 위치
    private var currentLongitude: Double = 126.733593

    private val categoryViews by lazy {
        listOf(
            binding.categoryTrash,    // 1: 쓰레기
            binding.categoryDamaged,  // 2: 시설파손
            binding.categoryDanger,   // 3: 안전위험
            binding.categoryParking,  // 4: 불법주차
            binding.categoryNoise,    // 5: 소음
            binding.categoryOther     // 6: 기타
        )
    }

    // 갤러리 이미지 선택 런처
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).centerCrop().into(binding.ivPreview)
            binding.ivPreview.visibility = android.view.View.VISIBLE
            binding.layoutPhotoGuide.visibility = android.view.View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 카테고리 기본 선택
        updateCategoryUI(1)

        // 버튼 초기 비활성화
        binding.submitButton.isEnabled = false
        binding.submitButton.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor("#D1D5DB"))

        // 닫기 버튼
        binding.closeButton.setOnClickListener {
            startActivity(Intent(this@ReportAddActivity, MainMapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        // 사진 첨부 클릭
        binding.photoFrame.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // 현재 위치 불러오기
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        // 카테고리 클릭 리스너
        binding.categoryTrash.setOnClickListener { selectCategory(1) }
        binding.categoryDamaged.setOnClickListener { selectCategory(2) }
        binding.categoryDanger.setOnClickListener { selectCategory(3) }
        binding.categoryParking.setOnClickListener { selectCategory(4) }
        binding.categoryNoise.setOnClickListener { selectCategory(5) }
        binding.categoryOther.setOnClickListener { selectCategory(6) }

        // 입력 감지 → 버튼 활성화
        binding.reportTitle.addTextChangedListener { checkInputs() }
        binding.reportDescription.addTextChangedListener { checkInputs() }

        // 제보하기 버튼
        binding.submitButton.setOnClickListener {
            val title = binding.reportTitle.text.toString().trim()
            val content = binding.reportDescription.text.toString().trim()

            val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("user_id", "") ?: ""

            if (userId.isEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 제출 중 버튼 비활성화
            binding.submitButton.isEnabled = false
            binding.submitButton.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            binding.submitButton.text = "제보 중..."

            val request = com.example.fixsiheung.model.ComplaintRequest(
                userId = userId,
                categoryId = selectedCategoryId,
                title = title,
                description = content,
                latitude = currentLatitude,
                longitude = currentLongitude,
                address = currentAddress
            )

            RetrofitClient.apiService.postReport(request)
                .enqueue(object :
                    retrofit2.Callback<com.example.fixsiheung.model.ComplaintResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.fixsiheung.model.ComplaintResponse>,
                        response: retrofit2.Response<com.example.fixsiheung.model.ComplaintResponse>
                    ) {
                        if (response.isSuccessful) {
                            // 이미지 선택했으면 업로드
                            val complaintId = response.body()?.complaintId ?: -1
                            if (complaintId != -1 && selectedImageUri != null) {
                                uploadImage(complaintId)
                            }
                            runOnUiThread {
                                Toast.makeText(
                                    this@ReportAddActivity,
                                    "제보가 등록되었습니다!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(
                                    Intent(
                                        this@ReportAddActivity,
                                        MainMapActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    })
                                finish()
                            }
                        } else {
                            runOnUiThread {
                                binding.submitButton.isEnabled = true
                                binding.submitButton.text = "제보하기"
                                binding.submitButton.backgroundTintList =
                                    ColorStateList.valueOf(Color.parseColor("#65C0C1"))
                                Toast.makeText(
                                    this@ReportAddActivity,
                                    "제보 등록 실패",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<com.example.fixsiheung.model.ComplaintResponse>,
                        t: Throwable
                    ) {
                        runOnUiThread {
                            binding.submitButton.isEnabled = true
                            binding.submitButton.text = "제보하기"
                            binding.submitButton.backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor("#65C0C1"))
                            Toast.makeText(this@ReportAddActivity, "서버 통신 실패", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                })
        }
    }

    // 제목, 내용 모두 입력 시 버튼 활성화
    private fun checkInputs() {
        val isFilled = binding.reportTitle.text.toString().trim().isNotEmpty() &&
                binding.reportDescription.text.toString().trim().isNotEmpty()

        binding.submitButton.isEnabled = isFilled
        binding.submitButton.backgroundTintList = if (isFilled) {
            ColorStateList.valueOf(Color.parseColor("#65C0C1"))
        } else {
            ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
        }
    }

    private fun selectCategory(id: Int) {
        selectedCategoryId = id
        updateCategoryUI(id)
    }

    private fun updateCategoryUI(selectedIndex: Int) {
        categoryViews.forEachIndexed { index, view ->
            view.setBackgroundResource(
                if (index == selectedIndex - 1) R.drawable.re_category_selected_bg
                else R.drawable.re_category_unselected_bg
            )
        }
    }

    // 현재 위치 GPS 불러오기
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude

                val geocoder = Geocoder(this, Locale.KOREAN)
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    currentAddress = addresses[0].getAddressLine(0)
                        .replace("대한민국 ", "")
                    binding.tvCurrentLocation.text = currentAddress
                }
            }
        }
    }

    // 이미지 업로드
    private fun uploadImage(complaintId: Int) {
        val uri = selectedImageUri ?: return
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val file = File(cacheDir, "upload_image.jpg")
        file.outputStream().use { inputStream.copyTo(it) }

        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

        RetrofitClient.apiService.uploadComplaintImage(complaintId, part)
            .enqueue(object : retrofit2.Callback<Map<String, Any>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) Log.d("Image", "이미지 업로드 성공")
                    else Log.e("Image", "이미지 업로드 실패: ${response.code()}")
                }

                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                    Log.e("Image", "이미지 업로드 실패: ${t.message}")
                }
            })
    }


}