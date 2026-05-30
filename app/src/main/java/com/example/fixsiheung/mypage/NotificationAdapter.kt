package com.example.fixsiheung.mypage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fixsiheung.R
import com.example.fixsiheung.model.Notification

class NotificationAdapter(
    private var items: List<Notification>,
    private val prefs: android.content.SharedPreferences
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView     = view.findViewById(R.id.iv_notification_icon)
        val tvTitle: TextView     = view.findViewById(R.id.tv_notification_title)
        val tvMessage: TextView   = view.findViewById(R.id.tv_notification_message)
        val tvTime: TextView      = view.findViewById(R.id.tv_notification_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text   = item.title
        holder.tvMessage.text = item.message
        holder.tvTime.text    = getTimeAgo(item.createdAt)

        val isRead = prefs.getBoolean("notification_read_${item.id}", false)

        // 읽음 여부에 따라 배경색 변경
        holder.itemView.alpha = if (isRead) 0.6f else 1.0f

        // 클릭 시 읽음 처리
        holder.itemView.setOnClickListener {
            prefs.edit().putBoolean("notification_read_${item.id}", true).apply()
            holder.itemView.alpha = 0.6f
        }

        // 타입별 아이콘
        val iconRes = when (item.type) {
            "status"       -> R.drawable.ic_campaign
            "empathy"      -> R.drawable.ic_favorite
            "new_complaint"-> R.drawable.ic_notifications
            else           -> R.drawable.ic_notifications
        }
        holder.ivIcon.setImageResource(iconRes)
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Notification>) {
        items = newItems
        notifyDataSetChanged()
    }

    // 시간 표시 (몇 분 전, 몇 시간 전)
    private fun getTimeAgo(createdAt: Long): String {
        val diff = System.currentTimeMillis() - createdAt
        val minutes = diff / 60_000
        val hours   = diff / 3_600_000
        val days    = diff / 86_400_000

        return when {
            minutes < 1  -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24   -> "${hours}시간 전"
            else         -> "${days}일 전"
        }
    }
}