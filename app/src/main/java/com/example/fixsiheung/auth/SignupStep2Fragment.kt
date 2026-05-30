package com.example.fixsiheung.auth

import android.R.attr.text
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.FragmentSignupStep2Binding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginBottomSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignupStep2Fragment : Fragment() {
    private var _binding: FragmentSignupStep2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by activityViewModels()

    private var isIdAvailable = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etSignupId.setText(viewModel.id.value)
        binding.etSignupPassword.setText(viewModel.password.value)
        validateIdAndCheckDuplicate(binding.etSignupId.text.toString().trim())
        validatePassword(binding.etSignupPassword.text.toString().trim())
        validatePasswordMatch(
            binding.etSignupPassword.text.toString().trim(),
            binding.etSignupPassword2.text.toString().trim()
        )
        checkValidation()

        // 추가: 서버 에러 메시지가 도착하면 화면에 띄움
        viewModel.idErrorMsg.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                binding.tvIdStatus.visibility = View.VISIBLE
                binding.tvIdStatus.text = errorMsg
                binding.tvIdStatus.setTextColor(Color.parseColor("#EF4444")) // 빨간색
                binding.layoutSignupId.endIconDrawable = null // 초록색 체크마크 숨김
            }
        }

        binding.etSignupId.addTextChangedListener {
            val id = it.toString().trim()
            viewModel.id.value = id

            // 사용자가 새 아이디를 다시 타이핑하기 시작하면 에러를 지움
            viewModel.idErrorMsg.value = ""
            isIdAvailable = false
            validateIdAndCheckDuplicate(id)
            checkValidation()
        }

        binding.etSignupPassword.addTextChangedListener {
            val password = it.toString().trim()
            viewModel.password.value = password
            validatePassword(password)

            val passwordConfirm = binding.etSignupPassword2.text.toString().trim()
            validatePasswordMatch(password, passwordConfirm)
            checkValidation()
        }

        binding.etSignupPassword2.addTextChangedListener {
            val password2 = it.toString().trim()
            val password = binding.etSignupPassword.text.toString().trim()
            validatePasswordMatch(password, password2)
            checkValidation()
        }
    }

    private fun validateIdAndCheckDuplicate(id: String) {
        when {
            id.isEmpty() -> {
                binding.tvIdStatus.visibility = View.GONE
                binding.layoutSignupId.endIconDrawable = null
                isIdAvailable = false
            }

            id.length in 2..10 -> {
                binding.tvIdStatus.visibility = View.VISIBLE
                binding.tvIdStatus.text = "중복 확인 중..."
                binding.tvIdStatus.setTextColor(Color.parseColor("#8E94A0"))
                binding.layoutSignupId.endIconDrawable = null

                val requestMap = mapOf("user_id" to id)
                com.example.fixsiheung.network.RetrofitClient.apiService.checkId(requestMap)
                    .enqueue(object : retrofit2.Callback<Map<String, Any>> {
                        override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                            if (response.isSuccessful && response.body() != null) {
                                val isAvailable = response.body()?.get("isAvailable") as? Boolean ?: false
                                val message = response.body()?.get("message") as? String ?: ""

                                if (isAvailable) {
                                    isIdAvailable = true
                                    binding.tvIdStatus.text = message
                                    binding.tvIdStatus.setTextColor(Color.parseColor("#10B981"))
                                    binding.layoutSignupId.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                                    binding.layoutSignupId.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#10B981")))
                                } else {
                                    isIdAvailable = false
                                    binding.tvIdStatus.text = message
                                    binding.tvIdStatus.setTextColor(Color.parseColor("#EF4444"))
                                    binding.layoutSignupId.endIconDrawable = null
                                }
                                checkValidation()
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                            isIdAvailable = false
                            binding.tvIdStatus.text = "서버 통신 오류"
                            binding.tvIdStatus.setTextColor(Color.parseColor("#EF4444"))
                            checkValidation()
                        }
                    })
            }

            else -> {
                binding.tvIdStatus.visibility = View.VISIBLE
                binding.tvIdStatus.text = "아이디는 2글자 이상 10글자 이하로 입력해주세요."
                binding.tvIdStatus.setTextColor(Color.parseColor("#EF4444"))
                binding.layoutSignupId.endIconDrawable = null
                isIdAvailable = false
            }
        }
    }

    private fun validatePassword(password: String) {
        if (password.isEmpty()) {
            binding.tvPasswordStatus1.visibility = View.GONE
            binding.tvPasswordStatus2.visibility = View.GONE
            binding.tvPasswordStatus3.visibility = View.GONE
            binding.tvPasswordStatus4.visibility = View.GONE
            return
        }

        // 4가지 조건 실시간 정규식 검증
        val isLengthValid = password.length >= 8
        val hasLetter = password.contains(Regex("[a-zA-Z]"))
        val hasNumber = password.contains(Regex("[0-9]"))
        val hasSpecial = password.contains(Regex("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]"))

        binding.tvPasswordStatus1.visibility = View.VISIBLE
        binding.tvPasswordStatus2.visibility = View.VISIBLE
        binding.tvPasswordStatus3.visibility = View.VISIBLE
        binding.tvPasswordStatus4.visibility = View.VISIBLE

        updateStatusUi(binding.tvPasswordStatus1, isLengthValid)
        updateStatusUi(binding.tvPasswordStatus2, hasLetter)
        updateStatusUi(binding.tvPasswordStatus3, hasNumber)
        updateStatusUi(binding.tvPasswordStatus4, hasSpecial)
    }

    private fun updateStatusUi(textView: TextView, isValid: Boolean) {
        if (isValid) {
            // 조건 충족: 초록색 텍스트 + 체크 아이콘
            textView.setTextColor(Color.parseColor("#10B981"))
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            textView.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        } else {
            // 조건 미충족: 회색 텍스트 + X 아이콘
            textView.setTextColor(Color.parseColor("#8E94A0"))
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel_circle, 0, 0, 0)
            textView.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#8E94A0"))
        }
    }

    private fun validatePasswordMatch(password: String, password2: String) {
        when {
            password2.isEmpty() -> {
                binding.tvPassword2Status.visibility = View.GONE
            }
            // 두 비밀번호가 똑같을때
            password == password2 -> {
                binding.tvPassword2Status.visibility = View.VISIBLE
                binding.tvPassword2Status.text = "비밀번호가 일치합니다."
                binding.tvPassword2Status.setTextColor(Color.parseColor("#10B981")) // 초록색
            }

            password != password2 -> {
                binding.tvPassword2Status.visibility = View.VISIBLE
                binding.tvPassword2Status.text = "비밀번호가 일치하지 않습니다."
                binding.tvPassword2Status.setTextColor(Color.parseColor("#EF4444")) // 빨간색
            }
        }
    }

    // checkValidation 함수 교체 (단순 글자 수 검사가 아닌 서버 승인 여부로 판별)
    private fun checkValidation() {
        val password = binding.etSignupPassword.text.toString().trim()
        val password2 = binding.etSignupPassword2.text.toString().trim()

        val isPasswordComplexValid = password.length >= 8 &&
                password.contains(Regex("[a-zA-Z]")) &&
                password.contains(Regex("[0-9]")) &&
                password.contains(Regex("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]"))

        val isPasswordValid = isPasswordComplexValid && password == password2

        (activity as? SignupActivity)?.setNextButtonState(isIdAvailable && isPasswordValid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginBottomSheetFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SignupStep2Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}