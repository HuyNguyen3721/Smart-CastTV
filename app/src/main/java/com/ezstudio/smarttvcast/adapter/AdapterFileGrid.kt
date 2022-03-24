package com.ezstudio.smarttvcast.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutItemAdsBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemFileGridBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.Utils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AdapterFileGrid<T>(
    context: Context,
    list: MutableList<T>,
    var db: AppDatabase,
    var isRecent: Boolean = false, var isShowingFragmentFavorite: Boolean = false
) :
    BaseAdapterCast<T, RecyclerView.ViewHolder>(context, list) {
    var listenerOnClickVideo: ((Int) -> Unit)? = null
    var listenerOnClickAudio: ((Int) -> Unit)? = null
    var listenerOnClickImage: ((Int) -> Unit)? = null

    inner class ViewHolder(var binding: LayoutItemFileGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        fun bindData(data: Any) {
            when (data) {
                is VideoModel -> {
                    Glide.with(context)
                        .load(data.path)
                        .into(binding.icFile)
                    //
                    binding.txtDuration.text = "${Utils.formatDurationLong(data.duration)} ,"
                    binding.nameFile.text = data.fileName
                    binding.txtDate.text = SimpleDateFormat("dd/MM/yyyy").format(Date(data.created))
                    binding.icFavorite.isVisible = Utils.isFavorite(data, db)
                }
                is AudioModel -> {
                    Glide.with(context).load(data.resId ?: R.drawable.ic_audio).into(binding.icFile)
                    binding.nameFile.text = data.songName
                    binding.txtDuration.text = data.singer
                    binding.txtDate.text = ""
                    binding.icFavorite.isVisible = Utils.isFavorite(data, db)
                }
                is ImageModel -> {
                    Glide.with(context).load(data.path).into(binding.icFile)
                    data.path?.let {
                        binding.txtDate.text =
                            SimpleDateFormat("dd/MM/yyyy").format(Date(File(it).lastModified()))
                    }
                    binding.nameFile.text = data.name
                    binding.icFavorite.isVisible = Utils.isFavorite(data, db)
                }
            }
        }
    }

    inner class ViewHolderAds(var bindingAds: LayoutItemAdsBinding) :
        RecyclerView.ViewHolder(bindingAds.root) {
        fun <T> bindData(data: T) {
            when (data) {
                is VideoModel -> {
                    data.ads?.let {
                        if (it.parent != null) {
                            (it.parent as ViewGroup).removeView(it)
                        }
                        bindingAds.adsView.addView(it)
                    }
                }
                is AudioModel -> {
                    data.ads?.let {
                        if (it.parent != null) {
                            (it.parent as ViewGroup).removeView(it)
                        }
                        bindingAds.adsView.addView(it)
                    }
                }
                is ImageModel -> {
                    data.ads?.let {
                        if (it.parent != null) {
                            (it.parent as ViewGroup).removeView(it)
                        }
                        bindingAds.adsView.addView(it)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                ViewHolder(
                    LayoutItemFileGridBinding.inflate(LayoutInflater.from(context), parent, false)
                )
            }
            else -> {
                ViewHolderAds(
                    LayoutItemAdsBinding.inflate(LayoutInflater.from(context), parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = list[position]
        holder.apply {
            when (holder) {
                is AdapterFileGrid<*>.ViewHolder -> {
                    when (data) {
                        is VideoModel -> {
                            holder.bindData(data)
                            holder.itemView.setOnClickListener { listenerOnClickVideo?.invoke(holder.adapterPosition) }
                            holder.binding.icMore.setOnClickListener {
                                showPopupMenu(
                                    initPopupMenu(
                                        holder.binding.icMore,
                                        holder.adapterPosition,
                                        Utils.isFavorite(data, db),
                                        isRecent,
                                        isShowingFragmentFavorite
                                    )
                                )
                            }
                        }
                        is AudioModel -> {
                            holder.bindData(data)
                            holder.itemView.setOnClickListener { listenerOnClickAudio?.invoke(holder.adapterPosition) }
                            holder.binding.icMore.setOnClickListener {
                                showPopupMenu(
                                    initPopupMenu(
                                        holder.binding.icMore,
                                        holder.adapterPosition,
                                        Utils.isFavorite(data, db),
                                        isRecent,
                                        isShowingFragmentFavorite
                                    )
                                )
                            }
                        }
                        is ImageModel -> {
                            holder.bindData(data)
                            holder.itemView.setOnClickListener { listenerOnClickImage?.invoke(holder.adapterPosition) }
                            holder.binding.icMore.setOnClickListener {
                                showPopupMenu(
                                    initPopupMenu(
                                        holder.binding.icMore,
                                        holder.adapterPosition,
                                        Utils.isFavorite(data, db),
                                        isRecent, true,
                                        isShowingFragmentFavorite
                                    )
                                )
                            }
                        }
                    }
                }
                is AdapterFileGrid<*>.ViewHolderAds -> {
                    holder.bindData(data)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (val data = list[position]) {
            is VideoModel -> {
                if (data.ads == null) {
                    1
                } else {
                    0
                }
            }
            is AudioModel -> {
                if (data.ads == null) {
                    1
                } else {
                    0
                }

            }
            is ImageModel -> {
                if (data.ads == null) {
                    1
                } else {
                    0
                }
            }
            else -> {
                0
            }
        }
    }

}