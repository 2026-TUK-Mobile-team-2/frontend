package com.example.fixsiheung.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.FragmentSignupStep1Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupStep1Fragment : Fragment() {
    private var _binding: FragmentSignupStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by activityViewModels()

    // 중복 추적 함수
    private var isNicknameAvailable = false
    private var isEmailAvailable = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 화면 복원 처리
        val savedNickname = viewModel.nickname.value ?: ""
        binding.etSignupNickname.setText(savedNickname)
        binding.etSignupEmail.setText(viewModel.email.value)

        validateNicknameAndCheckDuplicate(savedNickname)
        validateEmailAndCheckDuplicate(binding.etSignupEmail.text.toString().trim())

        // 닉네임 실시간 입력 감지
        binding.etSignupNickname.addTextChangedListener {
            val nickname = it.toString().trim()
            viewModel.nickname.value = nickname
            isNicknameAvailable = false // 글자를 수정하면 무조건 다시 검사해야 하므로 false로 초기화
            validateNicknameAndCheckDuplicate(nickname)
            checkValidation() // 입력 중에는 다음 버튼 비활성화
        }

        // 이메일 실시간 입력 감지
        binding.etSignupEmail.addTextChangedListener {
            val email = it.toString().trim()
            viewModel.email.value = email
            isEmailAvailable = false
            validateEmailAndCheckDuplicate(email)
            checkValidation()
        }
    }

    // 수정 - 로컬 글자 수 검사 + 서버 중복 체크 통합 로직
    private fun validateNicknameAndCheckDuplicate(nickname: String) {
        when {
            // 1. 아무것도 입력하지 않았을 때
            nickname.isEmpty() -> {
                binding.tvNicknameStatus.visibility = View.GONE
                binding.layoutSignupNickname.endIconDrawable = null
                isNicknameAvailable = false
            }

            // 2. 글자 수 조건을 만족할 때 -> 서버에 중복확인
            nickname.length in 2..10 -> {
                binding.tvNicknameStatus.visibility = View.VISIBLE
                binding.tvNicknameStatus.text = "중복 확인 중..."
                binding.tvNicknameStatus.setTextColor(Color.parseColor("#8E94A0")) // 회색
                binding.layoutSignupNickname.endIconDrawable = null

                // 서버 API 호출
                val requestMap = mapOf("nickname" to nickname)
                com.example.fixsiheung.network.RetrofitClient.apiService.checkNickname(requestMap)
                    .enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(
                            call: Call<Map<String, Any>>,
                            response: Response<Map<String, Any>>
                        ) {
                            if (response.isSuccessful && response.body() != null) {
                                val isAvailable = response.body()?.get("isAvailable") as? Boolean ?: false
                                val message = response.body()?.get("message") as? String ?: ""

                                if (isAvailable) {
                                    // 사용 가능한 닉네임 (초록색)
                                    isNicknameAvailable = true
                                    binding.tvNicknameStatus.text = message
                                    binding.tvNicknameStatus.setTextColor(Color.parseColor("#10B981"))
                                    binding.layoutSignupNickname.endIconDrawable =
                                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                                    binding.layoutSignupNickname.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#10B981")))
                                } else {
                                    // 이미 사용 중인 닉네임 (빨간색)
                                    isNicknameAvailable = false
                                    binding.tvNicknameStatus.text = message
                                    binding.tvNicknameStatus.setTextColor(Color.parseColor("#EF4444"))
                                    binding.layoutSignupNickname.endIconDrawable = null
                                }
                                checkValidation() // 서버 응답을 받은 후 다음 버튼 상태 갱신
                            }
                        }

                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            isNicknameAvailable = false
                            binding.tvNicknameStatus.text = "서버 통신 오류가 발생했습니다."
                            binding.tvNicknameStatus.setTextColor(Color.parseColor("#EF4444"))
                            checkValidation()
                        }
                    })
            }

            // 3. 글자 수 조건 불만족 (서버에 물어볼 필요도 없이 에러)
            else -> {
                binding.tvNicknameStatus.visibility = View.VISIBLE
                binding.tvNicknameStatus.text = "닉네임은 2글자 이상 10글자 이하로 입력해주세요."
                binding.tvNicknameStatus.setTextColor(Color.parseColor("#EF4444"))
                binding.layoutSignupNickname.endIconDrawable = null
                isNicknameAvailable = false
            }
        }
    }

    private fun validateEmailAndCheckDuplicate(email: String) {
        when {
            email.isEmpty() -> {
                binding.tvEmailStatus.visibility = View.GONE
                binding.layoutSignupEmail.endIconDrawable = null
                isEmailAvailable = false
            }

            // 서버 통신
            Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "중복 확인 중..."
                binding.tvEmailStatus.setTextColor(Color.parseColor("#8E94A0"))
                binding.layoutSignupEmail.endIconDrawable = null

                val requestMap = mapOf("email" to email)
                com.example.fixsiheung.network.RetrofitClient.apiService.checkEmail(requestMap)
                    .enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                            if (response.isSuccessful && response.body() != null) {
                                val isAvailable = response.body()?.get("isAvailable") as? Boolean ?: false
                                val message = response.body()?.get("message") as? String ?: ""

                                if (isAvailable) {
                                    isEmailAvailable = true
                                    binding.tvEmailStatus.text = message
                                    binding.tvEmailStatus.setTextColor(Color.parseColor("#10B981"))
                                    binding.layoutSignupEmail.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                                    binding.layoutSignupEmail.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#10B981")))
                                } else {
                                    isEmailAvailable = false
                                    binding.tvEmailStatus.text = message
                                    binding.tvEmailStatus.setTextColor(Color.parseColor("#EF4444"))
                                    binding.layoutSignupEmail.endIconDrawable = null
                                }
                                checkValidation()
                            }
                        }
                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            isEmailAvailable = false
                            binding.tvEmailStatus.text = "서버 통신 오류"
                            binding.tvEmailStatus.setTextColor(Color.parseColor("#EF4444"))
                            checkValidation()
                        }
                    })
            }

            else -> {
                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "올바른 이메일 형식이 아닙니다."
                binding.tvEmailStatus.setTextColor(Color.parseColor("#EF4444"))
                binding.layoutSignupEmail.endIconDrawable = null
                isEmailAvailable = false
            }
        }
    }

    // 승인
    private fun checkValidation() {

        (activity as? SignupActivity)?.setNextButtonState(isNicknameAvailable && isEmailAvailable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}