package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

class PartnershipAdapter(
    private val context: Context,
    private val partnerships: List<DocumentSnapshot>
) : BaseAdapter() {

    override fun getCount(): Int {
        return partnerships.size
    }

    override fun getItem(position: Int): Any {
        return partnerships[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.gridview_detail_partnership, parent, false)

        val partnership = partnerships[position]

        val title = partnership.getString("title") ?: "제목 없음"
        val content = partnership.getString("content") ?: "내용 없음"
        val photos = partnership.get("photos") as? List<String> // photos를 리스트로 처리

        val titleTextView = view.findViewById<TextView>(R.id.partnership_title)
        val contentTextView = view.findViewById<TextView>(R.id.partnership_content)
        val imageView = view.findViewById<ImageView>(R.id.partnership_image)

        titleTextView.text = title
        contentTextView.text = content

        if (!photos.isNullOrEmpty()) {
            // photos 리스트가 존재하고 비어있지 않다면 첫 번째 사진 URL 사용
            val uri = Uri.parse(photos[0])
            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.detail_basic)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.detail_basic) // 기본 이미지
        }

        return view
    }

}
