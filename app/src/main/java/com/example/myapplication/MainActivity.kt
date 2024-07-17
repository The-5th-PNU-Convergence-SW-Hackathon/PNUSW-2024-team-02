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

class MainActivity : AppCompatActivity() {
    lateinit var email_input: EditText
    lateinit var password_input: EditText
    lateinit var login_btn: Button
    lateinit var finder_btn: TextView
    lateinit var signup_btn: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            Log.i("Test Credentials", "Email : $email and Password : $password")
            signIn(email, password)
        }
        signup_btn.setOnClickListener {
            val email = email_input.text.toString()
            val password = password_input.text.toString()
            Log.i("Test Credentials", "Email : $email and Password : $password")
            signUp(email, password)
        }
    }

    fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase DB에 저장되어 있는 계정이 아닐 경우
                    // 입력한 계정을 새로 등록한다
                    task.result?.user?.let { goToMainActivity(it) }
                } else if (!task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                } else {
                    // 입력한 계정 정보가 이미 Firebase DB에 있는 경우
                    signIn(email, password)
                }
            }
    }

    // 회원가입 및 로그인 성공 시 메인 화면으로 이동하는 함수
    fun goToMainActivity(user: FirebaseUser) {
        // Firebase에 등록된 계정일 경우에만 메인 화면으로 이동
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
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
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
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
