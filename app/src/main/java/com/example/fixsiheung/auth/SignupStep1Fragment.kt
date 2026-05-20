package com.example.fixsiheung.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.FragmentSignupStep1Binding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginBottomSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignupStep1Fragment : Fragment() {
    private var _binding: FragmentSignupStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 복원 처리
        binding.etSignupNickname.setText(viewModel.nickname.value)
        checkValidation(binding.etSignupNickname.text.toString().trim())

        // 실시간 입력 감지기 변경
        binding.etSignupNickname.addTextChangedListener {
            val text = it.toString().trim()
            viewModel.nickname.value = text
            checkValidation(text)
        }
    }

    private fun checkValidation(text: String) {
        when {
            // 1. 아무것도 입력하지 않았을 때는 메시지와 아이콘을 모두 숨김
            text.isEmpty() -> {
                binding.tvNicknameStatus.visibility = View.GONE
                binding.layoutSignupNickname.endIconDrawable = null
                (activity as? SignupActivity)?.setNextButtonState(false)
            }

            // 2. 조건 만족 (2자 이상 10자 이하) -> 초록색 성공 UI 세팅
            text.length in 2..10 -> {
                binding.tvNicknameStatus.visibility = View.VISIBLE
                binding.tvNicknameStatus.text = "사용 가능한 닉네임입니다."
                binding.tvNicknameStatus.setTextColor(Color.parseColor("#10B981")) // 에메랄드 초록색

                // 우측에 체크마크 아이콘 주입 및 색상 부여
                binding.layoutSignupNickname.endIconDrawable =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                binding.layoutSignupNickname.setEndIconTintList(
                    ColorStateList.valueOf(
                        Color.parseColor(
                            "#10B981"
                        )
                    )
                )

                (activity as? SignupActivity)?.setNextButtonState(true)
            }

            // 3. 조건 불만족 -> 빨간색 경고 UI 세팅 (체크마크 제거)
            else -> {
                binding.tvNicknameStatus.visibility = View.VISIBLE
                binding.tvNicknameStatus.text = "닉네임은 2자 이상 10자 이하로 입력해주세요."
                binding.tvNicknameStatus.setTextColor(Color.parseColor("#EF4444")) // 경고 빨간색

                // 에러일 때는 우측 아이콘을 지워버립니다.
                binding.layoutSignupNickname.endIconDrawable = null

                (activity as? SignupActivity)?.setNextButtonState(false)
            }
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
        fun newInstance(param1: String, param2: String) = LoginBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
        }
    }
}