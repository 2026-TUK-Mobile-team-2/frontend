package com.example.fixsiheung.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.ActivitySignupBinding

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
                // 서버에 아이디, 비밀번호 넘겨야함
                val finalNickname = viewModel.nickname.value
                Toast.makeText(this, "${finalNickname}님 회원가입 완료!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainMapActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
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