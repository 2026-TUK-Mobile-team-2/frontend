package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
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

class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient


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

    // 현재위치 표시
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val myPosition = LatLng.from(location.latitude, location.longitude)

            val labelManager = kakaoMap?.labelManager ?: return
            val layer = labelManager.layer ?: return

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
}