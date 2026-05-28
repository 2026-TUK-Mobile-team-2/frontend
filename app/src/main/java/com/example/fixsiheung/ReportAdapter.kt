package com.example.fixsiheung

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.databinding.ItemReportBinding

//item_report.xml 랑 세트

class ReportAdapter(private val reportList: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val currentReport = reportList[position]
        holder.bind(currentReport)

        holder.itemView.setOnClickListener {       //실시간 화면으로 바꿔서 클릭 문제 해결
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context
                val report = reportList[currentPosition]

                //report.viewCount += 1 //조회수 +1
                notifyItemChanged(currentPosition) // 리스트 화면의 조회수도 갱신 (필요 시)

                // ReportDetailActivity를 목적지로 하는 인텐트 생성
                val intent = Intent(context, ReportDetailActivity::class.java)

                // 📦 택배 상자에 클릭된 민원의 상세 데이터들을 실어 보냅니다.
                intent.putExtra("detail_title", report.title)
                intent.putExtra("detail_content", report.description)
                intent.putExtra("detail_category", report.categoryId)
                //intent.putExtra("detail_status", report.status)
                intent.putExtra("detail_writer", report.userId)
                intent.putExtra("detail_like", report.empathyCount)
                //intent.putExtra("detail_view", report.viewCount)

                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = reportList.size

    class ReportViewHolder(private val binding: ItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {      //어댑터 본체
            //report에 이미지url 추가 되면 이미지 코드 추가하기
            binding.itemTitle.text = report.title
            binding.itemCategory.text = "카테고리 ${report.categoryId.toString()}"
            //binding.itemstatus.text = report.status
            binding.itemlikeCount.text = "공감 ${report.empathyCount}"  //리스트에 공감+숫자로 보이게함
            
            binding.itemcomplaintId.text = "민원 ${report.complaintId.toString()}" //add잘 작동하는지 확인 용도
        }
    }
}