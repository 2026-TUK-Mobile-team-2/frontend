package com.example.fixsiheung

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.databinding.ItemNearReportBinding // ⭕ 바뀐 레이아웃 바인딩으로 조준 변경!

class ReportAdapter(private var reportList: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        // 💡 [수동 변경 1] 구버전 item_report 대신 item_near_report 레이아웃을 인플레이트합니다.
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

                // ReportDetailActivity를 목적지로 하는 인텐트 생성
                val intent = Intent(context, ReportDetailActivity::class.java)

                // 📦 택배 상자에 클릭된 민원의 상세 데이터들을 실어 보냅니다.
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

    fun updateData(newItems: List<Report>) {
        this.reportList = newItems
        notifyDataSetChanged()
    }

    // 💡 [수동 변경 2] 뷰홀더가 사용하는 바인딩 클래스도 ItemNearReportBinding으로 교체합니다.
    class ReportViewHolder(private val binding: ItemNearReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            // 💡 [수동 변경 3] item_near_report.xml에 설계된 실제 새 부품 ID들과 데이터를 매핑합니다.
            binding.tvReportTitle.text = report.title
            binding.tvReportEmpathy.text = "❤️ ${report.empathyCount}"

            // 카테고리 숫자를 이쁜 한글 텍스트로 변환해서 태그에 입력
            binding.tvReportTag.text = when (report.categoryId) {
                1    -> "쓰레기"
                2    -> "시설파손"
                3    -> "도로위험"
                4    -> "안전위험"
                else -> "기타"
            }

            // 기본 썸네일 이미지 및 회색 필터 세팅
            binding.ivReportThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            binding.ivReportThumbnail.setColorFilter(Color.parseColor("#9CA3AF"))
        }
    }
}