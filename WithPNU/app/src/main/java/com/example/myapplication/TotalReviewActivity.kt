package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.Toast
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class TotalReviewActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var reviewGridView: GridView
    private var storeName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_total_review)

        reviewGridView = findViewById(R.id.review_gridView)

        // Intent로부터 storeName 가져오기
        storeName = intent.getStringExtra("storeName")

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "리뷰 전체보기" // 제목 달기

        // 리뷰 로드
        loadReviews()
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

    private fun loadReviews() {
        // storeName이 null이 아닌 경우에만 데이터 로드
        storeName?.let { store ->
            db.collection("reviews")
                .whereEqualTo("storeName", storeName)
                .get()
                .addOnSuccessListener { documents ->
                    val reviewContents = mutableListOf<String>()
                    for (document in documents) {
                        val review = document.getString("content") ?: "내용 없음"
                        reviewContents.add(review)
                    }
                    Log.d("TotalReviewActivity", "Fetched reviews: $reviewContents")
                    val adapter = ReviewAdapter(this, reviewContents)
                    reviewGridView.adapter = adapter
                }
                .addOnFailureListener { e ->
                    Log.w("TotalReviewActivity", "Error fetching reviews: ", e)
                }

        }

    }
}

class ReviewAdapter(private val context: Context, private val reviews: List<String>) : BaseAdapter() {

    override fun getCount(): Int {
        return reviews.size
    }

    override fun getItem(position: Int): Any {
        return reviews[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: TextView = convertView as? TextView
            ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false) as TextView

        // 각 항목에 리뷰 텍스트 설정
        view.text = reviews[position]
        return view
    }
}
