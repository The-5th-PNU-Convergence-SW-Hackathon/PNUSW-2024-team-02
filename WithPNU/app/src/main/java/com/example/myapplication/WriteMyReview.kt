package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class WriteMyReview : AppCompatActivity() {
    // Firebase Firestore 인스턴스 생성
    private val db = FirebaseFirestore.getInstance()

    // UI 요소 선언
    private lateinit var addLocationButton: TextView
    private lateinit var contentEditText: EditText
    private lateinit var submitBtn: ImageButton
    private lateinit var ratingBar : RatingBar

    // 위치 및 가게 이름 관련 변수 선언
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var storeName: String? = null
    private var rating: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_review)

        // UI 요소 초기화
        addLocationButton = findViewById(R.id.addLocationButton)
        contentEditText = findViewById(R.id.contentEditText)
        submitBtn = findViewById(R.id.submitBtn)

        // Intent에서 데이터 수신
        storeName = intent.getStringExtra("storeName")
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        ratingBar = findViewById(R.id.ratingBar)


        // 내용 입력 처리

        contentEditText.setOnClickListener {
            contentEditText.requestFocus()
        }

        // 위치 선택 버튼 클릭 시 지도 액티비티 열기
        addLocationButton.setOnClickListener {
            openMapActivity()
        }
        // 수신한 데이터로 addLocationButton 초기화
        if (storeName != null && latitude != null && longitude != null) {
            addLocationButton.text = "$storeName"
        }

        // RatingBar가 변경될 때마다 TextView에 반영
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            this.rating = rating
        }

        // 제출 버튼 클릭 시 리뷰를 데이터베이스에 저장
        submitBtn.setOnClickListener {
            saveReviewToDatabase()
        }

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "내 리뷰 작성하기" // 제목 달기

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

    // 지도 액티비티에서 위치 결과를 받아옴
    private val mapActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val placeName = result.data?.getStringExtra("place_name")
            val lat = result.data?.getDoubleExtra("latitude", 0.0)
            val lng = result.data?.getDoubleExtra("longitude", 0.0)
            if (lat != null && lng != null) {
                latitude = lat
                longitude = lng
                storeName = placeName // 가게 이름 저장
            }
            addLocationButton.text = placeName ?: "위치 선택됨"
        }
    }

    // 지도 액티비티 열기
    private fun openMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        mapActivityResultLauncher.launch(intent)
    }

    // 리뷰를 데이터베이스에 저장하는 함수
    private fun saveReviewToDatabase() {
        val content = contentEditText.text.toString()

        // 내용이 비어 있는지 확인
        if (content.isEmpty()) {
            // 비어 있을 경우 처리
            Toast.makeText(this, "리뷰를 적어주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 리뷰 데이터 생성
        val reviewData = mapOf(
            "content" to content,
            "storeName" to storeName,
            "latitude" to latitude,
            "longitude" to longitude,
            "rating" to rating,
            "postedDate" to Timestamp.now()
        )
        // Firebase Firestore에 데이터 업로드
        db.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                Toast.makeText(this, "리뷰가 성공적으로 업로드되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "리뷰 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
