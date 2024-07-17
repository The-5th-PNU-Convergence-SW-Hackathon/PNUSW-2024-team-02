package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    lateinit var email_input: EditText
    lateinit var password_input: EditText
    lateinit var login_btn: Button
    lateinit var finder_btn: TextView
    lateinit var signup_btn: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        email_input = findViewById(R.id.email_input)
        password_input = findViewById(R.id.password_input)
        login_btn = findViewById(R.id.login_btn)
        finder_btn = findViewById(R.id.finder_btn)
        signup_btn = findViewById(R.id.signup_btn)

        login_btn.setOnClickListener {
            val email = email_input.text.toString()
            val password = password_input.text.toString()
            Log.i("Test Credentials", "Email : $email and Password : $password")

        }
    }

}