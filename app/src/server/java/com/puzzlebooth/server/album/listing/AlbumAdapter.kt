package com.puzzlebooth.server.album.listing

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.R
import com.puzzlebooth.server.album.AlbumFragment.Companion.currentPositionSelected
import com.puzzlebooth.server.album.borderColor
import com.puzzlebooth.server.album.borderWidth
import java.io.File

class LocalImage(val file: File, val position: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LocalImage
        return file.path == other.file.path
    }

    override fun hashCode(): Int {
        return file.path.hashCode()
    }
}

class AlbumAdapter(
    private val images: List<LocalImage>,
    private val onImageSelected: (LocalImage) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ImageViewHolder>() {

    private var selectedPhoto: LocalImage? = null

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_photo)
        private val selectedOverlay: View = itemView.findViewById(R.id.selected_overlay)

        fun bind(localImage: LocalImage) {
            Glide.with(itemView.context)
                .load(localImage.file)
                .centerCrop()
                .into(imageView)

            val isSelected = localImage == selectedPhoto
            selectedOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            if (isSelected) {
                imageView.borderColor = ContextCompat.getColor(itemView.context, R.color.selected_border)
                imageView.borderWidth = 4f // Set appropriate border width in dp
            } else {
                imageView.borderColor = ContextCompat.getColor(itemView.context, R.color.transparent)
                imageView.borderWidth = 0f
            }

            itemView.setOnClickListener {
                selectedPhoto = localImage
                onImageSelected(localImage)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_photo, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun setSelectedPhoto(photo: LocalImage?) {
        selectedPhoto = photo
        notifyDataSetChanged()
    }
}