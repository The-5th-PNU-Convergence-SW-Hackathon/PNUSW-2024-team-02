package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class setting : AppCompatActivity() {

    private lateinit var noticeAlarmSwitch: Switch
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private val CHANNEL_ID = "notice_channel"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 상단 툴바 설정
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "설정-알림"

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // 스위치 설정
        noticeAlarmSwitch = findViewById(R.id.notice_alarm_switch)

        // 저장된 스위치 상태를 로드하고 스위치 초기화
        val isNoticeAlarmEnabled = sharedPreferences.getBoolean("notice_alarm_switch", false)
        noticeAlarmSwitch.isChecked = isNoticeAlarmEnabled

        // 스위치 상태에 따라 Firestore 리스너 설정
        if (isNoticeAlarmEnabled) {
            startListeningForNoticeUpdates()  // 스위치가 활성화된 상태이면 리스너 등록
        }

        // 알림 채널 생성 (안드로이드 8.0 이상을 위한 채널 생성)
        createNotificationChannel()

        // 스위치 상태에 따라 Firestore 리스너 설정 및 상태 저장
        noticeAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 스위치 상태를 SharedPreferences에 저장
            val editor = sharedPreferences.edit()
            editor.putBoolean("notice_alarm_switch", isChecked)
            editor.apply()

            if (isChecked) {
                startListeningForNoticeUpdates()
            } else {
                stopListeningForNoticeUpdates()
            }
        }
    }

    private fun createNotificationChannel() {
        // Android 8.0 (Oreo) 이상에서는 알림 채널이 필요함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notice Channel"
            val descriptionText = "알림 채널 설명"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // 시스템에 알림 채널 등록
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startListeningForNoticeUpdates() {
        listenerRegistration = db.collection("noticeinfo")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // 오류가 발생한 경우
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            // 새로 추가된 공지사항이 있을 때 알림을 생성
                            val title = dc.document.getString("title") ?: "새 공지사항"
                            sendNotification(title)
                        }
                        else -> {}
                    }
                }
            }
    }

    private fun stopListeningForNoticeUpdates() {
        // 리스너 제거
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun sendNotification(title: String) {
        // 알림 생성
        val builder = NotificationCompat.Builder(this@setting, CHANNEL_ID) // Context 명시적 전달
            .setSmallIcon(R.mipmap.ic_launcher) // 기본 아이콘 사용
            .setContentTitle("새 공지사항")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // NotificationManager를 사용하여 알림 표시
        val notificationManager = NotificationManagerCompat.from(this@setting) // Context 명시적 전달
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없는 경우 알림을 생성하지 않음
            return
        }
        notificationManager.notify(1001, builder.build())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 액티비티가 파괴될 때 리스너 제거
        stopListeningForNoticeUpdates()
    }
}
