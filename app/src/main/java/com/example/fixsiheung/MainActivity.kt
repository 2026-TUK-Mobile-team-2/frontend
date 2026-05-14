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
        RetrofitClient.apiService.getAllReports().enqueue(object : Callback<List<Report>> {

            // 서버 연결 시도 시
            override fun onResponse(call: Call<List<Report>>, response: Response<List<Report>>) {
                if (response.isSuccessful) {
                    // 서버가 200 OK와 함께 데이터를 제대로 줬을 때
                    val reportList = response.body()

                    Log.d("API_TEST", "통신 성공. 받아온 데이터: $reportList")

                    // UI 목록 화면(RecyclerView) 완성 시
                    // 여기에 reportList를 화면에 뿌려주는 코드를 삽입

                } else {
                    Log.e("API_TEST", "서버랑 연락은 됐는데 에러를 뱉음. 코드: ${response.code()}")
                }
            }

            // 2. 인터넷이 끊겼거나 서버가 꺼져서 아예 연락조차 안 될 때
            override fun onFailure(call: Call<List<Report>>, t: Throwable) {
                Log.e("API_TEST", "서버 통신 실패. 원인: ${t.message}")
            }
        })
    }
}