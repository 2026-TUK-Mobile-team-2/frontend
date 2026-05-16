package com.example.fixsiheung.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.ActivityLoginBottomBinding
import com.example.fixsiheung.databinding.ActivityMainMapBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
        _binding=ActivityLoginBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, tastes: Bundle?) {
        super.onViewCreated(view, tastes)

        // 바텀 시트 내부의 로그인 버튼 클릭 이벤트 처리
        binding.btnBottomSheetLogin.setOnClickListener {
            //val email = binding.etEmail.text.toString()
            // 백엔드 로그인 API 호출 로직 연결부
            dismiss() // 작업 끝나면 바텀 시트 닫기
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