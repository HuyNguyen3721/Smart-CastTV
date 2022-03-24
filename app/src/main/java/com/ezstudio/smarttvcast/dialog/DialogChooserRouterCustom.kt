package com.ezstudio.smarttvcast.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.mediarouter.media.MediaRouter
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.adapter.AdapterRouter
import com.ezstudio.smarttvcast.databinding.LayoutDialogChooserRouterBinding
import com.ezstudio.smarttvcast.model.ItemRouter
import com.ezstudio.smarttvcast.utils.RecycleViewUtils

class DialogChooserRouterCustom(
    context: Context,
    var style: Int,
    var mediaRouter: MediaRouter?
) :
    Dialog(context, style) {
    private lateinit var binding: LayoutDialogChooserRouterBinding
    private var adapterRouter: AdapterRouter? = null
    private var list = mutableListOf<ItemRouter>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setCancelable(true)
        initData()
        initView()
        initListener()
    }

    private fun initListener() {
        adapterRouter?.onClickItem = {
            mediaRouter?.routes?.forEach { router ->
                if (router.id == list[it].id) {
                    router.select()
                }
            }
        }
    }

    private fun initView() {
        //
        binding = LayoutDialogChooserRouterBinding.inflate(LayoutInflater.from(context))
        adapterRouter = AdapterRouter(context, list)
        binding.rclRouter.adapter = adapterRouter
        RecycleViewUtils.clearAnimation(binding.rclRouter)
        //
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

    fun initData() {
        list.clear()
        mediaRouter?.routes?.forEach {
            val item = ItemRouter(it.id, R.drawable.ic_cast_tv, it.name, it.description ?: "")
            if (!list.contains(item) && item.description.isNotEmpty()) {
                list.add(item)
            }
        }
        adapterRouter?.notifyDataSetChanged()
    }
}