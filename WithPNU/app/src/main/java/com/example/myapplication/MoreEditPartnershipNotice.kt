package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

class MoreEditPartnershipNotice : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var periodEditText: EditText
    private lateinit var addLocationButton: Button
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var spinnerCategories: Spinner
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var uploadButton: Button
    private lateinit var addPhotoButton: ImageButton

    private val selectedPhotos = mutableListOf<Uri>()
    private val galleryAdapter = GalleryAdapter(selectedPhotos)

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var selectedLatLng: LatLng? = null
    private var storeName: String? = null
    private var selectedCategory: String? = null
    private var documentId: String? = null
    private var existingPhotoUrls = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_more_edit_partnership_notice)

        periodEditText = findViewById(R.id.periodEditText)
        addLocationButton = findViewById(R.id.addLocationButton)
        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        spinnerCategories = findViewById(R.id.spinner_categories)
        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        uploadButton = findViewById(R.id.uploadButton)
        addPhotoButton = findViewById(R.id.addPhotoButton)

        photosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photosRecyclerView.adapter = galleryAdapter

        setupCategoriesSpinner() // 어댑터 설정 후에 인텐트 데이터 로드

        periodEditText.setOnClickListener {
            showDateRangePicker()
        }

        addLocationButton.setOnClickListener {
            openMapActivity()
        }

        addPhotoButton.setOnClickListener {
            selectPhotosFromGallery()
        }

        uploadButton.setOnClickListener {
            if (documentId != null) {
                updateDataInFirestore()
            } else {
                uploadDataToFirestore()
            }
        }
    }

    private fun setupCategoriesSpinner() {
        val categories = arrayOf("전체", "술집", "카페", "문화", "음식점•식품", "헬스•뷰티", "교육", "의료•법")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = adapter

        // 어댑터 설정이 끝난 후 인텐트에서 전달된 데이터를 로드
        loadIntentData()
    }

    private fun loadIntentData() {
        documentId = intent.getStringExtra("documentId")
        titleEditText.setText(intent.getStringExtra("title") ?: "")
        contentEditText.setText(intent.getStringExtra("content") ?: "")

        val startDateMillis = intent.getLongExtra("startDate", 0L) * 1000
        val endDateMillis = intent.getLongExtra("endDate", 0L) * 1000
        startDate = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        endDate = Calendar.getInstance().apply { timeInMillis = endDateMillis }
        updatePeriodEditText()

        val lat = intent.getDoubleExtra("latitude", 0.0)
        val lng = intent.getDoubleExtra("longitude", 0.0)
        if (lat != 0.0 && lng != 0.0) {
            selectedLatLng = LatLng(lat, lng)
            storeName = intent.getStringExtra("storeName")
            addLocationButton.text = storeName
        }

        selectedCategory = intent.getStringExtra("category")
        val adapter = spinnerCategories.adapter as? ArrayAdapter<String>
        adapter?.let {
            val position = it.getPosition(selectedCategory)
            spinnerCategories.setSelection(position)
        } ?: run {
            Log.e("MoreEditPartnershipNotice", "Spinner adapter is null")
        }

        val photoUrls = intent.getStringArrayListExtra("photoUrls") ?: arrayListOf()
        existingPhotoUrls.addAll(photoUrls)
        for (url in photoUrls) {
            selectedPhotos.add(Uri.parse(url))
        }
        galleryAdapter.notifyDataSetChanged()
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

    private fun openMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        mapActivityResultLauncher.launch(intent)
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
                storeName = placeName
            }
            addLocationButton.text = placeName
        }
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

    private fun uploadDataToFirestore() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()
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
                    "storeName" to storeName
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

    private fun updateDataInFirestore() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (title.isNotEmpty() && content.isNotEmpty() && currentUser != null && selectedLatLng != null && startDate != null && endDate != null && selectedCategory != null && storeName != null) {
            uploadPhotosToStorage { photoUrls ->
                val allPhotoUrls = existingPhotoUrls + photoUrls
                val data = hashMapOf(
                    "title" to title,
                    "content" to content,
                    "location" to GeoPoint(selectedLatLng!!.latitude, selectedLatLng!!.longitude),
                    "postedDate" to Timestamp.now(),
                    "uid" to currentUser.uid,
                    "photos" to allPhotoUrls,
                    "startDate" to Timestamp(startDate!!.time),
                    "endDate" to Timestamp(endDate!!.time),
                    "category" to selectedCategory,
                    "storeName" to storeName
                )

                documentId?.let { id ->
                    db.collection("partnershipinfo").document(id)
                        .set(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "데이터 업데이트 성공", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "데이터 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } else {
            Toast.makeText(this, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPhotosToStorage(onComplete: (List<String>) -> Unit) {
        val storageRef = storage.reference
        val photoUrls = mutableListOf<String>()
        var uploadCount = 0

        if (selectedPhotos.isEmpty()) {
            onComplete(existingPhotoUrls)
            return
        }

        for (uri in selectedPhotos) {
            if (uri.toString().startsWith("http")) {
                uploadCount++
                if (uploadCount == selectedPhotos.size) {
                    onComplete(photoUrls)
                }
                continue
            }

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
                }
                uploadCount++
                if (uploadCount == selectedPhotos.size) {
                    onComplete(photoUrls)
                }
            }.addOnFailureListener { exception ->
                uploadCount++
                if (uploadCount == selectedPhotos.size) {
                    onComplete(photoUrls)
                }
            }
        }
    }
}
