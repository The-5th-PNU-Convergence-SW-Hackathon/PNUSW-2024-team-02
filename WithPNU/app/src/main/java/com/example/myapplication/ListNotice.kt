package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class ListNotice : AppCompatActivity() {

    private lateinit var linearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_notice)

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "앱 공지사항"

        // LinearLayout 참조 가져오기
        linearLayout = findViewById(R.id.linearLayout)

        // Firestore에서 데이터 불러오기
        val db = FirebaseFirestore.getInstance()

        db.collection("appinfo")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val title = document.getString("title") ?: "제목 없음"
                    val content = document.getString("content") ?: "내용 없음"

                    // TextView 생성 및 데이터 설정
                    val textView = TextView(this)
                    textView.text = "제목: $title\n내용: $content"
                    textView.setPadding(16, 16, 16, 16)
                    textView.textSize = 16f

                    // TextView를 LinearLayout에 추가
                    linearLayout.addView(textView)
                }
            }
            .addOnFailureListener { exception ->
                // 예외 처리
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
