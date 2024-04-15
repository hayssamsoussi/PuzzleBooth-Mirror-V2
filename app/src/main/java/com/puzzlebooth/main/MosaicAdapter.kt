package com.puzzlebooth.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.puzzlebooth.main.models.MosaicBox
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.ListItemDesignBinding
import com.puzzlebooth.server.databinding.ListItemMosaicBinding
import com.puzzlebooth.server.network.Design
import java.io.File

data class MosaicItem(
    val position: Int,
    val file: File,
    val original: Boolean
)

class MosaicAdapter (val list: List<MosaicItem>, val clicked: (MosaicItem) -> Unit) : RecyclerView.Adapter<MosaicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListItemMosaicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(private val itemBinding: ListItemMosaicBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindItems(item: MosaicItem) {
            Glide.with(itemBinding.root.context)
                .load(item.file)
                .into(itemBinding.ivMosaic)

            itemBinding.tvPosition.text = item.position.toString()
            itemBinding.ivMosaic.alpha = if(item.original) 0.2F else 0.8F
            itemBinding.root.setOnClickListener {
                clicked.invoke(item)
            }
            //Gli.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)
//            val fileName = if(item.isLocal) {
//                "[LOCAL] ${item.filename}"
//            } else {
//                item.filename
//            }
//
//            itemBinding.tvDesign.text = fileName
//            //Picasso.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)
//
//            itemBinding.root.setOnClickListener {
//                clicked.invoke(item)
//            }
        }
    }
}

class RemoteMosaicAdapter (val list: List<MosaicBox>, val clicked: (Pair<MosaicBox, View>) -> Unit) : RecyclerView.Adapter<RemoteMosaicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListItemMosaicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(private val itemBinding: ListItemMosaicBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindItems(item: MosaicBox) {
            itemBinding.tvPosition.text = item.boxNumber.toString()
            itemBinding.tvPosition.alpha = if(item.imageExist) 1.0F else 0.3F
            //Glide.with(itemBinding.root.context).load(itemBinding.root.context.getDrawable(R.drawable.logo)).into(itemBinding.ivMosaic)
//            itemBinding.tvPosition.text = item.position.toString()
//            itemBinding.ivMosaic.alpha = if(item.original) 0.2F else 0.8F
            itemBinding.root.setOnClickListener {
                clicked.invoke(Pair(item, it))
            }
            //Gli.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)
//            val fileName = if(item.isLocal) {
//                "[LOCAL] ${item.filename}"
//            } else {
//                item.filename
//            }
//
//            itemBinding.tvDesign.text = fileName
//            //Picasso.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)
//
//            itemBinding.root.setOnClickListener {
//                clicked.invoke(item)
//            }
        }
    }
}