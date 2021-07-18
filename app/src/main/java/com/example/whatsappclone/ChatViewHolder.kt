package com.example.whatsappclone

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Inbox, onClick: (name: String, photo: String, id: String) -> Unit) =
        with(itemView) {

            val countTv = findViewById<TextView>(R.id.countTv)
            val timeTv = findViewById<TextView>(R.id.timeTv)
            val subTitleTv = findViewById<TextView>(R.id.subTitleTv)
            val titleTv = findViewById<TextView>(R.id.titleTv)
            val userImgView = findViewById<ShapeableImageView>(R.id.userImgView)

            countTv.isVisible = item.count > 0
            countTv.text = item.count.toString()
            timeTv.text = item.time.formatAsListItem(context)

            titleTv.text = item.name
            subTitleTv.text = item.msg
            Picasso.get()
                .load(item.image)
                .placeholder(R.drawable.defaultavatar)
                .error(R.drawable.defaultavatar)
                .into(userImgView)
            setOnClickListener {
                onClick.invoke(item.name, item.image, item.from)
            }
        }
}