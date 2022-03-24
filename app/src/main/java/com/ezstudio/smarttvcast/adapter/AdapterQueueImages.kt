package com.ezstudio.smarttvcast.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutItemQueueImagesBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemRouterBinding
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter

class AdapterQueueImages(var context: Context, list: MutableList<ImageModel>, var db: AppDatabase) :
    BaseRecyclerAdapter<ImageModel, AdapterQueueImages.ViewHolder>(context, list) {

    var listenerClickItem: ((Int) -> Unit)? = null

    inner class ViewHolder(var binding: LayoutItemQueueImagesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(data: ImageModel) {
            Glide.with(context).load(data.path).into(binding.icImage)
            binding.bgSelected.isVisible = data.isSelected
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(list[position])
        holder.itemView.setOnClickListener {
            listenerClickItem?.invoke(holder.position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutItemQueueImagesBinding.inflate(layoutInflater, parent, false))
    }
}