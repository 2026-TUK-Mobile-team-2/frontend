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
import com.example.fixsiheung.network.RetrofitClient
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
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import java.security.MessageDigest
import com.kakao.vectormap.label.LabelTextBuilder

class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 💡 추가됨: 서버에서 받아온 제보 목록을 저장해둘 리스트 (필터링할 때 원본이 필요함)
    private var allReports: List<Report> = listOf()


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
        initKakaoMap()
        initChipFilter()


        // 해시값 찾는코드
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val myKeyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
                Log.d("KakaoKeyHash", "해시값: $myKeyHash")
            }
        } catch (e: Exception) {
            Log.e("KakaoKeyHash", "해시코드를 가져오는 중 에러 발생", e)
        }


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 코드 추가 필요
    // 원하는 마커만 보기
    private fun initChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_all -> Log.d("Filter", "전체 선택됨")
                R.id.chip_trash -> Log.d("Filter", "쓰레기 선택됨")
                R.id.chip_facilities -> Log.d("Filter", "시설 선택됨")
                R.id.chip_road -> Log.d("Filter", "도로 선택됨")
                R.id.chip_any -> Log.d("Filter", "기타 선택됨")
            }
        }
    }

    // 추가됨: 서버에서 민원 데이터 가져오기
    private fun fetchReportsFromServer() {
        RetrofitClient.apiService.getAllReports().enqueue(object : Callback<List<Report>> {
            override fun onResponse(call: Call<List<Report>>, response: Response<List<Report>>) {
                if (response.isSuccessful) {
                    response.body()?.let { reports ->
                        allReports = reports // 원본 저장
                        drawMarkers(allReports) // 지도에 마커 쫙 뿌리기
                        Log.d("API_TEST", "제보 ${reports.size}개 마커 생성 완료!")
                    }
                }
            }

            override fun onFailure(call: Call<List<Report>>, t: Throwable) {
                Log.e("API_TEST", "서버 통신 실패: ${t.message}")
            }
        })
    }

    // 추가됨: 위도/경도를 바탕으로 카카오맵에 마커(Label) 그리기
    private fun drawMarkers(reports: List<Report>) {
        val labelManager = kakaoMap?.labelManager ?: return

        // 1. 제보용 마커들을 담을 전용 레이어 만들기 (내 위치 마커랑 안 섞이게)
        var reportLayer = labelManager.getLayer("reportLayer")
        if (reportLayer == null) {
            reportLayer = labelManager.addLayer(LabelLayerOptions.from("reportLayer"))
        } else {
            reportLayer.removeAll() // 필터링될 때 기존 마커 싹 지우기
        }

        // 2. 제보 마커 스타일 설정 (임시로 my_marker 사용. 나중에 report_marker로 바꾸세요)
        val styles = labelManager.addLabelStyles(
            LabelStyles.from(
                "reportStyle",
                LabelStyle.from(vectorToBitmap(R.drawable.my_marker)).setAnchorPoint(0.5f, 1.0f)
            )
        )

        // 3. 리스트를 돌면서 지도에 마커 추가
        reports.forEach { report ->
            val pos = LatLng.from(report.latitude, report.longitude)

            val textBuilder = LabelTextBuilder().setTexts(report.title)
            reportLayer?.addLabel(
                LabelOptions.from(report.complaintId.toString(), pos)
                    .setStyles(styles)
                    .setTexts(textBuilder) // 마커 옆에 민원 제목 띄우기
            )
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
        return Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
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
                Log.d("KakaoMap", "onMapDestroy")
            }

            override fun onMapError(error: Exception?) {
                Log.e("KakaoMap", "onMapError: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                this@MainMapActivity.kakaoMap = kakaoMap

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

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        // 지도가 이미 준비되었고 위치 권한이 획득된 상태라면 백그라운드에서 복귀 시 업데이트 재시작
        if (kakaoMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
}