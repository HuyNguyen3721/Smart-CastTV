package com.ezstudio.smarttvcast.adapter

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.ezstudio.smarttvcast.R
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter

abstract class BaseAdapterCast<T, VH : RecyclerView.ViewHolder>(
    var context: Context,
    list: MutableList<T>
) :
    BaseRecyclerAdapter<T, VH>(context, list) {

    var listenerDelete: ((Int) -> Unit)? = null
    var listenerRemoveRecent: ((Int) -> Unit)? = null
    var listenerCastTo: ((Int) -> Unit)? = null
    var listenerPlayOnPhone: ((Int) -> Unit)? = null
    var listenerAddFavorite: ((Int) -> Unit)? = null
    var listenerRemoveFavorite: ((Int) -> Unit)? = null
    var listenerAddPlaylist: ((Int) -> Unit)? = null

    fun initPopupMenu(
        view: View,
        position: Int,
        isFavorite: Boolean,
        isRecent: Boolean,
        isImage: Boolean = false,
        isShowingFragmentFavorite: Boolean = false
    ): PopupMenu {
        val popMenu = PopupMenu(context, view)
        popMenu.apply {
            inflate(R.menu.menu_file_option)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        listenerDelete?.invoke(position)
                    }
                    R.id.remove_recent -> {
                        listenerRemoveRecent?.invoke(position)
                    }
                    R.id.cast_to -> {
                        listenerCastTo?.invoke(position)
                    }
                    R.id.play_on_phone -> {
                        listenerPlayOnPhone?.invoke(position)
                    }
                    R.id.add_to_favorite -> {
                        listenerAddFavorite?.invoke(position)
                    }
                    R.id.remove_favorite -> {
                        listenerRemoveFavorite?.invoke(position)
                    }
                    R.id.add_play_list -> {
                        listenerAddPlaylist?.invoke(position)
                    }
                }
                true
            }
        }
        val itemSetAs: Menu = popMenu.menu
        val headerTitle1 = SpannableString(itemSetAs.findItem(R.id.cast_to).title)
        val headerTitle2 = SpannableString(itemSetAs.findItem(R.id.play_on_phone).title)
        val headerTitle3 = SpannableString(itemSetAs.findItem(R.id.add_to_favorite).title)
        val headerTitle5 = SpannableString(itemSetAs.findItem(R.id.remove_favorite).title)
        val headerTitle4 = SpannableString(itemSetAs.findItem(R.id.delete).title)
        val headerTitle6 = SpannableString(itemSetAs.findItem(R.id.remove_recent).title)
        val headerTitle7 = SpannableString(itemSetAs.findItem(R.id.add_play_list).title)
        // Change the color:
        headerTitle1.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle1.length, 0)
        headerTitle2.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle2.length, 0)
        headerTitle3.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle3.length, 0)
        headerTitle4.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle4.length, 0)
        headerTitle5.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle5.length, 0)
        headerTitle6.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle6.length, 0)
        headerTitle7.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle7.length, 0)
        //set title new color
        itemSetAs.getItem(0).title = headerTitle1
        itemSetAs.getItem(1).title = headerTitle2
        itemSetAs.getItem(2).title = headerTitle7
        itemSetAs.getItem(3).title = headerTitle3
        itemSetAs.getItem(4).title = headerTitle5
        itemSetAs.getItem(5).title = headerTitle4
        itemSetAs.getItem(6).title = headerTitle6

        if (isRecent) {
            itemSetAs.removeItem(R.id.delete)
            if (isShowingFragmentFavorite) {
                itemSetAs.removeItem(R.id.remove_recent)
            }
        } else {
            itemSetAs.removeItem(R.id.remove_recent)
            if (isShowingFragmentFavorite) {
                itemSetAs.removeItem(R.id.delete)
            }
        }
        if (isFavorite) {
            itemSetAs.removeItem(R.id.add_to_favorite)
        } else {
            itemSetAs.removeItem(R.id.remove_favorite)
        }
        if (isImage) {
            itemSetAs.removeItem(R.id.add_play_list)
        }

        return popMenu
    }

    fun showPopupMenu(popMenu: PopupMenu) {
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