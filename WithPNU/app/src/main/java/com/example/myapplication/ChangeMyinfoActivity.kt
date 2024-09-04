package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale


class ChangeMyinfoActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var changeNickname: EditText
    private lateinit var reviseButton: ImageButton

    // 새로 추가된 변수: 스피너 관련
    private lateinit var spinnerColleges: Spinner
    private lateinit var spinnerDepartments: Spinner

    // 스피너 데이터 맵
    private val departmentsMap = mapOf(
        "전체" to arrayOf("전체"),
        "인문대학" to arrayOf("국어국문학과", "일어일문학과", "불어불문학과", "노어노문학과", "중어중문학과", "영어영문학과", "독어독문학과", "한문학과", "언어정보학과", "사학과", "철학과", "고고학과"),
        "사회과학대학" to arrayOf("행정학과", "정치외교학과", "사회복지학과", "사회학과", "심리학과", "문헌정보학과", "미디어커뮤니케이션학과"),
        "자연과학대학" to arrayOf("수학과", "통계학과", "물리학과", "화학과", "생명과학과", "미생물학과", "분자생물학과", "지질환경과학과", "대기환경과학과", "해양학과"),
        "공과대학" to arrayOf("기계공학부", "고분자공학과", "유기소재시스템공학과", "화공생명·환경공학부 - 화공생명공학전공", "화공생명·환경공학부 - 환경공학전공", "전기전자공학부 - 전자공학전공", "전기전자공학부 - 전기공학전공", "전기전자공학부 - 반도체공학전공", "조선해양공학과", "재료공학부", "산업공학과", "항공우주공학과", "건축공학과", "건축학과", "도시공학과", "사회기반시스템공학과"),
        "사범대학" to arrayOf("국어교육과", "영어교육과", "독어교육과", "불어교육과", "교육학과", "유아교육과", "특수교육과", "일반사회교육과", "역사교육과", "지리교육과", "윤리교육과", "수학교육과", "물리교육과", "화학교육과", "생물교육과", "지구과학교육과", "체육교육과"),
        "경제통상대학" to arrayOf("무역학부", "경제학부", "관광컨벤션학과", "국제학부", "공공정책학부"),
        "경영대학" to arrayOf("경영학과"),
        "약학대학" to arrayOf("약학대학 - 약학전공", "약학대학 - 제약학전공"),
        "생활과학대학" to arrayOf("아동가족학과", "의류학과", "식품영양학과", "실내환경디자인학과", "스포츠과학과"),
        "예술대학" to arrayOf("음악학과", "한국음악학과", "미술학과", "조형학과", "디자인학과", "무용학과", "예술문화영상학과"),
        "나노과학기술대학" to arrayOf("나노메카트로닉스공학과", "나노에너지공학과", "광메카트로닉스공학과"),
        "간호대학" to arrayOf("간호학과"),
        "의과대학" to arrayOf("의예과", "의학과"),
        "정보의생명공학대학" to arrayOf("정보컴퓨터공학부 - 컴퓨터공학전공", "정보컴퓨터공학부 - 인공지능 전공", "의생명융합공학부")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_myinfo)

        auth = FirebaseAuth.getInstance()
        changeNickname = findViewById(R.id.change_nickname)
        reviseButton = findViewById(R.id.revise_btn)

        // 새로 추가된 스피너 초기화
        spinnerColleges = findViewById(R.id.spinner_colleges)
        spinnerDepartments = findViewById(R.id.spinner_major)

        // revise_btn 클릭 시 Firestore에서 username, college, department 업데이트 및 액티비티 종료
        reviseButton.setOnClickListener {
            val newNickname = changeNickname.text.toString().trim()
            val selectedCollege = spinnerColleges.selectedItem.toString()
            val selectedDepartment = spinnerDepartments.selectedItem.toString()

            if (newNickname.isNotEmpty()) {
                updateUserInfo(newNickname, selectedCollege, selectedDepartment)
            } else {
                Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }


        // 스피너 설정
        setupCollegeSpinners()

        // 현재 사용자 정보 불러오기
        fetchCurrentUserInfo()

        //상단 툴바 추가하기
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 버튼 추가
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "정보 수정하기" //제목 달기

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

    /**
     * Firestore에서 현재 사용자의 정보를 불러와 UI에 설정하는 함수
     */
    private fun fetchCurrentUserInfo() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val userDoc = db.collection("Users").document(userId).get().await()
                    if (userDoc.exists()) {
                        val currentUsername = userDoc.getString("username") ?: ""
                        val currentCollege = userDoc.getString("college") ?: "전체"
                        val currentDepartment = userDoc.getString("department") ?: "전체"

                        // 닉네임 설정
                        changeNickname.setText(currentUsername)

                        // 스피너의 선택을 현재 사용자 정보로 설정
                        setSpinnerSelection(currentCollege, currentDepartment)
                    } else {
                        Toast.makeText(this@ChangeMyinfoActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChangeMyinfoActivity, "사용자 정보 불러오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "사용자 인증 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 스피너의 선택을 설정된 값으로 변경하는 함수
     */
    private fun setSpinnerSelection(college: String, department: String) {
        // 대학 스피너에서 해당 대학을 찾고 선택
        val collegePosition = (spinnerColleges.adapter as ArrayAdapter<String>).getPosition(college)
        if (collegePosition >= 0) {
            spinnerColleges.setSelection(collegePosition)
        }

        // 부서 스피너는 대학 스피너의 onItemSelectedListener에 의해 자동으로 설정됨
        // 부서 스피너의 선택을 해당 부서로 설정
        CoroutineScope(Dispatchers.Main).launch {
            // 약간의 지연을 주어 스피너가 업데이트된 후 선택할 수 있도록 함
            kotlinx.coroutines.delay(500)
            val departmentAdapter = spinnerDepartments.adapter as ArrayAdapter<String>
            val departmentPosition = departmentAdapter.getPosition(department)
            if (departmentPosition >= 0) {
                spinnerDepartments.setSelection(departmentPosition)
            }
        }
    }

    /**
     * 대학과 전공 스피너를 설정하는 함수
     */
    private fun setupCollegeSpinners() {
        val colleges = arrayOf(
            "전체", "인문대학", "사회과학대학", "자연과학대학", "공과대학", "사범대학", "경제통상대학", "경영대학",
            "약학대학", "생활과학대학", "예술대학", "나노과학기술대학", "간호대학", "의과대학", "정보의생명공학대학"
        )

        // 대학 스피너 어댑터 설정
        val collegeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colleges)
        collegeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColleges.adapter = collegeAdapter

        // 대학 스피너 아이템 선택 리스너 설정
        spinnerColleges.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedCollege = colleges[position]
                if (departmentsMap.containsKey(selectedCollege)) {
                    val departments = departmentsMap[selectedCollege]!!
                    val departmentAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, departments)
                    departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerDepartments.adapter = departmentAdapter
                    spinnerDepartments.visibility = View.VISIBLE
                } else {
                    spinnerDepartments.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                spinnerDepartments.visibility = View.GONE
            }
        }
    }

    /**
     * Firestore의 Users 컬렉션에서 username, college, department를 업데이트하는 함수
     */
    private fun updateUserInfo(newUsername: String, newCollege: String, newDepartment: String) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Firestore에서 Users 컬렉션의 해당 사용자 문서를 업데이트
                    db.collection("Users").document(userId)
                        .update(
                            mapOf(
                                "username" to newUsername.lowercase(Locale.getDefault()),
                                "college" to newCollege,
                                "department" to newDepartment
                            )
                        )
                        .await()

                    Toast.makeText(this@ChangeMyinfoActivity, "정보가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()

                    // 업데이트 완료 후 액티비티 종료
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ChangeMyinfoActivity, "정보 변경 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
