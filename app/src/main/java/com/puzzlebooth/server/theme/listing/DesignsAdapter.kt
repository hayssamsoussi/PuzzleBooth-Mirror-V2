package com.puzzlebooth.server.theme.listing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puzzlebooth.server.databinding.ListItemDesignBinding
import com.puzzlebooth.server.network.Design

class DesignsAdapter (val list: List<Design>, val clicked: (Design) -> Unit) : RecyclerView.Adapter<DesignsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListItemDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(private val itemBinding: ListItemDesignBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindItems(item: Design) {
            val fileName = if(item.isLocal) {
                "[LOCAL] ${item.filename}"
            } else {
                item.filename
            }

            itemBinding.tvDesign.text = fileName
            //Picasso.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)

            itemBinding.root.setOnClickListener {
                clicked.invoke(item)
            }
        }
    }
}