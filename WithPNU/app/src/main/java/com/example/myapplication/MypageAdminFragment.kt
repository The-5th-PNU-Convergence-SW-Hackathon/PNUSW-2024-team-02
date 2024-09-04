package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import android.net.Uri
import org.w3c.dom.Text

class MypageAdminFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mypage_admin, container, false)

        // 제휴공지 작성하기 버튼 클릭
        val partnershipNoticeButton: TextView = view.findViewById(R.id.partnership_notice_btn)
        partnershipNoticeButton.setOnClickListener {
            val intent = Intent(activity, PartnershipNoticeActivity::class.java)
            startActivity(intent)
        }

        // 제휴공지 관리하기 버튼 클릭
        val editPartnershipNoticeButton: TextView = view.findViewById(R.id.edit_partnership_notice_btn)
        editPartnershipNoticeButton.setOnClickListener{
            val intent = Intent(activity, EditPartnershipNoticeActivity::class.java)
            startActivity(intent)
        }

        // 공지사항 작성하기 버튼 클릭
        val uploadNoticeButton: TextView = view.findViewById(R.id.upload_notice_btn)
        uploadNoticeButton.setOnClickListener {
            //UploadNoticeActivity로 이동
            val intent = Intent(activity, UploadNoticeActivity::class.java)
            startActivity(intent)
        }

        // 공지사항 관리하기 버튼 클릭
        val editNoticeButton: TextView = view.findViewById(R.id.edit_notice_btn)
        editNoticeButton.setOnClickListener {
            //EditNoticeActivity로 이동
            val intent = Intent(activity, EditNoticeActivity::class.java)
            startActivity(intent)
        }


        //설정 버튼 클릭
        val settingButton: TextView = view.findViewById(R.id.settings_btn)
        settingButton.setOnClickListener {
            //setting으로 이동
            val intent = Intent(activity, setting::class.java)
            startActivity(intent)
        }

        //공지사항 버튼 클릭
        val notice: TextView = view.findViewById(R.id.notice_btn)
        notice.setOnClickListener {
            //ListNotice로 이동
            val intent = Intent(activity, ListNotice::class.java)
            startActivity(intent)
        }

        //문의/버그 신고 버튼 클릭
        val bugreport: TextView = view.findViewById(R.id.report_issue_btn)
        bugreport.setOnClickListener {
            // 오픈채팅 링크로 이동
            val url = "https://open.kakao.com/o/sH8uWIMg"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        }

        // 로그아웃 버튼 클릭
        val logoutButton: TextView = view.findViewById(R.id.logout_btn)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val logoutIntent = Intent(activity, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(logoutIntent)
        }

        return view
    }
}