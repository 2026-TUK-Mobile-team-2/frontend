package com.example.fixsiheung.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.ActivitySignupBinding
import com.example.fixsiheung.network.RetrofitClient

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.signup_fragment_container, SignupStep1Fragment())
                .commit()
        }

        viewModel.currentStep.observe(this) { step ->
            if (step == 1) {
                updateProgressBar(0.5f, "1 / 2")
                binding.btnSignupNext.text = "다음"
            } else {
                updateProgressBar(1.0f, "2 / 2")
                binding.btnSignupNext.text = "가입 완료"
            }
        }

        binding.btnSignupNext.setOnClickListener {
            if (viewModel.currentStep.value == 1) {
                viewModel.currentStep.value = 2
                supportFragmentManager.beginTransaction()
                    .replace(R.id.signup_fragment_container, SignupStep2Fragment())
                    .addToBackStack(null) // 뒤로가기 누르면 1단계로 리턴 가능하게 함
                    .commit()
            } else {
                val finalId = viewModel.id.value ?: ""
                val finalPassword = viewModel.password.value ?: ""
                val finalEmail = viewModel.email.value ?: ""
                val finalNickname = viewModel.nickname.value ?: ""

                // name 없음
                val signupRequest = com.example.fixsiheung.model.SignupRequest(
                    userId = finalId,
                    password = finalPassword,
                    email = finalEmail,
                    nickname = finalNickname
                )

                com.example.fixsiheung.network.RetrofitClient.apiService.signup(signupRequest)
                    .enqueue(object : retrofit2.Callback<Any> {
                        override fun onResponse(call: retrofit2.Call<Any>, response: retrofit2.Response<Any>) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@SignupActivity, "${finalNickname}님 환영합니다!", Toast.LENGTH_SHORT).show()

                                val loginRequest = com.example.fixsiheung.model.LoginRequest(
                                    userId = finalId,
                                    password = finalPassword
                                )
                                com.example.fixsiheung.network.RetrofitClient.apiService.login(loginRequest)
                                    .enqueue(object : retrofit2.Callback<com.example.fixsiheung.model.LoginResponse> {
                                        override fun onResponse(call: retrofit2.Call<com.example.fixsiheung.model.LoginResponse>, response: retrofit2.Response<com.example.fixsiheung.model.LoginResponse>) {
                                            if (response.isSuccessful) {
                                                val body = response.body()!!
                                                getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putString("user_id", body.userId)
                                                    .putString("user_name", body.name)
                                                    .putBoolean("is_admin", false)
                                                    .apply()

                                                // FCM 토큰 전송 후 화면 이동
                                                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val token = task.result
                                                            RetrofitClient.apiService.updateFcmToken(
                                                                body.userId,
                                                                com.example.fixsiheung.model.FcmTokenRequest(token)
                                                            ).enqueue(object : retrofit2.Callback<Map<String, String>> {
                                                                override fun onResponse(call: retrofit2.Call<Map<String, String>>, response: retrofit2.Response<Map<String, String>>) {
                                                                    Log.d("FCM", "토큰 전송 성공")
                                                                }
                                                                override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {
                                                                    Log.e("FCM", "토큰 전송 실패: ${t.message}")
                                                                }
                                                            })
                                                        }
                                                        // 토큰 성공/실패 관계없이 화면 이동
                                                        val intent = Intent(this@SignupActivity, MainMapActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)
                                                    }
                                            }
                                        }
                                        override fun onFailure(call: retrofit2.Call<com.example.fixsiheung.model.LoginResponse>, t: Throwable) {
                                            val intent = Intent(this@SignupActivity, MainMapActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                    })
                            } else {
                                viewModel.idErrorMsg.value = "이미 사용 중이거나 가입된 아이디입니다."
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Any>, t: Throwable) {
                            Toast.makeText(this@SignupActivity, "서버 네트워크 통신 오류", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    private fun updateProgressBar(percent: Float, text: String) {
        val params =
            binding.vSignupProgress.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.matchConstraintPercentWidth = percent
        binding.vSignupProgress.layoutParams = params
        binding.tvStepIndicator.text = text
    }

    fun setNextButtonState(isEnable: Boolean) {
        binding.btnSignupNext.isEnabled = isEnable
        if (isEnable) {
            binding.btnSignupNext.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#00696B"))
            binding.btnSignupNext.setTextColor(Color.WHITE)
        } else {
            binding.btnSignupNext.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            binding.btnSignupNext.setTextColor(Color.parseColor("#8E94A0"))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            viewModel.currentStep.value = 1
        } else {
            finish()
        }
        return true
    }
}