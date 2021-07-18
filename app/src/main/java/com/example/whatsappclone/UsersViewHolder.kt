package com.example.whatsappclone

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(user: User, onClick:(name: String, photo: String, id: String) -> Unit) =
        with(itemView) {

            val countTv = findViewById<TextView>(R.id.countTv)
            val timeTv = findViewById<TextView>(R.id.timeTv)
            val subTitleTv = findViewById<TextView>(R.id.subTitleTv)
            val titleTv = findViewById<TextView>(R.id.titleTv)
            val userImgView = findViewById<ShapeableImageView>(R.id.userImgView)

            countTv.isVisible = false
            timeTv.isVisible = false

            titleTv.text = user.name
            subTitleTv.text = user.status
            Picasso.get()
                .load(user.thumbImage)
                .placeholder(R.drawable.defaultavatar)
                .error(R.drawable.defaultavatar)
                .into(userImgView)

            setOnClickListener {
                onClick.invoke(user.name, user.thumbImage, user.uid)
            }
        }
}