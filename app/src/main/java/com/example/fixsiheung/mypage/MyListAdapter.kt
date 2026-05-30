package com.example.fixsiheung.mypage

import android.content.res.ColorStateList
import android.graphics.Color
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

class MyListAdapter(
    private var items: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<MyListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_report_thumbnail)
        val tvTitle: TextView      = view.findViewById(R.id.tv_report_title)
        val tvTag: TextView        = view.findViewById(R.id.tv_report_tag)
        val tvEmpathy: TextView    = view.findViewById(R.id.tv_report_empathy)
        val tvStatus: TextView     = view.findViewById(R.id.tv_report_location)
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

        holder.tvTitle.text   = report.title
        holder.tvEmpathy.text = "❤️ ${report.empathyCount}"
        holder.tvTag.text     = when (report.categoryId) {
            1    -> "도로"
            2    -> "쓰레기"
            3    -> "시설"
            else -> "기타"
        }

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
            val fixedUrl = report.imageUrl.replace("127.0.0.1", "10.0.2.2")
            holder.ivThumbnail.clearColorFilter()
            Glide.with(holder.itemView.context)
                .load(fixedUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivThumbnail)
        } else {
            // imageUrl 없으면 상세 조회
            RetrofitClient.apiService.getComplaintDetail(report.complaintId)
                .enqueue(object : retrofit2.Callback<Report> {
                    override fun onResponse(call: retrofit2.Call<Report>, response: retrofit2.Response<Report>) {
                        if (response.isSuccessful) {
                            val detail = response.body() ?: return
                            val fixedUrl = detail.imageUrl?.replace("127.0.0.1", "10.0.2.2") ?: return
                            Glide.with(holder.itemView.context)
                                .load(fixedUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_gallery)
                                .centerCrop()
                                .into(holder.ivThumbnail)
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