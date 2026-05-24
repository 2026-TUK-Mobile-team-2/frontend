package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val registeredLabels = mutableListOf<Label>()
    private var allMarkerItems = listOf<Report>()
    private val cachedStyles = mutableMapOf<String, LabelStyles>()

    private val schoolLatLng = LatLng.from(37.340174, 126.733593)

    private val categoryDrawables = mapOf(
        1 to R.drawable.ic_marker_trash,
        2 to R.drawable.ic_marker_facilities,
        3 to R.drawable.ic_marker_road,
        4 to R.drawable.ic_marker_any
    )

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
        initChipStyles()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (kakaoMap != null && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ─── 임시 데이터 ───────────────────────────────────────────────────────────

    private fun loadMockData() {
        val now     = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val oneDay  = 86_400_000L

        allMarkerItems = listOf(
            Report(1, "a", 1, "쓰레기 무단투기",  "정왕역 앞 골목길에 쓰레기 무단투기가 너무 심합니다.",       "접수",  37.341500, 126.732500, "경기도 시흥시 정왕동 2321",        15, now - oneHour,       now - oneHour),
            Report(2, "b", 2, "파손된 벤치 수리", "놀이터 옆 벤치 나무가 부서져 아이들이 다칠 위험이 있습니다.", "접수", 37.339500, 126.735000, "경기도 시흥시 정왕동 1700 공원내",  3,  now - oneDay * 2,    now - oneDay),
            Report(3, "c", 3, "아스팔트 포트홀",  "서해안로 2차선 도로에 깊은 포트홀이 생겼습니다.",           "접수",  37.342000, 126.734000, "경기도 시흥시 정왕동 1284-4 도로", 24, now - oneDay * 3,    now - oneDay * 3),
            Report(4, "d", 4, "가로등 소등 신고", "골목 가로등이 완전히 꺼졌습니다. 밤길이 너무 어둡습니다.",   "접수",  37.340000, 126.736500, "경기도 시흥시 정왕동 1502-1",      0,  now - oneHour / 2,   now - oneHour / 2),
        )
    }

    // ─── 지도 초기화 ───────────────────────────────────────────────────────────

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
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(schoolLatLng))
                initMarkerStyles()
                displayRegisteredMarkers(allMarkerItems)
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        })
    }

    // ─── 마커 ──────────────────────────────────────────────────────────────────

    private fun initMarkerStyles() {
        val labelManager = kakaoMap?.labelManager ?: return

        categoryDrawables.forEach { (id, drawableId) ->
            val base = vectorToBitmap(drawableId)
            mapOf(
                "$id"          to createBadgeMarkerBitmap(base, isHot = false, isNew = false),
                "${id}_hot"    to createBadgeMarkerBitmap(base, isHot = true,  isNew = false),
                "${id}_new"    to createBadgeMarkerBitmap(base, isHot = false, isNew = true),
                "${id}_hot_new" to createBadgeMarkerBitmap(base, isHot = true, isNew = true),
            ).forEach { (key, bitmap) ->
                val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)
                labelManager.addLabelStyles(LabelStyles.from("style_$key", style))
                    ?.let { cachedStyles[key] = it }
            }
        }
    }

    private fun displayRegisteredMarkers(items: List<Report>) {
        val layer = kakaoMap?.labelManager?.layer ?: return
        registeredLabels.forEach { it.remove() }
        registeredLabels.clear()

        val now            = System.currentTimeMillis()
        val twentyFourHours = 86_400_000L

        items.forEach { report ->
            val isHot = report.empathyCount >= 10
            val isNew = (now - report.createdAt) <= twentyFourHours

            val styleKey = when {
                isHot && isNew -> "${report.categoryId}_hot_new"
                isHot          -> "${report.categoryId}_hot"
                isNew          -> "${report.categoryId}_new"
                else           -> report.categoryId.toString()
            }

            val styles = cachedStyles[styleKey] ?: cachedStyles["4"] ?: return@forEach
            layer.addLabel(
                LabelOptions.from(LatLng.from(report.latitude, report.longitude))
                    .setStyles(styles)
                    .setTag(report)
                    .setRank(5)
            )?.let { registeredLabels.add(it) }
        }
    }

    private fun createBadgeMarkerBitmap(baseBitmap: Bitmap, isHot: Boolean, isNew: Boolean): Bitmap {
        if (!isHot && !isNew) return baseBitmap

        val dp = resources.displayMetrics.density
        val badgeH   = (12 * dp).toInt()
        val badgeW   = (24 * dp).toInt()
        val gap      = (2  * dp).toInt()
        val overlap  = (-15 * dp).toInt()

        val badgeCount  = listOf(isHot, isNew).count { it }
        val totalBadgeH = badgeH * badgeCount + gap * (badgeCount - 1)

        val resultW = maxOf(baseBitmap.width, badgeW)
        val resultH = baseBitmap.height + totalBadgeH + overlap
        val result  = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888)
        val canvas  = Canvas(result)

        canvas.drawBitmap(baseBitmap, (resultW - baseBitmap.width) / 2f, (totalBadgeH + overlap).toFloat(), null)

        val badgePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val textPaint  = Paint().apply {
            color          = Color.WHITE
            textSize       = 8 * dp
            isAntiAlias    = true
            textAlign      = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val badgeLeft  = (resultW - badgeW) / 2f
        val badges = buildList {
            if (isHot) add("#FF3B30" to "HOT")
            if (isNew) add("#3B82F6" to "NEW")
        }

        badges.forEachIndexed { i, (colorHex, label) ->
            val top   = i * (badgeH + gap)
            val rectF = RectF(badgeLeft, top.toFloat(), badgeLeft + badgeW, (top + badgeH).toFloat())
            badgePaint.color = Color.parseColor(colorHex)
            canvas.drawRoundRect(rectF, badgeH / 2f, badgeH / 2f, badgePaint)
            val textY = top + badgeH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, resultW / 2f, textY, textPaint)
        }

        return result
    }

    // ─── 현재 위치 ─────────────────────────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location   = locationResult.lastLocation ?: return
            val myPosition = LatLng.from(location.latitude, location.longitude)
            val layer      = kakaoMap?.labelManager?.layer ?: return

            if (myLocationLabel == null) {
                val labelManager = kakaoMap?.labelManager ?: return
                val styles = labelManager.addLabelStyles(
                    LabelStyles.from("myStyle",
                        LabelStyle.from(vectorToBitmap(R.drawable.my_marker)).setAnchorPoint(0.5f, 0.5f)
                    )
                )
                myLocationLabel = layer.addLabel(LabelOptions.from(myPosition).setStyles(styles).setRank(10))
            } else {
                myLocationLabel?.moveTo(myPosition)
            }
            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPosition))
        }
    }

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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // ─── 칩 필터 ───────────────────────────────────────────────────────────────

    private fun initChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val categoryId: Int? = when (checkedIds.firstOrNull()) {
                R.id.chip_trash      -> 1
                R.id.chip_facilities -> 2
                R.id.chip_road       -> 3
                R.id.chip_any        -> 4
                else                 -> null
            }
            val filtered = if (categoryId == null) allMarkerItems
            else allMarkerItems.filter { it.categoryId == categoryId }
            displayRegisteredMarkers(filtered)
        }
    }

    private fun initChipStyles() {
        val states   = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val bgList   = ColorStateList(states, intArrayOf(ContextCompat.getColor(this, R.color.primary), Color.parseColor("#F3F4F6")))
        val textList = ColorStateList(states, intArrayOf(Color.WHITE, Color.parseColor("#4B5563")))

        listOf(binding.chipAll, binding.chipTrash, binding.chipFacilities, binding.chipRoad, binding.chipAny)
            .forEach { chip ->
                chip.chipBackgroundColor = bgList
                chip.setTextColor(textList)
                chip.isChipIconVisible = false
            }
    }

    // ─── 유틸 ──────────────────────────────────────────────────────────────────

    private fun vectorToBitmap(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!
        val maxPx    = (48 * resources.displayMetrics.density).toInt()

        var w = drawable.intrinsicWidth
        var h = drawable.intrinsicHeight

        if (w > maxPx || h > maxPx) {
            if (w > h) { h = (h * (maxPx.toFloat() / w)).toInt(); w = maxPx }
            else       { w = (w * (maxPx.toFloat() / h)).toInt(); h = maxPx }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(Canvas(it))
        }
    }
}