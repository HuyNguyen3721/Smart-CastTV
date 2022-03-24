package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible
import androidx.mediarouter.media.MediaRouter
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.adapter.AdapterRouterDialog
import com.ezstudio.smarttvcast.databinding.LayoutDialogScanRouterBinding
import com.ezstudio.smarttvcast.model.ItemRouter
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.utils.WifiUtils


class DialogScanRouter(
    context: Context,
    var mediaRouter: MediaRouter?, var height: Int
) :
    AlertDialog(context) {
    private lateinit var binding: LayoutDialogScanRouterBinding
    private var adapterRouter: AdapterRouterDialog? = null 
    private var list = mutableListOf<ItemRouter>()
    private var id: String? = null


    var listenerNo: (() -> Unit)? = null
    var listenerYes: (() -> Unit)? = null
    var listenerRescan: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setCancelable(true)
        initView()
        initData()
        initListener()
    }

    fun initData() {
        list.clear()
        mediaRouter?.routes?.forEach {
            val item = ItemRouter(it.id, R.drawable.ic_chorme_cast, it.name, it.description ?: "")
            if (!list.contains(item) && item.description.isNotEmpty() && !list.any { it.name == item.name }) {
                list.add(item)
            }
        }
        adapterRouter?.notifyDataSetChanged()
        if (list.isEmpty()) {
            binding.txtNoDevices.isVisible = true
            binding.rclRouter.isVisible = false
        } else {
            binding.txtNoDevices.isVisible = false
            binding.rclRouter.isVisible = true
        }
    }

    private fun initView() {
        binding = LayoutDialogScanRouterBinding.inflate(LayoutInflater.from(context))
        binding.layout.layoutParams.height = (height * 0.62f).toInt()
        binding.layout.invalidate()
        adapterRouter = AdapterRouterDialog(context, list)
        binding.rclRouter.adapter = adapterRouter
        //
        updateStateWifi()
        //
        animScan()


//        binding.scanning2.animate()
//            .translationY((binding.layoutScanning.width + binding.scanning1.width).toFloat())
        RecycleViewUtils.clearAnimation(binding.rclRouter)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //

        setContentView(binding.root)
    }

    private fun animScan() {
        binding.scanning1.post {
            val translation = TranslateAnimation(
                -binding.scanning1.width.toFloat(),
                binding.layoutScanning.width.toFloat(),
                0F,
                0F
            )
            translation.duration = 3000
            translation.fillAfter = true
            translation.repeatCount = Int.MAX_VALUE
            binding.scanning1.startAnimation(translation)

            Handler().postDelayed({
                binding.scanning2.post {
                    val translation2 = TranslateAnimation(
                        -binding.scanning2.width.toFloat(),
                        binding.layoutScanning.width.toFloat(),
                        0F,
                        0F
                    )
                    translation2.duration = 3000
                    translation2.fillAfter = true
                    translation2.repeatCount = Int.MAX_VALUE
                    binding.scanning2.startAnimation(translation2)
                    binding.scanning2.visibility = View.VISIBLE
                }
            }, 1500)
        }
    }

    private fun initListener() {
//        binding.btnNo.setOnClickListener {
//            dismiss()
//        }
//        binding.btnYes.setOnClickListener {
//            listenerYes?.invoke()
//            dismiss()
//        }
        adapterRouter?.onClickItem = {
            mediaRouter?.routes?.forEach { router ->
                if (router.id == list[it].id) {
                    id = router.id
                    router.select()
                }
            }
        }

        binding.icRescan.setOnClickListener {
            binding.icRescan.isClickable = false
            listenerRescan?.invoke()
            binding.layoutAnimRescan.visibility = View.VISIBLE
            binding.layoutAnimRescan.scaleX = 0F
            binding.layoutAnimRescan.scaleY = 0F
            binding.layoutAnimRescan.animate().scaleX(1F).scaleY(1F).setDuration(300)
                .withEndAction {
                    binding.layoutAnimRescan.postDelayed({
                        binding.layoutAnimRescan.animate().scaleX(0F).scaleY(0F)
                            .setDuration(300)
                            .withEndAction {
                                binding.layoutAnimRescan.visibility = View.INVISIBLE
                                binding.icRescan.isClickable = true
                            }.start()
                    }, 3500)
                }.start()
        }
        //
        binding.txtConnect.setOnClickListener {
            WifiUtils.openSettingWifi(context)
            dismiss()
        }
        //
        binding.btnCandle.setOnClickListener {
            dismiss()
        }
    }

    override fun show() {
        super.show()
    }

    fun updateItem() {
        for (item in list) {
            if (item.id == id) {
                adapterRouter?.notifyItemChanged(list.indexOf(item))
            }
        }
    }

    fun updateStateWifi() {
        if (WifiUtils.checkEnable(context)) {
            binding.icWifi.setImageResource(R.drawable.ic_wifi)
            if (WifiUtils.isConnected(context)) {
                binding.txtConnect.isVisible = false
                binding.layoutScanning.isVisible = false
                binding.txtStateWifi.text = context.getString(R.string.wi_fi_connected)
                binding.txtNoDevices.text = context.getString(R.string.no_devices_found)
                binding.txtNoDevices.setTextColor(Color.parseColor("#4A525C"))
                binding.txtHelp.isVisible = false
            } else {
                animScan()
                binding.txtNoDevices.text = context.getString(R.string.finding_for_device)
                binding.txtNoDevices.setTextColor(Color.parseColor("#222222"))
                list.clear()
                adapterRouter?.notifyDataSetChanged()
                binding.txtConnect.isVisible = true
                binding.txtHelp.isVisible = true
                binding.layoutScanning.isVisible = true
                binding.txtStateWifi.text = context.getString(R.string.wi_fi_not_connected)
            }
            binding.txtStateWifi.setTextColor(Color.parseColor("#4D222222"))
        } else {
            animScan()
            list.clear()
            binding.txtHelp.isVisible = true
            binding.txtNoDevices.text = context.getString(R.string.finding_for_device)
            binding.txtNoDevices.setTextColor(Color.parseColor("#222222"))
            adapterRouter?.notifyDataSetChanged()
            binding.txtConnect.isVisible = true
            binding.layoutScanning.isVisible = true
            binding.icWifi.setImageResource(R.drawable.ic_wifi_off)
            binding.txtStateWifi.text = context.getString(R.string.wi_fi_turn_off)
            binding.txtStateWifi.setTextColor(Color.parseColor("#EA4335"))
        }
    }


}