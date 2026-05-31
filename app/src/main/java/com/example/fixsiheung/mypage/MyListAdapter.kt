package com.example.fixsiheung.mypage

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.R
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.network.RetrofitClient
import com.bumptech.glide.Glide
import com.example.fixsiheung.AppPrefs

class MyListAdapter(
    private var items: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<MyListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_report_thumbnail)
        val tvTitle: TextView      = view.findViewById(R.id.tv_report_title)
        val tvTag: TextView        = view.findViewById(R.id.tv_report_tag)
        val tvEmpathy: TextView    = view.findViewById(R.id.tv_report_empathy)
        val tvStatus: TextView     = view.findViewById(R.id.tv_report_status)
        val tvLocation: TextView   = view.findViewById(R.id.tv_report_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_near_report, parent, false)
        view.layoutParams = view.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = items[position]
        Log.d("Image", "imageUrl: ${report.imageUrl}")
        holder.itemView.setOnClickListener { onItemClick(report) }

        holder.tvTitle.text   = report.title
        holder.tvEmpathy.text = "❤️ ${report.empathyCount}"
        holder.tvTag.text     = when (report.categoryId) {
            1 -> "쓰레기"
            2 -> "시설파손"
            3 -> "안전위험"
            4 -> "불법주차"
            5 -> "소음"
            6 -> "기타"
            else -> "기타"
        }
        holder.tvLocation.text = report.address ?: ""

        // 서버값 관계없이 표시 텍스트 통일
        holder.tvStatus.text = when (report.status) {
            "접수", "접수완료" -> "접수완료"
            "처리중"           -> "처리중"
            "완료", "처리완료" -> "처리완료"
            else               -> report.status ?: "접수완료"
        }

        // 처리 상태별 배지 색상
        val (bgColor, textColor) = when (report.status) {
            "접수완료", "접수" -> "#E5E7EB" to "#4B5563"
            "처리중"           -> "#FEF3C7" to "#B45309"
            "처리완료", "완료" -> "#D1FAE5" to "#065F46"
            else               -> "#F3F4F6" to "#6B7280"
        }
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        holder.tvStatus.setTextColor(Color.parseColor(textColor))

        if (!report.imageUrl.isNullOrEmpty()) {
            val fixedUrl = report.imageUrl.replace("127.0.0.1", "192.168.0.41")
            holder.ivThumbnail.clearColorFilter()
            Glide.with(holder.itemView.context)
                .load(fixedUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivThumbnail)
        } else {
            holder.itemView.tag = report.complaintId


            val currentUserId = AppPrefs.getUserId(holder.itemView.context)

            // 💡 3. API 호출 시 두 번째 파라미터로 currentUserId를 넣어줍니다!
            RetrofitClient.apiService.getComplaintDetail(report.complaintId, currentUserId)
                .enqueue(object : retrofit2.Callback<Report> {
                    override fun onResponse(call: retrofit2.Call<Report>, response: retrofit2.Response<Report>) {
                        if (response.isSuccessful) {
                            val detail = response.body() ?: return

                            Log.d("Image", "detail imageUrl: ${detail.imageUrl}")
                            Log.d("Image", "fixedUrl: ${detail.imageUrl?.replace("127.0.0.1", "192.168.0.41")}")

                            // 뷰홀더가 재사용되지 않고 제자리에 있는지 검사
                            if (holder.itemView.tag == detail.complaintId) {
                                holder.tvLocation.text = detail.address ?: ""

                                val fixedUrl = detail.imageUrl?.replace("127.0.0.1", "192.168.0.41")
                                if (!fixedUrl.isNullOrEmpty()) {
                                    Glide.with(holder.itemView.context)
                                        .load(fixedUrl)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .centerCrop()
                                        .into(holder.ivThumbnail)
                                } else {
                                    holder.ivThumbnail.clearColorFilter()
                                    holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<Report>, t: Throwable) {}
                })
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Report>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].complaintId == newItems[newPos].complaintId
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}