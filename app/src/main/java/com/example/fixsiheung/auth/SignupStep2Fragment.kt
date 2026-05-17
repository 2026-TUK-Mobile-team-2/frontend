package com.example.fixsiheung.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.etSignupEmail.setText(viewModel.email.value)
        binding.etSignupPassword.setText(viewModel.password.value)
        checkValidation()
        validateEmail(binding.etSignupEmail.text.toString().trim())
        validatePasswordMatch(binding.etSignupPassword.text.toString().trim(), binding.etSignupPassword2.text.toString().trim())

        binding.etSignupEmail.addTextChangedListener {
            val email = it.toString().trim()
            viewModel.email.value = email
            validateEmail(email)
            checkValidation()
        }
        binding.etSignupPassword.addTextChangedListener {
            val password = it.toString().trim()
            viewModel.password.value = password

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
    private fun validateEmail(email: String) {
        when {
            email.isEmpty() -> {
                binding.tvEmailStatus.visibility = View.GONE
                binding.layoutSignupEmail.endIconDrawable = null
            }
            // 안드로이드 내장 시스템 이메일 정규식 패턴 검사
            Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "사용 가능한 이메일입니다."
                binding.tvEmailStatus.setTextColor(Color.parseColor("#10B981")) // 초록색

                binding.layoutSignupEmail.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                binding.layoutSignupEmail.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#10B981")))
            }
            else -> {
                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "올바른 이메일 형식이 아닙니다."
                binding.tvEmailStatus.setTextColor(Color.parseColor("#EF4444")) // 빨간색
                binding.layoutSignupEmail.endIconDrawable = null
            }
        }
    }

    private fun validatePasswordMatch(password: String, password2: String) {
        when {
            password2.isEmpty() -> {
                binding.tvPasswordStatus.visibility = View.GONE
            }
            // 두 비밀번호가 똑같을때
            password == password2 -> {
                binding.tvPasswordStatus.visibility = View.VISIBLE
                binding.tvPasswordStatus.text = "비밀번호가 일치합니다."
                binding.tvPasswordStatus.setTextColor(Color.parseColor("#10B981")) // 초록색
            }
            password != password2 -> {
                binding.tvPasswordStatus.visibility = View.VISIBLE
                binding.tvPasswordStatus.text = "비밀번호가 일치하지 않습니다."
                binding.tvPasswordStatus.setTextColor(Color.parseColor("#EF4444")) // 빨간색
            }
        }
    }

    private fun checkValidation() {
        val email = binding.etSignupEmail.text.toString().trim()
        val password = binding.etSignupPassword.text.toString().trim()
        val password2 = binding.etSignupPassword2.text.toString().trim()

        val isEmailValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = password.isNotEmpty() && password == password2
        (activity as? SignupActivity)?.setNextButtonState(isEmailValid && isPasswordValid)
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
            LoginBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}