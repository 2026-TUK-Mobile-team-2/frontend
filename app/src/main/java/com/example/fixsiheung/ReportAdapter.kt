package com.example.fixsiheung

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.databinding.ItemNearReportBinding

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

                // notifyItemChanged(currentPosition) // 이 줄은 상세 페이지 이동 시 굳이 필요 없으므로 제거했습니다.

                val intent = Intent(context, ReportDetailActivity::class.java)

                // 💡 핵심 수정 부분: 기존의 detail_... 대신 COMPLAINT_ID 를 넘겨줍니다.
                intent.putExtra("COMPLAINT_ID", report.complaintId)

                // (선택) 로그인 유저 정보가 아직 없다면 테스트를 위해 임시 USER_ID 를 넘겨줍니다.
                // 상세 페이지의 '공감하기' 기능 테스트를 위해 필요합니다.
                intent.putExtra("USER_ID", report.userId)

                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = reportList.size

    fun updateData(newItems: List<Report>) {
        this.reportList = newItems
        notifyDataSetChanged()
    }

    class ReportViewHolder(private val binding: ItemNearReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            // 1. 타이틀 및 공감수 데이터 바인딩
            binding.tvReportTitle.text = report.title
            binding.tvReportEmpathy.text = "❤️ 공감 ${report.empathyCount}"

            // 2. 카테고리 숫자를 매칭되는 한글 텍스트 태그로 치환
            binding.tvReportTag.text = when(report.categoryId) {
                1 -> "쓰레기"
                2 -> "시설파손"
                3 -> "안전위험"
                4 -> "불법주차"
                5 -> "소음"
                else -> "기타"
            }
            binding.tvReportStatus.text = when(report.status) {
                "접수", "접수완료" -> "접수완료"
                "처리중"           -> "처리중"
                "완료", "처리완료" -> "처리완료"
                else               -> report.status ?: "접수완료"
            }
            val (bgColor, textColor) = when(report.status) {
                "접수완료", "접수" -> "#E5E7EB" to "#4B5563"
                "처리중"           -> "#FEF3C7" to "#B45309"
                "처리완료", "완료" -> "#D1FAE5" to "#065F46"
                else               -> "#F3F4F6" to "#6B7280"
            }
            binding.tvReportStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.tvReportStatus.setTextColor(Color.parseColor(textColor))

            if (!report.imageUrl.isNullOrEmpty()) {
                // DB에 있는 127.0.0.1 주소를 컴퓨터의 실제 내부 IP로 치환
                val fixedUrl = report.imageUrl.replace("127.0.0.1", "192.168.0.41")

                // 기존에 칠해져 있던 회색 필터 제거
                binding.ivReportThumbnail.clearColorFilter()

                Glide.with(itemView.context)
                    .load(fixedUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery) // 로딩 중 보여줄 이미지
                    .error(android.R.drawable.ic_menu_gallery)       // 통신 실패 시 보여줄 이미지
                    .centerCrop()
                    .into(binding.ivReportThumbnail)
            } else {
                // 이미지가 아예 없는 글일 경우에만 기본 회색 아이콘 표시
                binding.ivReportThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                binding.ivReportThumbnail.setColorFilter(Color.parseColor("#9CA3AF"))
            }
        }
    }
}