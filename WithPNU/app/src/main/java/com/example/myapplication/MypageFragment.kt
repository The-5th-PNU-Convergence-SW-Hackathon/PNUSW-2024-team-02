package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MypageFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    // Firestore와 FirebaseAuth 인스턴스
    private val db = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // View 요소를 나중에 초기화할 변수
    private lateinit var mypageNickname: TextView
    private lateinit var userMajorInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // View 요소 초기화
        mypageNickname = view.findViewById(R.id.mypage_nickname)
        userMajorInfo = view.findViewById(R.id.userMajorInfo)

        // Firestore에서 사용자 정보 가져오기
        fetchUserInfo()

        // 로그아웃 버튼 클릭
        val logoutButton: TextView = view.findViewById(R.id.logout_btn)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val logoutIntent = Intent(activity, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(logoutIntent)
        }

        // 내 정보 수정하기 버튼 클릭
        val editProfileButton: TextView = view.findViewById(R.id.edit_profile_btn)
        editProfileButton.setOnClickListener {
            val intent = Intent(activity, ChangeMyinfoActivity::class.java)
            startActivity(intent)
        }

        // 내가 작성한 리뷰 보기 버튼 클릭
        val viewReviewButton: TextView = view.findViewById(R.id.view_review_btn)
        viewReviewButton.setOnClickListener {
            val intent = Intent(activity, ViewMyReview::class.java)
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

        return view
    }

    /**
     * Firestore에서 현재 사용자의 정보를 가져와서 화면에 표시하는 함수
     */
    private fun fetchUserInfo() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Firestore에서 사용자 문서를 가져옴
                    val userDoc = db.collection("Users").document(userId).get().await()
                    if (userDoc.exists()) {
                        val username = userDoc.getString("username") ?: "닉네임 없음"
                        val department = userDoc.getString("department") ?: "학과 없음"

                        // 화면에 표시
                        mypageNickname.text = username
                        userMajorInfo.text = department
                    } else {
                        mypageNickname.text = "닉네임 없음"
                        userMajorInfo.text = "학과 없음"
                    }
                } catch (e: Exception) {
                    mypageNickname.text = "오류 발생"
                    userMajorInfo.text = "오류 발생"
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MypageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
