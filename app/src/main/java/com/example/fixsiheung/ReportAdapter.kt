package com.example.fixsiheung

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.databinding.ItemNearReportBinding // ⭕ 새 XML 바인딩으로 변경

// item_near_report.xml 이랑 세트
class ReportAdapter(private var reportList: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemNearReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val currentReport = reportList[position]
        holder.bind(currentReport)

        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context
                val report = reportList[currentPosition]

                notifyItemChanged(currentPosition)

                val intent = Intent(context, ReportDetailActivity::class.java)

                intent.putExtra("detail_title", report.title)
                intent.putExtra("detail_content", report.description)
                intent.putExtra("detail_category", report.categoryId)
                intent.putExtra("detail_writer", report.userId)
                intent.putExtra("detail_like", report.empathyCount)

                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = reportList.size

    fun updateData(newList: List<Report>) {
        this.reportList = newList
        notifyDataSetChanged()
    }

    class ReportViewHolder(private val binding: ItemNearReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            binding.tvReportTitle.text = report.title
            binding.tvReportEmpathy.text = "❤️ 공감 ${report.empathyCount}"

            // 카테고리 숫자를 텍스트로
            binding.tvReportTag.text = when(report.categoryId) {
                1 -> "쓰레기"
                2 -> "시설파손"
                3 -> "안전위험"
                4 -> "도로위험"
                else -> "기타"
            }

            // 기본 갤러리 이미지 기본 설정 (나중에 이미지 URL 들어오면 Glide 코드가 들어갈 자리입니다)
            binding.ivReportThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
}