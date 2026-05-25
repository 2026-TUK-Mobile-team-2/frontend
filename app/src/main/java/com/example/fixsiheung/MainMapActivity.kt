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

class MainMapActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainMapBinding.inflate(layoutInflater) }
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
        initSearchBar()
        initNearReports()

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
            Report(1, "a", 1, "쓰레기 무단투기",  "정왕역 앞 골목길에 쓰레기 무단투기가 너무 심합니다.",       "접수",  37.341500, 126.732500, "경기도 시흥시 정왕동 2321",        15, now - oneHour,     now - oneHour),
            Report(2, "b", 2, "파손된 벤치 수리", "놀이터 옆 벤치 나무가 부서져 아이들이 다칠 위험이 있습니다.", "처리중", 37.339500, 126.735000, "경기도 시흥시 정왕동 1700 공원내",  3,  now - oneDay * 2,  now - oneDay),
            Report(3, "c", 3, "아스팔트 포트홀",  "서해안로 2차선 도로에 깊은 포트홀이 생겼습니다.",           "접수",  37.342000, 126.734000, "경기도 시흥시 정왕동 1284-4 도로", 24, now - oneDay * 3,  now - oneDay * 3),
            Report(4, "d", 4, "가로등 소등 신고", "골목 가로등이 완전히 꺼졌습니다. 밤길이 너무 어둡습니다.",   "해결",  37.340000, 126.736500, "경기도 시흥시 정왕동 1502-1",      0,  now - oneHour / 2, now - oneHour / 2),
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

    private fun displayRegisteredMarkers(items: List<Report>) {
        val layer = kakaoMap?.labelManager?.layer ?: return
        registeredLabels.forEach { it.remove() }
        registeredLabels.clear()

        val now         = System.currentTimeMillis()
        val twentyFourH = 86_400_000L
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
                    .setTag(report)
                    .setRank(5)
            )?.let { registeredLabels.add(it) }
        }
    }

    private fun createBadgeMarkerBitmap(baseBitmap: Bitmap, isHot: Boolean, isNew: Boolean): Bitmap {
        if (!isHot && !isNew) return baseBitmap

        val dp          = resources.displayMetrics.density
        val badgeH      = (12 * dp).toInt()
        val badgeW      = (24 * dp).toInt()
        val gap         = (2  * dp).toInt()
        val overlap     = (-15 * dp).toInt()
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
            if (isHot) add("#FF3B30" to "HOT")
            if (isNew) add("#3B82F6" to "NEW")
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

    private fun initSearchBar() {
        searchAdapter = SearchResultAdapter(emptyList()) { report ->
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(report.latitude, report.longitude), 16),
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
                binding.rvSearchResults.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
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
                CameraUpdateFactory.newCenterPosition(LatLng.from(report.latitude, report.longitude), 16),
                CameraAnimation.from(500, true, true)
            )
            BottomSheetBehavior.from(binding.layoutBottomNews).state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.rvNearReports.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvNearReports.adapter = nearReportAdapter

        binding.rvNearReports.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val maxScroll = recyclerView.computeHorizontalScrollRange() - recyclerView.computeHorizontalScrollExtent()
                if (maxScroll <= 0) return
                val ratio     = recyclerView.computeHorizontalScrollOffset().toFloat() / maxScroll
                val maxMoveX  = (binding.layoutIndicator.width - binding.viewIndicatorBar.width).toFloat()
                binding.viewIndicatorBar.translationX = ratio * maxMoveX
            }
        })

        BottomSheetBehavior.from(binding.layoutBottomNews)
            .addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (nearReportAdapter.itemCount == 0) {
                        binding.rvNearReports.visibility   = View.GONE
                        binding.layoutIndicator.visibility = View.GONE
                        binding.tvNoReports.visibility     = View.VISIBLE
                        return
                    }
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.rvNearReports.layoutManager = LinearLayoutManager(this@MainMapActivity, LinearLayoutManager.VERTICAL, false)
                            binding.rvNearReports.recycledViewPool.clear()
                            nearReportAdapter.isVertical = true
                            nearReportAdapter.notifyDataSetChanged()
                            binding.layoutIndicator.visibility = View.GONE
                            binding.tvNoReports.visibility     = View.GONE
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.rvNearReports.layoutManager = LinearLayoutManager(this@MainMapActivity, LinearLayoutManager.HORIZONTAL, false)
                            binding.rvNearReports.recycledViewPool.clear()
                            nearReportAdapter.isVertical = false
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

    var isVertical: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_report_thumbnail)
        val tvTitle: TextView      = view.findViewById(R.id.tv_report_title)
        val tvTag: TextView        = view.findViewById(R.id.tv_report_tag)
        val tvEmpathy: TextView    = view.findViewById(R.id.tv_report_empathy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_near_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val dp   = holder.itemView.context.resources.displayMetrics.density
        val lp   = holder.itemView.layoutParams

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