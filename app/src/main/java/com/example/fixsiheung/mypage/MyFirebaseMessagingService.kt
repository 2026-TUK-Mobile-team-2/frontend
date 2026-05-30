package com.example.fixsiheung

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val prefs = getSharedPreferences("notification_prefs", MODE_PRIVATE)

        // 전체 알림 꺼져있으면 무시
        if (!prefs.getBoolean("all", true)) return

        val type = remoteMessage.data["type"] ?: ""

        // 타입별 알림 설정 확인
        val isEnabled = when (type) {
            "status"        -> prefs.getBoolean("status", true)
            "empathy"       -> prefs.getBoolean("empathy", true)
            "new_complaint" -> prefs.getBoolean("new_complaint", true)
            else            -> true
        }

        if (!isEnabled) return

        val title   = remoteMessage.notification?.title ?: "시흥고쳐줘"
        val message = remoteMessage.notification?.body  ?: ""

        sendNotification(title, message)
    }

    // FCM 토큰이 갱신될 때 호출 → 서버에 새 토큰 저장
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("user_id", null) ?: return

        com.example.fixsiheung.network.RetrofitClient.apiService.updateFcmToken(
            userId,
            com.example.fixsiheung.model.FcmTokenRequest(token)
        ).enqueue(object : retrofit2.Callback<Map<String, String>> {
            override fun onResponse(call: retrofit2.Call<Map<String, String>>, response: retrofit2.Response<Map<String, String>>) {}
            override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {}
        })
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "fixsiheung_channel"

        // 알림 클릭 시 앱 실행
        val intent = Intent(this, MainMapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상은 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "시흥고쳐줘 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}