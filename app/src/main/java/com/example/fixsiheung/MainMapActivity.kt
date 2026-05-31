package com.example.fixsiheung

import android.Manifest
import android.content.Intent
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
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
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
import com.example.fixsiheung.mypage.MyPageActivity
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
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.security.MessageDigest

class MainMapActivity : AppCompatActivity() {

    private var kakaoMap: KakaoMap? = null
    private var myLocationLabel: Label? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var nearReportAdapter: NearReportAdapter

    private val registeredLabels = mutableListOf<Label>()
    private var allMarkerItems = listOf<Report>()
    private val cachedStyles = mutableMapOf<String, LabelStyles>()

    private val schoolLatLng = LatLng.from(37.340174, 126.733593)

    private val categoryDrawables = mapOf(
        1 to R.drawable.ic_marker_trash,
        2 to R.drawable.ic_marker_facilities,
        3 to R.drawable.ic_marker_parking,
        4 to R.drawable.ic_marker_danger,
        5 to R.drawable.ic_marker_noise,
        6 to R.drawable.ic_marker_any
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

    private lateinit var binding: ActivityMainMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding 연결
        binding = ActivityMainMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비 기본 선택
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        // 하단 네비 클릭 이벤트
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 현재 지도 화면이므로 홈 클릭 시 별도 동작 없음
                    true
                }

                R.id.nav_list -> {
                    startActivity(Intent(this, ReportListActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                R.id.nav_report -> {
                    startActivity(Intent(this, ReportAddActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                R.id.nav_mypage -> {
                    startActivity(Intent(this, MyPageActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                else -> false
            }
        }

        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        for (signature in info.signatures!!) {
            val md = MessageDigest.getInstance("SHA")
            md.update(signature.toByteArray())
            val hashKey = String(Base64.encode(md.digest(), 0))
            Log.d("KeyHash", "KeyHash: $hashKey")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initKakaoMap()
        initSearchBar()
        initNearReports()
        initChipFilter()

        // API 호출
        com.example.fixsiheung.network.RetrofitClient.apiService.getAllReports()
            .enqueue(object : retrofit2.Callback<List<Report>> {
                override fun onResponse(
                    call: retrofit2.Call<List<Report>>,
                    response: retrofit2.Response<List<Report>>
                ) {
                    if (response.isSuccessful) {
                        allMarkerItems = response.body() ?: emptyList()
                        runOnUiThread {
                            displayRegisteredMarkers(allMarkerItems)
                            nearReportAdapter.updateList(allMarkerItems)
                        }
                    }
                }

                override fun onFailure(call: retrofit2.Call<List<Report>>, t: Throwable) {
                    Log.e("MainMap", "API 실패: ${t.message}")
                }
            })

        binding.tvViewAll.setOnClickListener {
            startActivity(Intent(this, ReportListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
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

    private var backPressedTime = 0L

    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            finishAffinity()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
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
                kakaoMap.setOnLabelClickListener { kakaoMap, layer, label ->
                    val report = label.tag as? Report
                    if (report != null) {
                        val intent = Intent(this@MainMapActivity, ReportDetailActivity::class.java).apply {
                            putExtra("intent_complaint_id", report.complaintId)
                        }
                        startActivity(intent)
                        true
                    } else {
                        false
                    }
                }
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
                "$id" to createBadgeMarkerBitmap(base, isHot = false, isNew = false),
                "${id}_hot" to createBadgeMarkerBitmap(base, isHot = true, isNew = false),
                "${id}_new" to createBadgeMarkerBitmap(base, isHot = false, isNew = true),
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

        val now = System.currentTimeMillis()
        val twentyFourH = 86_400_000L
        val topHotIds =
            allMarkerItems.sortedByDescending { it.empathyCount }.take(2).map { it.complaintId }

        items.forEach { report ->
            val isHot = report.complaintId in topHotIds
            val isNew = try {
                val sdf =
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val date = sdf.parse(report.createdAt ?: "")
                date != null && (now - date.time) <= twentyFourH
            } catch (e: Exception) {
                false
            }

            val styleKey = when {
                isHot && isNew -> "${report.categoryId}_hot_new"
                isHot -> "${report.categoryId}_hot"
                isNew -> "${report.categoryId}_new"
                else -> report.categoryId.toString()
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

    private fun createBadgeMarkerBitmap(
        baseBitmap: Bitmap,
        isHot: Boolean,
        isNew: Boolean
    ): Bitmap {
        if (!isHot && !isNew) return baseBitmap

        val dp = resources.displayMetrics.density
        val badgeH = (12 * dp).toInt()
        val badgeW = (24 * dp).toInt()
        val gap = (2 * dp).toInt()
        val overlap = (-15 * dp).toInt()
        val badgeCount = listOf(isHot, isNew).count { it }
        val totalBadgeH = badgeH * badgeCount + gap * (badgeCount - 1)

        val resultW = maxOf(baseBitmap.width, badgeW)
        val resultH = baseBitmap.height + totalBadgeH + overlap
        val result = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(
            baseBitmap,
            (resultW - baseBitmap.width) / 2f,
            (totalBadgeH + overlap).toFloat(),
            null
        )

        val badgePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8 * dp
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val badgeLeft = (resultW - badgeW) / 2f
        buildList {
            if (isHot) add("#FF3B30" to "HOT")
            if (isNew) add("#3B82F6" to "NEW")
        }.forEachIndexed { i, (colorHex, label) ->
            val top = i * (badgeH + gap)
            val rectF =
                RectF(badgeLeft, top.toFloat(), badgeLeft + badgeW, (top + badgeH).toFloat())
            badgePaint.color = Color.parseColor(colorHex)
            canvas.drawRoundRect(rectF, badgeH / 2f, badgeH / 2f, badgePaint)
            val textY = top + badgeH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, resultW / 2f, textY, textPaint)
        }

        return result
    }

    // ─── 검색 ──────────────────────────────────────────────────────────────────

    private fun initSearchBar() {
        searchAdapter = SearchResultAdapter(emptyList()) { report ->
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                    LatLng.from(
                        report.latitude,
                        report.longitude
                    ), 16
                ),
                CameraAnimation.from(500, true, true)
            )
            displayRegisteredMarkers(listOf(report))
            binding.searchBar.setText(report.title)
            binding.rvSearchResults.visibility = View.GONE
            hideKeyboard()
        }
        binding.rvSearchResults.adapter = searchAdapter

        binding.searchBar.addTextChangedListener { editable ->
            val keyword = editable?.toString()?.trim() ?: ""
            binding.icClearText.visibility = if (keyword.isNotEmpty()) View.VISIBLE else View.GONE

            if (keyword.isEmpty()) {
                binding.rvSearchResults.visibility = View.GONE
                displayRegisteredMarkers(allMarkerItems)
            } else {
                val filtered = allMarkerItems
                    .filter { it.title.contains(keyword, ignoreCase = true) }
                    .sortedBy { it.title.indexOf(keyword, ignoreCase = true) }
                binding.rvSearchResults.visibility =
                    if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
                if (filtered.isNotEmpty()) searchAdapter.updateList(filtered)
            }
        }

        binding.icClearText.setOnClickListener {
            binding.searchBar.text?.clear()
            binding.searchBar.clearFocus()
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    // ─── 바텀시트 (내 주변 소식) ────────────────────────────────────────────────

    private fun initNearReports() {
        nearReportAdapter = NearReportAdapter(allMarkerItems) { report ->
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                    LatLng.from(
                        report.latitude,
                        report.longitude
                    ), 16
                ),
                CameraAnimation.from(500, true, true)
            )
        }

        binding.rvNearReports.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvNearReports.adapter = nearReportAdapter
    }

    // ─── 현재 위치 ─────────────────────────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val myPosition = LatLng.from(location.latitude, location.longitude)
            val layer = kakaoMap?.labelManager?.layer ?: return

            if (myLocationLabel == null) {
                val labelManager = kakaoMap?.labelManager ?: return
                val styles = labelManager.addLabelStyles(
                    LabelStyles.from(
                        "myStyle",
                        LabelStyle.from(vectorToBitmap(R.drawable.my_marker))
                            .setAnchorPoint(0.5f, 0.5f)
                    )
                )
                myLocationLabel =
                    layer.addLabel(LabelOptions.from(myPosition).setStyles(styles).setRank(10))
            } else {
                myLocationLabel?.moveTo(myPosition)
            }
            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPosition))
            updateNearReports(location)
        }
    }

    private fun updateNearReports(location: Location) {
        val results = FloatArray(1)
        val nearReports = allMarkerItems
            .map { report ->
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    report.latitude,
                    report.longitude,
                    results
                )
                report to results[0]
            }
            .filter { (_, dist) -> dist <= 1000f }
            .sortedBy { (_, dist) -> dist }
            .map { (report, _) -> report }

        if (nearReports.isEmpty()) {
            binding.rvNearReports.visibility = View.GONE
            binding.tvNoReports.visibility = View.VISIBLE
            nearReportAdapter.updateList(emptyList())
        } else {
            binding.tvNoReports.visibility = View.GONE
            binding.rvNearReports.visibility = View.VISIBLE
            nearReportAdapter.updateList(nearReports)
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
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // ─── 칩 필터 ───────────────────────────────────────────────────────────────

    private fun initChipFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val categoryId: Int? = when (checkedIds.firstOrNull()) {
                R.id.chip_trash -> 1
                R.id.chip_facilities -> 2
                R.id.chip_danger -> 3
                R.id.chip_parking -> 4
                R.id.chip_noise -> 5
                R.id.chip_any -> 6
                else -> null
            }
            val filtered = if (categoryId == null) allMarkerItems
            else allMarkerItems.filter { it.categoryId == categoryId }
            displayRegisteredMarkers(filtered)
        }
    }

    // ─── 유틸 ──────────────────────────────────────────────────────────────────

    private fun vectorToBitmap(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!
        val maxPx = (48 * resources.displayMetrics.density).toInt()

        var w = drawable.intrinsicWidth
        var h = drawable.intrinsicHeight

        if (w > maxPx || h > maxPx) {
            if (w > h) {
                h = (h * (maxPx.toFloat() / w)).toInt(); w = maxPx
            } else {
                w = (w * (maxPx.toFloat() / h)).toInt(); h = maxPx
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(Canvas(it))
        }
    }
}

// ─── 검색 어댑터 ────────────────────────────────────────────────────────────

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

class NearReportAdapter(

    private var items: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<NearReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_report_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_report_title)
        val tvTag: TextView = view.findViewById(R.id.tv_report_tag)
        val tvEmpathy: TextView = view.findViewById(R.id.tv_report_empathy)
        val tvStatus: TextView = view.findViewById(R.id.tv_report_status)
        val tvLocation: TextView = view.findViewById(R.id.tv_report_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_near_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val lp = holder.itemView.layoutParams

        holder.itemView.layoutParams = lp

        holder.tvTitle.text = item.title
        holder.tvEmpathy.text = "❤️ ${item.empathyCount}"
        holder.tvTag.text = when (item.categoryId) {
            1 -> "쓰레기"
            2 -> "시설파손"
            3 -> "안전위험"
            4 -> "불법주차"
            5 -> "소음"
            6 -> "기타"
            else -> "기타"
        }

        // 상태 표시
        holder.tvStatus.text = when (item.status) {
            "접수", "접수완료" -> "접수완료"
            "처리중" -> "처리중"
            "완료", "처리완료" -> "처리완료"
            else -> item.status ?: "접수완료"
        }
        val (bgColor, textColor) = when (item.status) {
            "접수완료", "접수" -> "#E5E7EB" to "#4B5563"
            "처리중" -> "#FEF3C7" to "#B45309"
            "처리완료", "완료" -> "#D1FAE5" to "#065F46"
            else -> "#F3F4F6" to "#6B7280"
        }
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        holder.tvStatus.setTextColor(Color.parseColor(textColor))

        // 주소 표시
        holder.tvLocation.text = item.address ?: ""

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