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
import com.ezstudio.smarttvcast.databinding.LayoutItemAudioBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemImageBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemVideoBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.Utils
import com.ezstudio.smarttvcast.utils.Utils.isFavorite
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AdapterLineaFile<T>(
    context: Context,
    list: MutableList<T>,
    var db: AppDatabase,
    var isRecent: Boolean = false,
    var isShowingFragmentFavorite: Boolean = false
) :
    BaseAdapterCast<T, RecyclerView.ViewHolder>(context, list) {
    var onClickVideo: ((Int) -> Unit)? = null
    var onClickAudio: ((Int) -> Unit)? = null
    var onClickImage: ((Int) -> Unit)? = null

    inner class ViewHolderAudio(var binding: LayoutItemAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(data: AudioModel) {
            Glide.with(context).load(data.resId ?: R.drawable.ic_audio).into(binding.icFile)
            binding.nameFile.text = data.songName
            binding.txtAuthor.text = data.singer
            binding.icFavorite.isVisible = isFavorite(data, db)
        }
    }

    inner class ViewHolderVideo(var binding: LayoutItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        fun bindData(data: VideoModel) {
            data.path?.let {
                Glide.with(context)
                    .load(data.path)
                    .into(binding.icFile)
            }
            //
            binding.txtDuration.text = "${Utils.formatDurationLong(data.duration)} ,"
            binding.nameFile.text = data.fileName
            binding.txtDate.text = SimpleDateFormat("dd/MM/yyyy").format(Date(data.created))
            binding.icFavorite.isVisible = isFavorite(data, db)
        }
    }

    inner class ViewHolderImage(var binding: LayoutItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat")
        fun bindData(data: ImageModel) {
            Glide.with(context)
                .load(data.path)
                .into(binding.icFile)
            data.path?.let {
                binding.txtDate.text =
                    SimpleDateFormat("dd/MM/yyyy").format(Date(File(it).lastModified()))
            }
            binding.nameFile.text = data.name
            binding.icFavorite.isVisible = isFavorite(data, db)
        }
    }

    //
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
                is ImageModel -> {
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
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = list[position]
        holder.apply {
            when (holder) {
                is AdapterLineaFile<*>.ViewHolderVideo -> {
                    holder.bindData(data as VideoModel)
                    holder.itemView.setOnClickListener {
                        onClickVideo?.invoke(holder.adapterPosition)
                    }
                    holder.binding.icMore.setOnClickListener {
                        showPopupMenu(
                            initPopupMenu(
                                holder.binding.icMore,
                                holder.adapterPosition,
                                isFavorite(data, db),
                                isRecent, isShowingFragmentFavorite
                            )

                        )
                    }
                }
                is AdapterLineaFile<*>.ViewHolderAudio -> {
                    holder.bindData(data as AudioModel)
                    holder.itemView.setOnClickListener {
                        onClickAudio?.invoke(holder.adapterPosition)
                    }
                    holder.binding.icMore.setOnClickListener {
                        showPopupMenu(
                            initPopupMenu(
                                holder.binding.icMore, holder.adapterPosition,
                                isFavorite(data, db),
                                isRecent, isShowingFragmentFavorite
                            )
                        )
                    }
                }
                is AdapterLineaFile<*>.ViewHolderImage -> {
                    holder.bindData(data as ImageModel)
                    holder.itemView.setOnClickListener {
                        onClickImage?.invoke(holder.adapterPosition)
                    }
                    holder.binding.icMore.setOnClickListener {
                        showPopupMenu(
                            initPopupMenu(
                                holder.binding.icMore, holder.adapterPosition,
                                isFavorite(data, db),
                                isRecent, true, isShowingFragmentFavorite
                            )
                        )
                    }
                }
                is AdapterLineaFile<*>.ViewHolderAds -> {
                    holder.bindData(data)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                ViewHolderVideo(
                    LayoutItemVideoBinding.inflate(LayoutInflater.from(context), parent, false)
                )
            }
            2 -> {
                ViewHolderAudio(
                    LayoutItemAudioBinding.inflate(LayoutInflater.from(context), parent, false)
                )
            }
            3 -> {
                ViewHolderImage(
                    LayoutItemImageBinding.inflate(LayoutInflater.from(context), parent, false)
                )
            }
            else -> {
                ViewHolderAds(
                    LayoutItemAdsBinding.inflate(LayoutInflater.from(context), parent, false)
                )
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
                    2
                } else {
                    0
                }

            }
            is ImageModel -> {
                if (data.ads == null) {
                    3
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