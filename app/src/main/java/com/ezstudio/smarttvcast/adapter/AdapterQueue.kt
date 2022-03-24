package com.ezstudio.smarttvcast.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutItemQueueBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.Utils
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter
import java.text.SimpleDateFormat
import java.util.*

class AdapterQueue<T>(var context: Context, list: MutableList<T>, var db: AppDatabase) :
    BaseRecyclerAdapter<T, AdapterQueue<T>.ViewHolder>(context, list) {

    var listenerDelete: ((Int) -> Unit)? = null
    var listenerClick: ((Int) -> Unit)? = null

    inner class ViewHolder(var binding: LayoutItemQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat")
        fun bindData(data: T) {
            when (data) {
                is VideoModel -> {
                    Glide.with(context).load(data.path).into(binding.icFile)
                    binding.nameFile.text = data.fileName
                    binding.txtDuration.text = Utils.formatDurationLong(data.duration)
                    binding.txtDate.text = SimpleDateFormat("dd/MM/yyyy").format(Date(data.created))
                    if (data.isSelected) {
                        binding.layout.setBackgroundColor(Color.parseColor("#73002884"))
                        binding.txtDuration.setTextColor(Color.WHITE)
                        binding.txtDate.setTextColor(Color.WHITE)
                        binding.nameFile.setTextColor(Color.WHITE)
                    } else {
                        val color81 = Color.parseColor("#818181")
                        binding.txtDuration.setTextColor(color81)
                        binding.txtDate.setTextColor(color81)
                        binding.nameFile.setTextColor(Color.parseColor("#222222"))
                        binding.layout.setBackgroundColor(Color.parseColor("#2E002884"))
                    }
                }
                is AudioModel -> {
                    Glide.with(context).load(data.resId).into(binding.icFile)
                    binding.nameFile.text = data.songName
                    binding.txtDuration.text = Utils.formatDurationLong(data.duration)
                    binding.txtDate.text = data.singer
                    if (data.isSelected) {
                        binding.layout.setBackgroundColor(Color.parseColor("#73002884"))
                    } else {
                        binding.layout.setBackgroundColor(Color.parseColor("#2E002884"))
                    }
                }
            }
            binding.icFavorite.isVisible = Utils.isFavorite(data as Any, db)
        }

    }

    override fun onBindViewHolder(holder: AdapterQueue<T>.ViewHolder, position: Int) {
        holder.bindData(list[position])
        holder.binding.icDelete.setOnClickListener {
            listenerDelete?.invoke(holder.adapterPosition)
        }
        holder.itemView.setOnClickListener {
            listenerClick?.invoke(holder.adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterQueue<T>.ViewHolder {
        return ViewHolder(
            LayoutItemQueueBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

} 