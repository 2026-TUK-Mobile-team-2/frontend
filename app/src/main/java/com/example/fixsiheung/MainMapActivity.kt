package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
    private val binding: ActivityMainMapBinding by lazy {
        ActivityMainMapBinding.inflate(layoutInflater)
    }
    private var kakaoMap: KakaoMap? = null

    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient




    // 위치 권한
    private var locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 권한 허용됨 -> 현재 위치로 이동하는 함수 호출
                startLocationUpdates()
            }

            else -> {
                // 권한 거부됨 -> 종료
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        KakaoMapView()

        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            when (checkedId) {
                R.id.chip_all -> {
                    Log.d("Filter", "전체 선택됨")
                }
                R.id.chip_trash -> {
                    Log.d("Filter", "쓰레기 선택됨")
                }
                R.id.chip_facilities -> {
                    Log.d("Filter", "시설 선택됨")
                }
                R.id.chip_road -> {
                    Log.d("Filter", "도로 선택됨")
                }
                R.id.chip_any -> {
                    Log.d("Filter", "기타 선택됨")
                }
            }
        }







        ViewCompat.setOnApplyWindowInsetsListener((binding.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val myPosition = LatLng.from(location.latitude, location.longitude)

            val labelManager = kakaoMap?.labelManager
            val layer = labelManager?.layer

            if (myLocationLabel == null) {
                val style = LabelStyle.from(R.drawable.my_marker)
                val styles = labelManager?.addLabelStyles(LabelStyles.from(style))

                val options = LabelOptions.from(myPosition).setStyles(styles)
                myLocationLabel = layer?.addLabel(options)

                kakaoMap?.trackingManager?.startTracking(myLocationLabel)
                Log.d("Location", "최초 위치 잡음: ${location.latitude}, ${location.longitude}")
            } else {
                myLocationLabel?.moveTo(myPosition)
            }
        }
    }

    //실시간 추적
    private fun startLocationUpdates() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5초 간격
                .setMinUpdateDistanceMeters(5f) // 5미터 이상 이동 시
                .build()

        // 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 콜백 등록 및 위치 추적 시작
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 앱이 중지되면 배터리 절약을 위해 중단
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun KakaoMapView() {

        //연동 실패시 오류확인
        try {
            KakaoMapSdk.init(this, "f0f61ee911139544fa9381c376c79833")
        } catch (e: Exception) {
            Log.e("KakaoMap", "초기화 실패: ${e.message}")
        }

        //해시값 확인
        /*try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("KeyHash", "현재 앱의 해시값: ${keyHash.trim()}")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "해시값을 가져올 수 없습니다.", e)
        }*/
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("KakaoMap", "onMapDestroy")
            }

            override fun onMapError(error: Exception?) {
                Log.e("KakaoMap", "onMapError")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                this@MainMapActivity.kakaoMap = kakaoMap
                // 학교에서 시작
                val startLatLng = LatLng.from(37.340174, 126.733593)
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(startLatLng))

                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            // 내 위치 버튼 보이기
            }
        }
        )
    }
}