package com.example.fixsiheung

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fixsiheung.databinding.ActivityMainBinding
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
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var kakaoMap: KakaoMap? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    // 위치 권한 함수
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 권한 허용됨 -> 현재 위치로 이동하는 함수 호출
                startLocationUpdates()
            }

            else -> {
                // 권한 거부됨
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 업데이트 시 실행할 행동 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentLatLng = LatLng.from(location.latitude, location.longitude)
                    Log.d("RealTimeLocation", "업데이트: ${location.latitude}, ${location.longitude}")

                    // 지도가 준비되었을 때만 이동
                    kakaoMap?.moveCamera(
                        CameraUpdateFactory.newCenterPosition(currentLatLng),
                        CameraAnimation.from(500)
                    )
                }
            }
        }
        KakaoMapView()

        ViewCompat.setOnApplyWindowInsetsListener((binding.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    //실시간 추적
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5초 간격
            .setMinUpdateDistanceMeters(5f) // 5미터 이상 이동 시 업데이트
            .build()

        //권환 확인후 실행
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    // 앱이 중지되면 배터리 절약을 위해 중단
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun KakaoMapView() {

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
                this@MainActivity.kakaoMap = kakaoMap
                //학교
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