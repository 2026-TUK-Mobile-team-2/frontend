package com.example.fixsiheung

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    private const val PREFS_NAME = "fix_siheung_prefs"

    // 💡 로그인 성공 시 아이디 저장하기
    fun saveUserId(context: Context, userId: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("USER_ID", userId).apply()
    }

    // 💡 저장된 내 아이디 꺼내오기 (저장된 값이 없으면 빈 문자열 "" 반환)
    fun getUserId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("USER_ID", "") ?: ""
    }

    // (필요하다면 나중에 닉네임이나 다른 정보도 같은 방식으로 추가할 수 있습니다)
}