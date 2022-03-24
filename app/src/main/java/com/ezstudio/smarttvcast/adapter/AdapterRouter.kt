package com.ezstudio.smarttvcast.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.databinding.LayoutItemRouterBinding
import com.ezstudio.smarttvcast.model.ItemRouter
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter

class AdapterRouter(var context: Context, list: MutableList<ItemRouter>) :
    BaseRecyclerAdapter<ItemRouter, AdapterRouter.ViewHolder>(context, list) {

    var onClickItem: ((Int) -> Unit)? = null

    inner class ViewHolder(var binding: LayoutItemRouterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(data: ItemRouter) {
            binding.name.text = data.name
            binding.description.text = data.description
            Glide.with(context).load(data.redId).into(binding.icRouter)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(list[position])
        holder.itemView.setOnClickListener {
            holder.binding.icNext.visibility = View.INVISIBLE
            holder.binding.casting.visibility = View.VISIBLE
            onClickItem?.invoke(holder.adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutItemRouterBinding.inflate(layoutInflater, parent, false))
    }

}