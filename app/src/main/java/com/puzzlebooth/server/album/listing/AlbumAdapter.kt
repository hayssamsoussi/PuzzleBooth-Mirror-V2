package com.puzzlebooth.server.album.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.puzzlebooth.server.R
import java.io.File

data class LocalImage(val file: File, val position: Int)

class AlbumAdapter(private val mList: List<LocalImage>, val action: (String) -> Unit) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    companion object {
        var currentPosition = 0
    }

    fun getFileAtPosition(): File? {
        return mList.firstOrNull { it.position == currentPosition }?.file
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_file_item, parent, false)

        return ViewHolder(view)
    }

    fun setSelectedPosition(position: Int) {
        val oldPosition = currentPosition
        currentPosition = position
        notifyItemChanged(oldPosition, Object())
        notifyItemChanged(currentPosition, Object())
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val file = mList[position]

        holder.textView.text = file.file.name
        holder.textView.visibility = View.GONE

        Glide.with(holder.itemView.context)
            .load(file.file.path)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transform(
                com.puzzlebooth.server.utils.RotateTransformation(
                    holder.itemView.context,
                    270f
                )
            )
            .into(holder.imageView)

        if(position == currentPosition) {
            holder.imageNumberTv.visibility = View.VISIBLE
        } else {
            holder.imageNumberTv.visibility = View.GONE
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageNumberTv: TextView = itemView.findViewById(R.id.imageNumber)
    }
}