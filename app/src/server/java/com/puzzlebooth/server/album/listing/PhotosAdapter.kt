package com.puzzlebooth.server.album.listing

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.puzzlebooth.server.R

class PhotosAdapter(private val context: Context, private val photos: List<LocalImage>) : BaseAdapter() {

    private var selectedPosition = -1

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
    override fun getCount(): Int = photos.size

    override fun getItem(position: Int): Any = photos[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView

        if (convertView == null) {
            imageView = ImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(400, 600)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView = convertView as ImageView
        }

        Glide.with(context)
            .load(photos[position].file.path)
            .into(imageView)

        // Highlight the selected item
        if (position == selectedPosition) {
            imageView.alpha = 1.0F
            imageView.setBackgroundResource(R.drawable.ic_launcher_background)
        } else {
            imageView.alpha = 0.5F
            imageView.background = null
        }

        return imageView
    }
}