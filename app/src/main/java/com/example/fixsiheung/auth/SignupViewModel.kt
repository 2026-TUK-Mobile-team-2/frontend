package com.example.fixsiheung.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fixsiheung.R

class  SignupViewModel : ViewModel() {
    val nickname = MutableLiveData<String>("")

    val id = MutableLiveData<String>("")
    val email = MutableLiveData<String>("")
    val password = MutableLiveData<String>("")

    //에러 메시지 전달
    val idErrorMsg = MutableLiveData<String>("")

    // 현재 단계를 관리 (1단계 시작)
    val currentStep = MutableLiveData<Int>(1)
}