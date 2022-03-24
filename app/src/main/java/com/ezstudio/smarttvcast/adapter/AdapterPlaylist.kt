package com.ezstudio.smarttvcast.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.databinding.LayoutItemAdsBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemPlaylistBinding
import com.ezstudio.smarttvcast.databinding.LayoutItemPlaylistQueueBinding
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter

class AdapterPlaylist(
    var context: Context,
    list: MutableList<PlayListModel>,
    var isQueue: Boolean = false
) :
    BaseRecyclerAdapter<PlayListModel, RecyclerView.ViewHolder>(context, list) {

    var listenerOnClickItem: ((Int) -> Unit)? = null
    var listenerOnClickItemQueue: ((Int) -> Unit)? = null
    var listenerDelete: ((Int) -> Unit)? = null
    var listenerRename: ((Int) -> Unit)? = null

    inner class ViewHolder(var binding: LayoutItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bindData(data: PlayListModel) {
            binding.nameFile.text = data.name
            binding.txtNumberItem.text = "${data.listMusic.size} items"
        }
    }

    inner class ViewHolderQueue(var binding: LayoutItemPlaylistQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bindData(data: PlayListModel) {
            binding.nameFile.text = data.name
            binding.txtNumberItem.text = "${data.listMusic.size} items"
        }
    }

    inner class ViewHolderAds(var bindingAds: LayoutItemAdsBinding) :
        RecyclerView.ViewHolder(bindingAds.root) {
        fun bindData(data: PlayListModel) {
            data.ads?.let {
                if (it.parent != null) {
                    (it.parent as ViewGroup).removeView(it)
                }
                bindingAds.adsView.addView(it)
            }
        }
    }


    //inner class view Ads

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = list[position]
        holder.apply {
            when (holder) {
                is ViewHolder -> {
                    holder.bindData(data)
                    holder.itemView.setOnClickListener {
                        listenerOnClickItem?.invoke(holder.position)
                    }
                    holder.binding.icMore.setOnClickListener {
                        showPopupMenu(
                            initPopupMenu(
                                holder.binding.icMore,
                                holder.adapterPosition,
                            )
                        )
                    }
                }
                is ViewHolderQueue -> {
                    holder.bindData(data)
                    holder.itemView.setOnClickListener {
                        listenerOnClickItemQueue?.invoke(holder.position)
                    }
                }
                is ViewHolderAds -> {
                    holder.bindData(data)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                if (isQueue) {
                    ViewHolderQueue(
                        LayoutItemPlaylistQueueBinding.inflate(
                            layoutInflater,
                            parent,
                            false
                        )
                    )
                } else {
                    ViewHolder(LayoutItemPlaylistBinding.inflate(layoutInflater, parent, false))
                }
            }
            else -> {
                ViewHolderAds(LayoutItemAdsBinding.inflate(layoutInflater, parent, false))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val data = list[position]
        return if (data.ads == null) 1 else 0
    }

    private fun initPopupMenu(
        view: View,
        position: Int
    ): PopupMenu {
        val popMenu = PopupMenu(context, view)
        popMenu.apply {
            inflate(R.menu.menu_folder_playlist)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        listenerDelete?.invoke(position)
                    }
                    R.id.rename -> {
                        listenerRename?.invoke(position)
                    }
                }
                true
            }
        }
        val itemSetAs: Menu = popMenu.menu
        val headerTitle1 = SpannableString(itemSetAs.findItem(R.id.rename).title)
        val headerTitle2 = SpannableString(itemSetAs.findItem(R.id.delete).title)
        // Change the color:
        headerTitle1.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle1.length, 0)
        headerTitle2.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle2.length, 0)
        //set title new color
        itemSetAs.getItem(0).title = headerTitle1
        itemSetAs.getItem(1).title = headerTitle2

        return popMenu
    }

    private fun showPopupMenu(popMenu: PopupMenu) {
        try {
            val pop = PopupMenu::class.java.getDeclaredField("mPopup")
            pop.isAccessible = true
            val menu = pop.get(popMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menu, true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            popMenu.show()
        }
    }


}