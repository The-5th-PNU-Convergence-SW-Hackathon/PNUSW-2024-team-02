package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class UploadNoticeActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var uploadButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_notice)

        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        uploadButton = findViewById(R.id.uploadButton)

        uploadButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val uid = currentUser?.uid

            // Firestore에 저장
            val noticeData = hashMapOf(
                "title" to title,
                "content" to content,
                "uid" to uid
            )

            db.collection("noticeinfo")
                .add(noticeData)
                .addOnSuccessListener {
                    // 성공 시 현재 액티비티 종료하고 이전 액티비티로 돌아감
                    finish()
                }
                .addOnFailureListener { e ->
                    // 실패 시 처리
                    e.printStackTrace()
                }
        }
        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "공지사항 작성하기" // 제목 달기
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
