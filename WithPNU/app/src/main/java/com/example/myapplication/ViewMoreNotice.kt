package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class ViewMoreNotice : AppCompatActivity() {

    private lateinit var noticeTitleTxt: TextView
    private lateinit var noticeCouncilTxt: TextView
    private lateinit var noticeContentTxt: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_more_notice)

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "공지사항 자세히 보기" // 제목 설정

        // View 초기화
        noticeTitleTxt = findViewById(R.id.notice_title_txt)
        noticeCouncilTxt = findViewById(R.id.notice_council_txt)
        noticeContentTxt = findViewById(R.id.notice_content_txt)

        // Intent로부터 전달받은 noticeTitle과 userId를 가져오기
        val noticeTitle = intent.getStringExtra("noticeTitle") ?: return
        val userId = intent.getStringExtra("userId") ?: return

        if (noticeTitle.isNotEmpty() && userId.isNotEmpty()) {
            // 공지사항 내용 불러오기
            loadNoticeDetails(noticeTitle, userId)
        } else {
            Toast.makeText(this, "필수 데이터가 전달되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNoticeDetails(noticeTitle: String, userId: String) {
        db.collection("noticeinfo")
            .whereEqualTo("title", noticeTitle)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val document = documents.documents[0]
                    Log.d("ViewMoreNotice", "Document found: ${document.data}")

                    // 공지사항의 제목과 내용을 설정
                    noticeTitleTxt.text = document.getString("title")
                    noticeContentTxt.text = document.getString("content")

                    // 작성자 이름 불러오기
                    loadUsername(userId)
                } else {
                    Log.d("ViewMoreNotice", "No document found for title: $noticeTitle")
                    Toast.makeText(this, "공지사항을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "공지사항을 불러오는 중 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUsername(userId: String) {
        db.collection("Users")
            .whereEqualTo("id", userId)  // "id" 필드와 uid가 일치하는지 확인
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val document = documents.documents[0]
                    Log.d("ViewMoreNotice", "User found: ${document.data}")

                    // 작성자의 이름을 설정
                    noticeCouncilTxt.text = document.getString("username")
                } else {
                    Log.d("ViewMoreNotice", "No user found for ID: $userId")
                    Toast.makeText(this, "작성자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "작성자 정보를 불러오는 중 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 뒤로가기 버튼 클릭 시 액티비티 종료
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
