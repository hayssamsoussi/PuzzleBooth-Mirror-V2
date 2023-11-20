package com.puzzlebooth.server.settings.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puzzlebooth.server.databinding.ListItemSettingBinding
import com.puzzlebooth.server.settings.Setting

class SettingsAdapter (val list: List<Setting>, val clicked: (Pair<View, Setting>) -> Unit) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ListItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(private val itemBinding: ListItemSettingBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindItems(item: Setting) {
            itemBinding.tvTitle.text = item.title
            itemBinding.tvValue.text = item.defaultValue
            //Picasso.with(itemBinding.root.context).load(item.url).into(itemBinding.ivDesign)

            itemBinding.root.setOnClickListener {
                clicked.invoke(Pair(it, item))
            }
        }
    }
}