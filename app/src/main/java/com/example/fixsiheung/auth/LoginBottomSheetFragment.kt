package com.example.fixsiheung.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.databinding.ActivityLoginBottomBinding
import com.example.fixsiheung.model.LoginRequest
import com.example.fixsiheung.model.LoginResponse
import com.example.fixsiheung.network.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.jvm.java

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginBottomSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: ActivityLoginBottomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ActivityLoginBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, tastes: Bundle?) {
        super.onViewCreated(view, tastes)
        setButtonState(false)

        // 회원가입
        binding.tvBottomSignup.setOnClickListener {
            val intent = Intent(requireContext(), SignupActivity::class.java)
            startActivity(intent)
        }

        // id 입력 확인
        binding.etEmail.addTextChangedListener {
            checkInputs()
        }

        // 비밀번호 입력 확인
        binding.etPassword.addTextChangedListener {
            checkInputs()
        }

        binding.btnBottomSheetLogin.setOnClickListener {
            val userId = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            val loginRequest = LoginRequest(userId = userId, password = password)

            RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        // 로그인 성공! (200 OK)
                        val userName = response.body()?.name ?: "시민"
                        Toast.makeText(requireContext(), "${userName}님 환영합니다!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), MainMapActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                        dismiss()
                    } else {
                        // 아이디나 비밀번호가 틀렸을 때 (401 에러)
                        Toast.makeText(requireContext(), "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 네트워크 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("API_TEST", "로그인 통신 실패: ${t.message}")
                }
            })
        }
    }


    // 아이디, 비밀번호 입력 체크
    private fun checkInputs() {
        val id = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val isBothFilled = id.isNotEmpty() && password.isNotEmpty()

        setButtonState(isBothFilled)
    }

    // 버튼 상태 변경
    private fun setButtonState(isEnable: Boolean) {
        binding.btnBottomSheetLogin.isEnabled = isEnable

        if (isEnable) {
            // 활성화 상태
            binding.btnBottomSheetLogin.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#00696B"))
            binding.btnBottomSheetLogin.setTextColor(Color.WHITE)
        } else {
            // 비활성화 상태
            binding.btnBottomSheetLogin.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            binding.btnBottomSheetLogin.setTextColor(Color.parseColor("#8E94A0"))
        }
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