package com.example.fixsiheung

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.model.Report
import com.example.fixsiheung.network.RetrofitClient
import com.example.fixsiheung.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 앱이 켜지면 서버에 데이터를 요청하는 함수 실행
        fetchReports()
    }

    // 서버에 제보 목록을 요청하는 함수
    private fun fetchReports() {
        // RetrofitClient를 통해 'getAllReports()' api 호출 (enqueue = 비동기 대기줄에 서기)
        RetrofitClient.apiService.getAllReports().enqueue(object : Callback<List<Report>>
        {

            // 서버 연결 시도 시
            override fun onResponse(call: Call<List<Report>>, response: Response<List<Report>>)
            {
                if (response.isSuccessful) {
                    val reportList = response.body()

                    Log.d("API_TEST", "통신 성공. 받아온 데이터: $reportList")

                    // UI 목록 화면(RecyclerView) 완성 시
                    // 여기에 reportList를 화면에 뿌려주는 코드를 삽입

                } else {
                    Log.e("API_TEST", "서버랑 연락은 됐는데 에러를 뱉음. 코드: ${response.code()}")
                }
            }

            // 2. 인터넷이 끊겼거나 서버가 꺼짐
            override fun onFailure(call: Call<List<Report>>, t: Throwable) {
                Log.e("API_TEST", "서버 통신 실패. 원인: ${t.message}")
            }
        })
    }

    //통신 테스트용 sendReport()
    private fun sendReport() {
        // 1. 방금 DB에 만든 'testuser'와 카테고리 '1'을 참조하는 진짜 제보 객체
        val newReport = Report(
            userId = "testuser",  // 반드시 DB에 있는 유저 ID여야함.
            categoryId = 1,       // 1번: 도로 및 시설물 파손
            title = "정왕동 도로가 파였어요",
            description = "차량 통행이 위험합니다. 빠른 복구 부탁드려요.",
            latitude = 37.345,
            longitude = 126.734,
            address = "경기도 시흥시 정왕동"
        )

        // 2. 서버에 POST 요청
        RetrofitClient.apiService.postReport(newReport).enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.isSuccessful) {
                    Log.d("API_TEST", "진짜 DB에 제보 전송 성공!")
                    // 성공하면 목록을 다시 불러와서 DB에서 잘 꺼내오는지 확인
                    fetchReports()
                } else {
                    Log.e("API_TEST", "전송 실패 (코드: ${response.code()})")
                }
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.e("API_TEST", "네트워크 에러: ${t.message}")
            }
        })
    }
}