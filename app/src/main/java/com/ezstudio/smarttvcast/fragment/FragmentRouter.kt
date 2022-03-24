package com.ezstudio.smarttvcast.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.mediarouter.media.MediaRouter
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterRouter
import com.ezstudio.smarttvcast.databinding.LayoutFragmentRouterBinding
import com.ezstudio.smarttvcast.model.ItemRouter
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezteam.baseproject.fragment.BaseFragment
import com.google.android.gms.ads.ez.EzAdControl


class FragmentRouter(var mediaRouter: MediaRouter?) : BaseCastFragment<LayoutFragmentRouterBinding>() {
    private var adapterRouter: AdapterRouter? = null
    private var list = mutableListOf<ItemRouter>()
    private var id: String? = null

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        adapterRouter = AdapterRouter(requireContext(), list)
        binding.rclRouter.adapter = adapterRouter
        RecycleViewUtils.clearAnimation(binding.rclRouter)
    }

    public override fun initData() {
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

    override fun initListener() {
        adapterRouter?.onClickItem = {
            mediaRouter?.routes?.forEach { router ->
                if (router.id == list[it].id) {
                    id = router.id
                    router.select()
                }
            }
        }

        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
            (requireActivity() as MainActivity).removeCallBackScan()
        }

        binding.icRescan.setOnClickListener {
            if (!aVoidDoubleClick()) {
                binding.icRescan.isClickable = false
                (requireActivity() as MainActivity).removeCallBackScan()
                (requireActivity() as MainActivity).startScan()
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
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentRouterBinding {
        return LayoutFragmentRouterBinding.inflate(inflater, container, false)
    }

    fun updateItem() {
        for (item in list) {
            if (item.id == id) {
                adapterRouter?.notifyItemChanged(list.indexOf(item))
            }
        }
    }
}