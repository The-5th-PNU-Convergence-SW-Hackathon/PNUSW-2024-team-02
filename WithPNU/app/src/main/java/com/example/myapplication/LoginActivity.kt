package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    lateinit var email_input: EditText
    lateinit var password_input: EditText
    lateinit var login_btn: Button
    lateinit var finder_btn: TextView
    lateinit var signup_btn: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        email_input = findViewById(R.id.email_input)
        password_input = findViewById(R.id.password_input)
        login_btn = findViewById(R.id.login_btn)
        finder_btn = findViewById(R.id.finder_btn)
        signup_btn = findViewById(R.id.signup_btn)

        login_btn.setOnClickListener {
            val email = email_input.text.toString()
            val password = password_input.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "정보를 모두 입력해주세요", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(this, "비밀번호는 8자리 이상어어야 합니다", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Log.i("Test Credentials", "Email : $email and Password : $password")
            signIn(email, password)
        }
        signup_btn.setOnClickListener {
//            val email = email_input.text.toString()
//            val password = password_input.text.toString()
//            if (email.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "정보를 모두 입력해주세요", Toast.LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//            Log.i("Test Credentials", "Email : $email and Password : $password")
//            signUp(email, password)

            startActivity(Intent(this, SignUp::class.java))
        }
        finder_btn.setOnClickListener {
            //비밀번호 찾기를 누를 경우 페이지 이동
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }

    // 회원가입 및 로그인 성공 시 메인 화면으로 이동하는 함수
    fun goToMainActivity(user: FirebaseUser) {
        // Firebase에 등록된 계정일 경우에만 메인 화면으로 이동
        if (user != null) {
            startActivity(Intent(this, MainNavigationbar::class.java))
        }
    }

    // 로그인 함수
    fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인에 성공한 경우 메인 화면으로 이동
                    task.result?.user?.let { goToMainActivity(it) }
                } else {
                    // 로그인에 실패한 경우 Toast 메시지로 에러를 띄워준다
                    Toast.makeText(this, "로그인 정보가 틀렸습니다.", Toast.LENGTH_LONG).show()
                }
            }
    }

    // 로그아웃 함수
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    // uid 값을 가져오는 함수
    fun getUid(): String {
        return auth.currentUser?.uid.toString()
    }
}