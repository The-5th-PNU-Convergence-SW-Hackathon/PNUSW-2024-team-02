package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.text.TextUtils
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUp : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    private lateinit var emailInput: EditText
    private lateinit var password_input: EditText
    private lateinit var password_check: EditText
    private lateinit var signupBtn: Button
    private lateinit var authenticButton: Button
    private lateinit var spinnerColleges: Spinner
    private lateinit var spinnerDepartments: Spinner
    private lateinit var nickname: EditText
    private lateinit var adminCode: EditText

    private lateinit var auth: FirebaseAuth
    private var isEmailVerified = false

    companion object {
        private const val TAG = "SignUp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // 레이아웃들 연결
        emailInput = findViewById(R.id.signup_email_input)
        password_input = findViewById(R.id.password_input)
        password_check = findViewById(R.id.password_check)
        signupBtn = findViewById(R.id.signup_btn)
        authenticButton = findViewById(R.id.authentic_button)
        spinnerColleges = findViewById(R.id.spinner_colleges)
        spinnerDepartments = findViewById(R.id.spinner_departments)
        nickname = findViewById(R.id.nickname)
        adminCode = findViewById(R.id.admin_code)

        // 버튼 클릭 리스너 설정
        authenticButton.setOnClickListener { sendEmailVerification() }
        signupBtn.setOnClickListener { completeSignUp() }

        // 비밀번호 입력 시 유효성 검사
        password_input.addTextChangedListener(createTextWatcher())
        password_check.addTextChangedListener(createTextWatcher())
        emailInput.addTextChangedListener(emailTextWatcher())

        setupCollegeSpinners()

    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePassword()
            }
        }
    }

    private fun emailTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail()
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = emailInput.text.toString()
        return if (email.endsWith("@pusan.ac.kr")) {
            emailInput.error = null
            true
        } else {
            emailInput.error = "@pusan.ac.kr로 끝나는 이메일을 입력해주세요."
            false
        }
    }

    private fun sendEmailVerification() {
        val email = emailInput.text.toString()
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Firebase에서 임시 계정 생성 후 이메일 인증 메일 발송
                auth.createUserWithEmailAndPassword(email, "temporaryPassword").await()
                val user = auth.currentUser
                user?.sendEmailVerification()?.await()
                Log.d(TAG, "Email sent.")
                Toast.makeText(this@SignUp, "인증 이메일이 발송되었습니다. 이메일을 확인해주세요.", Toast.LENGTH_LONG).show()

                // 이메일 인증 완료 여부를 확인하기 위해 checkEmailVerification 호출
                checkEmailVerification()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send email verification: ${e.message}")
                Toast.makeText(this@SignUp, "이메일 발송에 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkEmailVerification() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                while (!isEmailVerified) {
                    val user = auth.currentUser
                    user?.reload()?.await()
                    isEmailVerified = user?.isEmailVerified ?: false
                    if (isEmailVerified) {
                        Log.d(TAG, "Email verification complete.")
                        Toast.makeText(this@SignUp, "이메일 인증이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        break
                    }
                    kotlinx.coroutines.delay(2000) // 2초 간격으로 확인
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during email verification check: ${e.message}")
            }
        }
    }


    private fun completeSignUp() {
        val email = emailInput.text.toString()
        val password = password_input.text.toString()
        val passwordCheck = password_check.text.toString()
        val nickname = nickname.text.toString()
        val adminCode = adminCode.text.toString()
        val college = spinnerColleges.selectedItem.toString()
        val department = spinnerDepartments.selectedItem.toString()


        // 예외 처리
        if (TextUtils.isEmpty(nickname) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }


        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Starting completeSignUp process")
                val user = auth.currentUser
                user?.reload()?.await()

                if (user != null && user.isEmailVerified) {
                    Log.d(TAG, "User is verified, updating password")
                    user.updatePassword(password).await()
                    val role = if (adminCode == "1111") "admin" else "user"
                    saveUserToFirestore(user.uid, nickname, email, role, college, department)

                    // 회원가입 완료 후 로그인 화면으로 이동
                    val intent = Intent(this@SignUp, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Log.d(TAG, "User is not verified or is null")
                    Toast.makeText(this@SignUp, "이메일 인증이 완료되지 않았습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "회원가입 중 문제가 발생했습니다: ${e.message}")
                Toast.makeText(this@SignUp, "회원가입 중 문제가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun saveUserToFirestore(userId: String, name: String, email: String, role: String,college: String, department: String ) {
        val userMap = hashMapOf(
            "id" to userId,
            "username" to name.lowercase(Locale.getDefault()),
            "email" to email,
            "role" to role,
            "college" to college,
            "department" to department
        )

        db.collection("Users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this@SignUp, "가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@SignUp, "회원가입 중 문제가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun validatePassword(): Boolean {
        val password = password_input.text.toString()
        val passwordCheck = password_check.text.toString()

        if (password.length < 8) {
            password_input.error = "비밀번호는 최소 8자리여야 합니다"
            return false
        }

        if (password != passwordCheck) {
            password_check.error = "비밀번호가 일치하지 않습니다"
            return false
        }
        return true
    }


    private fun setupCollegeSpinners() {
        val colleges = arrayOf(
            "전체", "인문대학", "사회과학대학", "자연과학대학", "공과대학", "사범대학", "경제통상대학", "경영대학",
            "약학대학", "생활과학대학", "예술대학", "나노과학기술대학", "간호대학", "의과대학", "정보의생명공학대학"
        )

        val departmentsMap = mapOf(
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

        val collegeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colleges)
        collegeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColleges.adapter = collegeAdapter

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
}


