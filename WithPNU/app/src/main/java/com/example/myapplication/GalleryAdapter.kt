package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(
    private val photoList: MutableList<Uri>
) : RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.photoImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUri = photoList[position]
        Glide.with(holder.photoImageView.context)
            .load(photoUri)
            .into(holder.photoImageView)

        // 사진 클릭 시 제거하는 기능 추가
        holder.photoImageView.setOnClickListener {
            removePhotoAt(position)
        }
    }

    override fun getItemCount(): Int = photoList.size

    // 사진 제거 메서드
    private fun removePhotoAt(position: Int) {
        if (position >= 0 && position < photoList.size) {
            photoList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
    }
}
