package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class PartnershipNoticeActivity : AppCompatActivity() {

    // Firebase Firestore 인스턴스 생성
    private val db = FirebaseFirestore.getInstance()

    // Firebase Storage 인스턴스 생성
    private val storage = FirebaseStorage.getInstance()

    // UI 요소 선언
    private lateinit var periodEditText: EditText
    private lateinit var addLocationButton: Button
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var spinnerCategories: Spinner
    private val selectedPhotos = mutableListOf<Uri>()
    private val galleryAdapter = GalleryAdapter(selectedPhotos)

    // 날짜 관련 변수 선언
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    // 위치 및 가게 이름 관련 변수 선언
    private var selectedLatLng: LatLng? = null
    private var storeName: String? = null // 가게 이름을 저장할 변수 추가

    // 선택된 카테고리를 저장할 변수 선언
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partnership_notice)

        // UI 요소 초기화 및 이벤트 리스너 설정
        periodEditText = findViewById(R.id.periodEditText)
        periodEditText.setOnClickListener {
            showDateRangePicker()
        }

        addLocationButton = findViewById(R.id.addLocationButton)
        addLocationButton.setOnClickListener {
            openMapActivity()
        }

        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        photosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photosRecyclerView.adapter = galleryAdapter

        val addPhotoButton: ImageButton = findViewById(R.id.addPhotoButton)
        addPhotoButton.setOnClickListener {
            selectPhotosFromGallery()
        }

        val uploadButton: Button = findViewById(R.id.uploadButton)
        uploadButton.setOnClickListener {
            uploadDataToFirestore()
        }

        setupCategoriesSpinner()

        // 상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = "제휴공지 작성하기" // 제목 달기
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

    private fun setupCategoriesSpinner() {
        spinnerCategories = findViewById(R.id.spinner_categories)
        val categories = arrayOf("전체", "술집", "카페", "문화", "음식점•식품", "헬스•뷰티", "교육", "의료•법")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = adapter

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showDateRangePicker() {
        val today = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            startDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                clearTime()
            }
            DatePickerDialog(this, { _, endYear, endMonth, endDayOfMonth ->
                endDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, endYear)
                    set(Calendar.MONTH, endMonth)
                    set(Calendar.DAY_OF_MONTH, endDayOfMonth)
                    clearTime()
                }
                updatePeriodEditText()
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun updatePeriodEditText() {
        val dateFormat = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
        if (startDate != null && endDate != null) {
            val startDateString = dateFormat.format(startDate!!.time)
            val endDateString = dateFormat.format(endDate!!.time)
            periodEditText.setText("$startDateString ~ $endDateString")
        }
    }

    private val mapActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val placeName = result.data?.getStringExtra("place_name")
            val lat = result.data?.getDoubleExtra("latitude", 0.0)
            val lng = result.data?.getDoubleExtra("longitude", 0.0)
            if (lat != null && lng != null) {
                selectedLatLng = LatLng(lat, lng)
                storeName = placeName // 가게 이름 저장
            }
            addLocationButton.text = placeName
        }
    }

    private fun openMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        mapActivityResultLauncher.launch(intent)
    }

    private fun selectPhotosFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val clipData = result.data?.clipData
            val data = result.data?.data

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedPhotos.add(uri)
                }
            } else if (data != null) {
                selectedPhotos.add(data)
            }
            galleryAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "사진 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Firestore에 데이터를 업로드하는 메서드
    private fun uploadDataToFirestore() {
        val title = findViewById<EditText>(R.id.titleEditText).text.toString()
        val content = findViewById<EditText>(R.id.contentEditText).text.toString()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (title.isNotEmpty() && content.isNotEmpty() && currentUser != null && selectedLatLng != null && startDate != null && endDate != null && selectedCategory != null && storeName != null) {
            uploadPhotosToStorage { photoUrls ->
                val data = hashMapOf(
                    "title" to title,
                    "content" to content,
                    "location" to GeoPoint(selectedLatLng!!.latitude, selectedLatLng!!.longitude),
                    "postedDate" to Timestamp.now(),
                    "uid" to currentUser.uid,
                    "photos" to photoUrls,
                    "startDate" to Timestamp(startDate!!.time),
                    "endDate" to Timestamp(endDate!!.time),
                    "category" to selectedCategory,
                    "storeName" to storeName // 가게 이름 추가
                )

                db.collection("partnershipinfo")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "데이터 업로드 성공", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "데이터 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    // Firebase Storage에 이미지를 업로드하는 메서드
    private fun uploadPhotosToStorage(onComplete: (List<String>) -> Unit) {
        val storageRef = storage.reference
        val photoUrls = mutableListOf<String>()
        var uploadCount = 0

        for (uri in selectedPhotos) {
            val fileRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
            val uploadTask = fileRef.putFile(uri)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                fileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    photoUrls.add(downloadUri.toString())
                    Log.d("Upload", "Download URL: $downloadUri")
                } else {
                    Log.e("Upload", "Failed to get download URL", task.exception)
                }
                uploadCount++
                if (uploadCount == selectedPhotos.size) {
                    Log.d("Upload", "All uploads completed. URLs: $photoUrls")
                    onComplete(photoUrls)
                }
            }.addOnFailureListener { exception ->
                Log.e("Upload", "Upload failed for URI: $uri", exception)
                uploadCount++
                if (uploadCount == selectedPhotos.size) {
                    Log.d("Upload", "Uploads finished with some failures. URLs: $photoUrls")
                    onComplete(photoUrls)
                }
            }
        }
    }
}
