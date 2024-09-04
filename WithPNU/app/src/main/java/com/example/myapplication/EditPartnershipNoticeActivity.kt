package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class EditPartnershipNoticeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var partnershipGridView: GridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_partnership_notice)

        partnershipGridView = findViewById(R.id.partnership_gridView)

        // 현재 사용자가 올린 제휴 공지들을 GridView에 표시
        loadUserPartnerships()

        // GridView의 항목을 클릭하면 MoreEditPartnershipNotice로 이동
        partnershipGridView.setOnItemClickListener { parent, _, position, _ ->
            val selectedDocument = parent.getItemAtPosition(position) as DocumentSnapshot
            openEditPartnershipNoticeActivity(selectedDocument)
        }

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "제휴공지 관리하기" // 제목 달기
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

    private fun loadUserPartnerships() {
        val uid = currentUser?.uid
        if (uid != null) {
            db.collection("partnershipinfo")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { documents ->
                    val filteredDocuments = documents.documents.filter { document ->
                        val startDate = document.getTimestamp("startDate")?.toDate()
                        val endDate = document.getTimestamp("endDate")?.toDate()
                        val today = Date()
                        startDate != null && endDate != null && today in startDate..endDate
                    }

                    if (filteredDocuments.isEmpty()) {
                        partnershipGridView.adapter = null
                    } else {
                        val adapter = PartnershipAdapter(this, filteredDocuments)
                        partnershipGridView.adapter = adapter
                    }
                }
                .addOnFailureListener { exception ->
                    partnershipGridView.adapter = null
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("EditPartnershipNotice", "Current user is null")
        }
    }

    private fun openEditPartnershipNoticeActivity(document: DocumentSnapshot) {
        val intent = Intent(this, MoreEditPartnershipNotice::class.java).apply {
            putExtra("documentId", document.id)
            putExtra("title", document.getString("title") ?: "")
            putExtra("content", document.getString("content") ?: "")
            putExtra("photoUrls", ArrayList(document.get("photos") as? List<String> ?: emptyList()))
            putExtra("startDate", document.getTimestamp("startDate")?.seconds ?: 0L)
            putExtra("endDate", document.getTimestamp("endDate")?.seconds ?: 0L)
            putExtra("category", document.getString("category") ?: "")
            putExtra("storeName", document.getString("storeName") ?: "")

            val location = document.getGeoPoint("location")
            if (location != null) {
                putExtra("latitude", location.latitude)
                putExtra("longitude", location.longitude)
            }
        }
        startActivity(intent)
    }
}
