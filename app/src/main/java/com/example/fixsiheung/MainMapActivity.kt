package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fixsiheung.databinding.ActivityMainMapBinding
import com.example.fixsiheung.model.Report
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.security.MessageDigest

class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val registeredLabels = mutableListOf<Label>() // 지도에 표시된 마커 보관 리스트
    private var allMarkerItems = listOf<Report>()
    private val cachedStyles = mutableMapOf<String, LabelStyles>()

    // 임시 입력값
    private fun loadMockData() {
        allMarkerItems = listOf(
            Report(1, "쓰레기 무단투기", "내용", "trash", 37.341500, 126.732500, "닉네임"),
            Report(2, "파손된 벤치 수리 필요", "내용", "facilities", 37.33950, 126.735000, "닉네임"),
            Report(3, "아스팔트 포트홀 위험", "내용", "road", 37.342000, 126.734000, "닉네임"),
            Report(4, "기타 불편 사항", "내용", "any", 37.340000, 126.736500, "닉네임"),
            )
    }



    // 위치 권환
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        loadMockData()
        initKakaoMap()
        initChipFilter()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 마커표시
    private fun displayRegisteredMarkers(items: List<Report>) {
        val labelManager = kakaoMap?.labelManager ?: return
        val layer = labelManager.layer ?: labelManager.getLayer() ?: return

        registeredLabels.forEach { it.remove() }
        registeredLabels.clear()

        for (report in items) {
            val reportPosition = LatLng.from(report.latitude, report.longitude)

            val styles = cachedStyles[report.category] ?: cachedStyles["any"] ?: continue

            val label = layer.addLabel(
                LabelOptions.from(reportPosition)
                    .setStyles(styles)
                    .setTag(report)
                    .setRank(5)
            )

            if (label != null) {
                registeredLabels.add(label)
            }
        }
    }

    private fun initChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chip_trash -> "trash"
                R.id.chip_facilities -> "facilities"
                R.id.chip_road -> "road"
                R.id.chip_any -> "any"
                else -> "all" // chip_all 이거나 선택 안 된 경우
            }

            if (selectedCategory == "all") {
                // 전체 보기인 경우 원본 리스트 통째로 넘김
                displayRegisteredMarkers(allMarkerItems)
            } else {
                // 특정 카테고리만 필터링해서 넘김
                val filteredList = allMarkerItems.filter { it.category == selectedCategory }
                displayRegisteredMarkers(filteredList)
            }
        }
    }

    // 현재위치 표시
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val myPosition = LatLng.from(location.latitude, location.longitude)

            val labelManager = kakaoMap?.labelManager ?: return
            val layer = labelManager.layer ?: labelManager.getLayer() ?: return

            if (myLocationLabel == null) {
                val styles = labelManager.addLabelStyles(
                    LabelStyles.from(
                        "myStyle",
                        LabelStyle.from(vectorToBitmap(R.drawable.my_marker))
                            .setAnchorPoint(0.5f, 0.5f)
                    )
                )
                myLocationLabel = layer.addLabel(
                    LabelOptions.from(myPosition).setStyles(styles).setRank(10)
                )
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPosition))
            } else {
                myLocationLabel?.moveTo(myPosition)
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPosition))
            }
        }
    }

    // 위치 추적 시작
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // 내 위치 마커 (벡터 -> 비트맵)
    private fun vectorToBitmap(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!

        // 원래 이미지 크기 로그 찍어보기 (여기서 범인을 찾을 수 있습니다)
        Log.d("MapBitmapCheck", "ID: $drawableId 의 원래 크기: ${drawable.intrinsicWidth} x ${drawable.intrinsicHeight}")

        // ★ 안전장치: 마커로 사용할 최대 크기를 기기 해상도 기준 48dp로 제한합니다.
        val maxDp = 48
        val maxPx = (maxDp * resources.displayMetrics.density).toInt()

        var width = drawable.intrinsicWidth
        var height = drawable.intrinsicHeight

        // 만약 이미지가 기준치보다 크다면, 비율을 유지하면서 크기를 줄입니다.
        if (width > maxPx || height > maxPx) {
            if (width > height) {
                height = (height * (maxPx.toFloat() / width)).toInt()
                width = maxPx
            } else {
                width = (width * (maxPx.toFloat() / height)).toInt()
                height = maxPx
            }
        }

        // 안전하게 축소된 크기로 비트맵을 생성합니다.
        return Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(Canvas(it))
        }
    }

    private fun initKakaoMap() {
        KakaoMapSdk.init(this, "f0f61ee911139544fa9381c376c79833")

        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                kakaoMap = null
                myLocationLabel = null
                registeredLabels.clear()
            }

            override fun onMapError(error: Exception?) {
                Log.e("KakaoMap", "onMapError: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                this@MainMapActivity.kakaoMap = kakaoMap
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(37.340174, 126.733593)))

                initMarkerStyles()
                // ★ 2. 지도가 준비되었으니 초기 마커(전체)를 화면에 쫙 뿌려줍니다.
                displayRegisteredMarkers(allMarkerItems)

                // 기본값 : 학교 위치
                kakaoMap.moveCamera(
                    CameraUpdateFactory.newCenterPosition(
                        LatLng.from(
                            37.340174,
                            126.733593
                        )
                    )
                )
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        })
    }

    private fun initMarkerStyles() {
        val labelManager = kakaoMap?.labelManager ?: return

        // 카테고리별로 사용할 드로어블 매핑
        val categories = mapOf(
            "trash" to R.drawable.ic_marker_trash,
            "facilities" to R.drawable.ic_marker_facilities,
            "road" to R.drawable.ic_marker_road,
            "any" to R.drawable.ic_marker_any
        )

        // 최초 1회만 카카오 지도 엔진에 스타일을 등록하고 캐싱합니다.
        categories.forEach { (category, drawableId) ->
            val labelStyle = LabelStyle.from(vectorToBitmap(drawableId)).setAnchorPoint(0.5f, 1.0f)
            val styles = labelManager.addLabelStyles(LabelStyles.from("style_$category", labelStyle))

            if (styles != null) {
                cachedStyles[category] = styles
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        // 지도가 이미 준비되었고 위치 권한이 획득된 상태라면 백그라운드에서 복귀 시 업데이트 재시작
        if (kakaoMap != null && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }
}