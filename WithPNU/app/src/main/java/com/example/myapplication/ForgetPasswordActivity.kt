package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import java.net.PasswordAuthentication

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var email_input: EditText
    private lateinit var search: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // 레이아웃에서 뷰 연결
        email_input = findViewById(R.id.email_input)
        search = findViewById(R.id.search)

        // 버튼 클릭 리스너 설정
        search.setOnClickListener {
            val email = email_input.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "비밀번호 재설정 이메일이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(this, "이메일 전송에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    navigateToLogin() // 실패해도 로그인 페이지로 이동
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // ForgetPasswordActivity를 종료하여 뒤로 가기 버튼을 눌렀을 때 다시 돌아오지 않도록 함
    }
}