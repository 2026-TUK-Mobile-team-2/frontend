package com.example.fixsiheung

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.databinding.ActivityMainMapBinding
import com.example.fixsiheung.model.Report
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

/*
 * 지도 메인 화면
 * - 카카오맵 표시 및 민원 마커 렌더링
 * - 현재 위치 실시간 추적
 * - 검색, 카테고리 필터, 주변 소식 바텀시트 제공
 */
class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var nearReportAdapter: NearReportAdapter

    private val registeredLabels = mutableListOf<Label>()         // 지도에 표시된 민원 마커 목록
    private var allMarkerItems = listOf<Report>()                  // 전체 민원 데이터
    private val cachedStyles = mutableMapOf<String, LabelStyles>() // 카테고리별 마커 스타일 캐시

    // 앱 시작 시 카메라가 이동할 기본 위치 (한국공학대학교)
    private val schoolLatLng = LatLng.from(37.340174, 126.733593)

    // 바텀시트 최대 확장 시 상단 여백 (검색바 + 칩 영역 높이)
    private val EXPANDED_OFFSET_DP = 130

    // 카테고리 ID → 마커 아이콘 drawable 매핑 (1:쓰레기, 2:시설, 3:도로, 4:기타)
    private val categoryDrawables = mapOf(
        1 to R.drawable.ic_marker_trash,
        2 to R.drawable.ic_marker_facilities,
        3 to R.drawable.ic_marker_road,
        4 to R.drawable.ic_marker_any
    )

    // 위치 권한 요청 런처 (허용 시 위치 추적 시작, 거부 시 앱 종료)
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
        loadMockData()      // 임시 데이터 로드 (추후 API 연동으로 교체)
        initKakaoMap()      // 카카오맵 초기화
        initChipFilter()    // 카테고리 칩 필터 초기화
        initChipStyles()    // 칩 스타일 (색상 등) 설정
        initSearchBar()     // 검색바 초기화
        initNearReports()   // 바텀시트 (내 주변 소식) 초기화

        // 시스템 바(상태바, 네비게이션 바) 높이만큼 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // 백그라운드에서 복귀 시 지도가 준비되어 있고 권한이 있으면 위치 추적 재시작
        if (kakaoMap != null && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        // 배터리 절약을 위해 백그라운드 진입 시 위치 업데이트 중단
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ─── 임시 데이터 ───────────────────────────────────────────────────────────
    // TODO: 백엔드 API 연동 후 이 함수를 실제 API 호출로 교체

    private fun loadMockData() {
        val now     = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val oneDay  = 86_400_000L

        allMarkerItems = listOf(
            Report(1,  "a", 1, "쓰레기 무단투기",      "정왕역 앞 골목길에 쓰레기 무단투기가 너무 심합니다.",              "접수완료", 37.341500, 126.732500, "경기도 시흥시 정왕동 2321",        15, now - oneHour,     now - oneHour),
            Report(2,  "b", 2, "파손된 벤치 수리",      "놀이터 옆 벤치 나무가 부서져 아이들이 다칠 위험이 있습니다.",       "처리중",   37.339500, 126.735000, "경기도 시흥시 정왕동 1700 공원내",  3,  now - oneDay * 2,  now - oneDay),
            Report(3,  "c", 3, "아스팔트 포트홀",       "서해안로 2차선 도로에 깊은 포트홀이 생겼습니다.",                  "접수완료", 37.342000, 126.734000, "경기도 시흥시 정왕동 1284-4 도로", 24, now - oneDay * 3,  now - oneDay * 3),
            Report(4,  "d", 4, "가로등 소등 신고",      "골목 가로등이 완전히 꺼졌습니다. 밤길이 너무 어둡습니다.",          "처리완료", 37.340000, 126.736500, "경기도 시흥시 정왕동 1502-1",      0,  now - oneHour / 2, now - oneHour / 2),
            Report(5,  "b", 2, "가로등 고장 및 파손",   "배곧생명공원 산책로 중간에 가로등이 꺼져있어 밤에 위험합니다.",     "접수완료", 37.369200, 126.721400, "경기도 시흥시 배곧동 190",          8,  now - oneHour,     now - oneHour),
            Report(6,  "c", 3, "보도블럭 파손 및 침하", "시흥시청역 2번 출구 앞 보도블럭이 심하게 깨져있어 불편합니다.",     "처리중",   37.380200, 126.802800, "경기도 시흥시 장현동 300",         23, now - oneHour,     now - oneHour),
            Report(7,  "d", 4, "불법 노점 및 적치물",   "오이도 빨강등대 선착장 입구 쪽에 불법 적치물이 쌓여있습니다.",      "접수완료", 37.348600, 126.669800, "경기도 시흥시 정왕동 2063",         5,  now - oneDay,      now - oneDay),
            Report(8,  "e", 1, "원룸가 음식물 쓰레기",  "대학가 뒤편 원룸 밀집 구역에 음식물 쓰레기 수거함이 넘칩니다.",     "처리완료", 37.340100, 126.733200, "경기도 시흥시 정왕동 1824",        42, now - oneDay,      now - oneDay),
            Report(9,  "f", 3, "아스팔트 도로 파손",    "연구동 뒤편 이면도로 아스팔트가 파여 차량이 지나갈 때 위험합니다.", "접수완료", 37.340650, 126.732800, "경기도 시흥시 정왕동 1824-2",       3,  now - oneHour,     now - oneHour),
            Report(10, "g", 2, "어린이공원 벤치 파손",  "작은 공원 의자 나무 상판이 부서져 앉다가 다칠 것 같습니다.",        "처리중",   37.339200, 126.734200, "경기도 시흥시 정왕동 1828",        11, now - oneHour,     now - oneHour),
        )
    }

    // ─── 지도 초기화 ───────────────────────────────────────────────────────────

    private fun initKakaoMap() {
        KakaoMapSdk.init(this, "f0f61ee911139544fa9381c376c79833")

        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // 지도가 파괴될 때 참조 초기화 (메모리 누수 방지)
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
                initMarkerStyles()                       // 마커 스타일 미리 캐싱
                displayRegisteredMarkers(allMarkerItems) // 전체 민원 마커 표시
                locationPermissionRequest.launch(        // 위치 권한 요청
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        })
    }

    // ─── 마커 ──────────────────────────────────────────────────────────────────

    /*
     * 카테고리별 마커 스타일을 미리 생성하여 캐시에 저장
     * 마커는 일반 / HOT / NEW / HOT+NEW 4가지 상태
     * HOT: 공감수 상위 2개 / NEW: 등록된 지 24시간 이내
     */
    private fun initMarkerStyles() {
        val labelManager = kakaoMap?.labelManager ?: return

        categoryDrawables.forEach { (id, drawableId) ->
            val base = vectorToBitmap(drawableId)
            mapOf(
                "$id"           to createBadgeMarkerBitmap(base, isHot = false, isNew = false),
                "${id}_hot"     to createBadgeMarkerBitmap(base, isHot = true,  isNew = false),
                "${id}_new"     to createBadgeMarkerBitmap(base, isHot = false, isNew = true),
                "${id}_hot_new" to createBadgeMarkerBitmap(base, isHot = true,  isNew = true),
            ).forEach { (key, bitmap) ->
                val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)
                labelManager.addLabelStyles(LabelStyles.from("style_$key", style))
                    ?.let { cachedStyles[key] = it }
            }
        }
    }

    /*
     * 민원 리스트를 받아 지도에 마커를 표시
     * 기존 마커를 모두 제거한 후 새로 그림
     * 칩 필터나 검색 결과 변경 시 이 함수를 호출
     */
    private fun displayRegisteredMarkers(items: List<Report>) {
        val layer = kakaoMap?.labelManager?.layer ?: return
        registeredLabels.forEach { it.remove() }
        registeredLabels.clear()

        val now         = System.currentTimeMillis()
        val twentyFourH = 86_400_000L
        // 공감수 기준 상위 2개 민원 ID → HOT 배지 표시 대상
        val topHotIds   = allMarkerItems.sortedByDescending { it.empathyCount }.take(2).map { it.complaintId }

        items.forEach { report ->
            val isHot = report.complaintId in topHotIds
            val isNew = (now - report.createdAt) <= twentyFourH

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
                    .setTag(report)   // 마커 클릭 시 Report 데이터 접근용
                    .setRank(5)
            )?.let { registeredLabels.add(it) }
        }
    }

    /*
     * 기본 마커 비트맵 위에 HOT/NEW 배지를 합성하여 반환
     * isHot, isNew 모두 false이면 원본 비트맵을 그대로 반환
     */
    private fun createBadgeMarkerBitmap(baseBitmap: Bitmap, isHot: Boolean, isNew: Boolean): Bitmap {
        if (!isHot && !isNew) return baseBitmap

        val dp          = resources.displayMetrics.density
        val badgeH      = (12 * dp).toInt()
        val badgeW      = (24 * dp).toInt()
        val gap         = (2  * dp).toInt()
        val overlap     = (-15 * dp).toInt() // 배지와 마커 아이콘 사이 겹치는 정도 (음수 = 겹침)
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

        val badgeLeft = (resultW - badgeW) / 2f
        buildList {
            if (isHot) add("#FF3B30" to "HOT") // 빨간색
            if (isNew) add("#3B82F6" to "NEW") // 파란색
        }.forEachIndexed { i, (colorHex, label) ->
            val top   = i * (badgeH + gap)
            val rectF = RectF(badgeLeft, top.toFloat(), badgeLeft + badgeW, (top + badgeH).toFloat())
            badgePaint.color = Color.parseColor(colorHex)
            canvas.drawRoundRect(rectF, badgeH / 2f, badgeH / 2f, badgePaint)
            val textY = top + badgeH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, resultW / 2f, textY, textPaint)
        }

        return result
    }

    // ─── 검색 ──────────────────────────────────────────────────────────────────

    /*
     * 검색바 초기화
     * - 실시간 입력 감지 → 제목 기준 필터링 후 드롭다운 표시
     * - 결과 클릭 시 해당 민원 위치로 카메라 이동
     * - X 버튼 클릭 시 검색어 초기화
     */
    private fun initSearchBar() {
        searchAdapter = SearchResultAdapter(emptyList()) { report ->
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(report.latitude, report.longitude), 16),
                CameraAnimation.from(500, true, true)
            )
            binding.chipAll.isChecked = true  // 칩 필터를 '전체'로 초기화
            displayRegisteredMarkers(allMarkerItems)
            binding.searchBar.setText(report.title)
            binding.rvSearchResults.visibility = View.GONE
            hideKeyboard()
            binding.searchBar.clearFocus()
        }
        binding.rvSearchResults.adapter = searchAdapter

        binding.searchBar.addTextChangedListener { editable ->
            val keyword = editable?.toString()?.trim() ?: ""
            binding.icClearText.visibility = if (keyword.isNotEmpty()) View.VISIBLE else View.GONE

            if (keyword.isEmpty()) {
                binding.rvSearchResults.visibility = View.GONE
                displayRegisteredMarkers(allMarkerItems)
            } else {
                // 제목에 키워드가 포함된 민원만 필터링, 키워드 등장 위치 순으로 정렬
                val filtered = allMarkerItems
                    .filter { it.title.contains(keyword, ignoreCase = true) }
                    .sortedBy { it.title.indexOf(keyword, ignoreCase = true) }
                binding.rvSearchResults.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
                if (filtered.isNotEmpty()) searchAdapter.updateList(filtered)
            }
        }

        binding.icClearText.setOnClickListener {
            binding.searchBar.text?.clear()
            hideKeyboard()
            binding.searchBar.clearFocus()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    // ─── 바텀시트 (내 주변 소식) ────────────────────────────────────────────────

    /*
     * 바텀시트 초기화
     * - 접힌 상태: 가로 스크롤 카드 리스트
     * - 펼친 상태: 세로 리스트로 전환
     * - 카드 클릭 시 해당 민원 위치로 카메라 이동
     */
    private fun initNearReports() {
        binding.root.post {
            val dp = resources.displayMetrics.density

            // 기기 제스처 바 높이를 고려하여 네비게이션 바 높이 동적 설정
            val navBarHeight = resources.getDimensionPixelSize(
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            )
            val navHeight = (80 * dp).toInt() + navBarHeight
            binding.bottomNavigation.layoutParams.height = navHeight
            binding.bottomNavigation.requestLayout()

            // 바텀시트 최대 확장 시 검색바+칩 영역 아래까지만 올라오도록 설정
            BottomSheetBehavior.from(binding.layoutBottomNews).apply {
                isFitToContents = false
                expandedOffset = (EXPANDED_OFFSET_DP * dp).toInt()
            }
        }

        nearReportAdapter = NearReportAdapter(allMarkerItems) { report ->
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(report.latitude, report.longitude), 16),
                CameraAnimation.from(500, true, true)
            )
            binding.chipAll.isChecked = true
            displayRegisteredMarkers(allMarkerItems)
            BottomSheetBehavior.from(binding.layoutBottomNews).state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.rvNearReports.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvNearReports.adapter = nearReportAdapter

        // 가로 스크롤 인디케이터 위치 연동
        binding.rvNearReports.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val maxScroll = recyclerView.computeHorizontalScrollRange() - recyclerView.computeHorizontalScrollExtent()
                if (maxScroll <= 0) return
                val ratio    = recyclerView.computeHorizontalScrollOffset().toFloat() / maxScroll
                val maxMoveX = (binding.layoutIndicator.width - binding.viewIndicatorBar.width).toFloat()
                binding.viewIndicatorBar.translationX = ratio * maxMoveX
            }
        })

        // 바텀시트 상태 변화 감지
        BottomSheetBehavior.from(binding.layoutBottomNews)
            .addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (nearReportAdapter.itemCount == 0) {
                        binding.rvNearReports.visibility   = View.GONE
                        binding.layoutIndicator.visibility = View.GONE
                        binding.tvNoReports.visibility     = View.VISIBLE
                        return
                    }

                    val dp     = resources.displayMetrics.density
                    val params = binding.layoutListContainer.layoutParams as LinearLayout.LayoutParams

                    when (newState) {
                        // 펼쳤을 때: 세로 리스트로 전환
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.rvNearReports.layoutManager = LinearLayoutManager(this@MainMapActivity, LinearLayoutManager.VERTICAL, false)
                            binding.rvNearReports.recycledViewPool.clear()
                            nearReportAdapter.isVertical = true
                            binding.rvNearReports.setPadding((20 * dp).toInt(), 0, (20 * dp).toInt(), ((EXPANDED_OFFSET_DP + 24) * dp).toInt())
                            params.height = 0
                            params.weight = 1f
                            binding.layoutListContainer.layoutParams = params
                            nearReportAdapter.notifyDataSetChanged()
                            binding.layoutIndicator.visibility = View.GONE
                            binding.tvNoReports.visibility     = View.GONE
                        }
                        // 접혔을 때: 가로 리스트로 전환
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.rvNearReports.layoutManager = LinearLayoutManager(this@MainMapActivity, LinearLayoutManager.HORIZONTAL, false)
                            binding.rvNearReports.recycledViewPool.clear()
                            nearReportAdapter.isVertical = false
                            binding.rvNearReports.setPadding((20 * dp).toInt(), 0, (20 * dp).toInt(), 0)
                            params.height = LinearLayout.LayoutParams.WRAP_CONTENT
                            params.weight = 0f
                            binding.layoutListContainer.layoutParams = params
                            nearReportAdapter.notifyDataSetChanged()
                            binding.layoutIndicator.visibility = View.VISIBLE
                            binding.tvNoReports.visibility     = View.GONE
                        }
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
    }

    // ─── 현재 위치 ─────────────────────────────────────────────────────────────

    /*
     * 위치 업데이트 콜백
     * - 내 위치 마커를 생성하거나 이동
     * - 현재 위치 기준 1km 이내 민원을 바텀시트에 표시
     */
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
            updateNearReports(location)
        }
    }

    /*
     * 현재 위치 기준 1km 이내 민원을 필터링하여 바텀시트를 업데이트
     * 거리 가까운 순으로 정렬
     */
    private fun updateNearReports(location: Location) {
        val results = FloatArray(1)
        val nearReports = allMarkerItems
            .map { report ->
                Location.distanceBetween(location.latitude, location.longitude, report.latitude, report.longitude, results)
                report to results[0]
            }
            .filter { (_, dist) -> dist <= 1000f }
            .sortedBy { (_, dist) -> dist }
            .map { (report, _) -> report }

        if (nearReports.isEmpty()) {
            binding.rvNearReports.visibility   = View.GONE
            binding.layoutIndicator.visibility = View.GONE
            binding.tvNoReports.visibility     = View.VISIBLE
            nearReportAdapter.updateList(emptyList())
        } else {
            val behavior = BottomSheetBehavior.from(binding.layoutBottomNews)
            binding.tvNoReports.visibility     = View.GONE
            binding.rvNearReports.visibility   = View.VISIBLE
            binding.layoutIndicator.visibility =
                if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) View.GONE else View.VISIBLE
            nearReportAdapter.updateList(nearReports)
        }
    }

    /* 5초 간격, 5m 이상 이동 시 위치 업데이트를 요청 */
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

    /*
     * 카테고리 칩 선택 시 해당 카테고리의 마커만 지도에 표시
     * '전체' 선택 시 모든 마커를 표시
     */
    private fun initChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val categoryId: Int? = when (checkedIds.firstOrNull()) {
                R.id.chip_trash      -> 1
                R.id.chip_facilities -> 2
                R.id.chip_road       -> 3
                R.id.chip_any        -> 4
                else                 -> null // null = 전체
            }
            val filtered = if (categoryId == null) allMarkerItems
            else allMarkerItems.filter { it.categoryId == categoryId }
            displayRegisteredMarkers(filtered)
        }
    }

    /* 칩 선택/미선택 상태에 따른 배경색 및 글자색을 설정 */
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

    /*
     * 벡터 drawable을 Bitmap으로 변환
     * 카카오맵 SDK가 벡터를 직접 지원하지 않아 변환이 필요
     * 최대 크기는 48dp로 제한하여 마커가 너무 크게 표시되지 않도록 함
     */
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

// ─── 검색 어댑터 ────────────────────────────────────────────────────────────

/* 검색 결과 드롭다운 리스트 어댑터 */
class SearchResultAdapter(
    private var items: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].title
        holder.textView.setTextSize(14f)
        holder.itemView.setOnClickListener { onItemClick(items[position]) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Report>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// ─── 주변 소식 어댑터 ─────────────────────────────────────────────────────────

/*
 * 내 주변 소식 바텀시트 어댑터
 * - isVertical: true이면 세로(펼침), false이면 가로(접힘) 레이아웃
 * - 처리 상태에 따라 배지 색상이 다르게 표시
 *   접수완료(회색) / 처리중(노란색) / 처리완료(초록색)
 */
class NearReportAdapter(
    private var items: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<NearReportAdapter.ViewHolder>() {

    var isVertical: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_report_thumbnail)
        val tvTitle: TextView      = view.findViewById(R.id.tv_report_title)
        val tvTag: TextView        = view.findViewById(R.id.tv_report_tag)
        val tvEmpathy: TextView    = view.findViewById(R.id.tv_report_empathy)
        val tvStatus: TextView     = view.findViewById(R.id.tv_report_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_near_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val dp   = holder.itemView.context.resources.displayMetrics.density
        val lp   = holder.itemView.layoutParams

        // 바텀시트 상태에 따라 카드 너비 조절
        if (isVertical) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            (lp as? ViewGroup.MarginLayoutParams)?.marginEnd = 0
        } else {
            lp.width = (240 * dp).toInt()
            (lp as? ViewGroup.MarginLayoutParams)?.marginEnd = (12 * dp).toInt()
        }
        holder.itemView.layoutParams = lp

        holder.tvTitle.text   = item.title
        holder.tvEmpathy.text = "❤️ ${item.empathyCount}"
        holder.tvTag.text     = when (item.categoryId) {
            1    -> "쓰레기"
            2    -> "시설"
            3    -> "도로"
            else -> "기타"
        }

        // 처리 상태별 배지 색상 적용
        holder.tvStatus.text = item.status
        val (bgColor, textColor) = when (item.status) {
            "접수완료" -> "#E5E7EB" to "#4B5563"
            "처리중"   -> "#FEF3C7" to "#B45309"
            "처리완료" -> "#D1FAE5" to "#065F46"
            else       -> "#F3F4F6" to "#6B7280"
        }
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        holder.tvStatus.setTextColor(Color.parseColor(textColor))

        // TODO: 추후 실제 이미지 URL로 교체 필요
        holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        holder.ivThumbnail.setColorFilter(Color.parseColor("#9CA3AF"))
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Report>) {
        items = newItems
        notifyDataSetChanged()
    }
}