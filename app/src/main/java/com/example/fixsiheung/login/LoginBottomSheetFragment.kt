package com.example.fixsiheung.login

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.example.fixsiheung.MainMapActivity
import com.example.fixsiheung.databinding.ActivityLoginBottomBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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


        binding.etId.addTextChangedListener {
            checkInputs()
        }

        binding.etPassword.addTextChangedListener {
            checkInputs()
        }
        // 바텀 시트 내부의 로그인 버튼 클릭 이벤트 처리
        binding.btnBottomSheetLogin.setOnClickListener {
            //val email = binding.etEmail.text.toString()
            // 백엔드 로그인 API 호출 로직 연결부
            val intent = Intent(requireContext(), MainMapActivity::class.java)
            startActivity(intent)
            activity?.finish()
            dismiss()
        }
    }


    // 아이디, 비입력 체크
    private fun checkInputs() {
        val id = binding.etId.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val isBothFilled = id.isNotEmpty() && password.isNotEmpty()

        setButtonState(isBothFilled)
    }

    // 버튼 상태 변경
    private fun setButtonState(isEnable: Boolean) {
        binding.btnBottomSheetLogin.isEnabled = isEnable

        if (isEnable) {
            // 활성화 상태: 버튼 보라색, 글자 흰색
            binding.btnBottomSheetLogin.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#8B00FF"))
            binding.btnBottomSheetLogin.setTextColor(Color.WHITE)
        } else {
            // 비활성화 상태: 버튼 연회색, 글자 어두운 회색
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