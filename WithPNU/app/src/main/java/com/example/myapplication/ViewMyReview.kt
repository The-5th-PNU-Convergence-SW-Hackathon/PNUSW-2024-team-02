package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem

class ViewMyReview : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view_myreview.xml 레이아웃 설정
        setContentView(R.layout.activity_myreview)

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "내 리뷰 보기" // 제목 달기
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 뒤로가기 버튼 눌렀을 때 액티비티 종료
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
