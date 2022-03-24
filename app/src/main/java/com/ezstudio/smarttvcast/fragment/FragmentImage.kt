package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterFileGrid
import com.ezstudio.smarttvcast.adapter.AdapterLineaFile
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentImageBinding
import com.ezstudio.smarttvcast.key.Vault
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.viewmodel.ImageViewModel
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import org.koin.android.ext.android.inject
import java.io.File

class FragmentImage : BaseCastFragment<LayoutFragmentImageBinding>() {

    private lateinit var adapterImage: AdapterLineaFile<ImageModel>
    private lateinit var adapterFileGrid: AdapterFileGrid<*>
    private val listImage = mutableListOf<ImageModel>()
    private val listSearchImage = mutableListOf<ImageModel>()
    private val listSaver = mutableListOf<ImageModel>()
    private val viewModel by inject<ImageViewModel>()
    private var layoutLiner: Boolean = true
    private var isAddAds = false
    private val db by inject<AppDatabase>()

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        layoutLiner = PreferencesUtils.getBoolean(Vault.KEY_IMAGE_LAYOUT, true)

        adapterImage = AdapterLineaFile(requireContext(), listImage, db)
        adapterFileGrid = AdapterFileGrid(requireContext(), listImage, db)
//
        if (layoutLiner) {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
            binding.rclImage.layoutManager = LinearLayoutManager(requireContext())
            binding.rclImage.adapter = adapterImage
        } else {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
            binding.rclImage.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rclImage.adapter = adapterFileGrid
        }
        // clear animation rcl
        RecycleViewUtils.clearAnimation(binding.rclImage)
    }

    override fun initData() {
        listImage.clear()
        listSaver.clear()
        viewModel.imageStoreLiveData.observe(this) {
            it?.let { path ->
                val data = ImageModel(path, File(path).name)
                listImage.add(data)
                listSaver.add(data)
                adapterImage.notifyItemInserted(listImage.size - 1)
                checkEmpty()
                if (!isAddAds) {
                    isAddAds = true
                    Handler().postDelayed({
                        loadAds()
                    }, 1500)
                }
            }
        }

        binding.root.postDelayed({
            viewModel.getImagesGallery(requireContext())
        }, 200)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun initListener() {
        adapterImage.onClickImage = {
            if (!aVoidDoubleClick()) {
                (activity as MainActivity).fragmentCastImage(listImage, it)
            }
        }
        adapterFileGrid.listenerOnClickImage = {
            if (!aVoidDoubleClick()) {
                (activity as MainActivity).fragmentCastImage(listImage, it)
            }
        }
        //back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // search
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                TODO("Not yet implemented")
                if (listSearchImage.isNullOrEmpty()) {
                    listSearchImage.addAll(listImage)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listImage.clear()
                    for (item in listSearchImage) {
                        item.name?.let {
                            if (it.contains(s.toString(), true)) {
                                listImage.add(item)
                            }
                        }
                    }
                    binding.rclImage.adapter?.notifyDataSetChanged()
                    if (listImage.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listImage.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listImage.addAll(listSaver)
                    binding.rclImage.adapter?.notifyDataSetChanged()
                    listSearchImage.clear()
                }
            }
        })
        //delete
        adapterImage.listenerDelete = {
            deleteFile(it)
        }
        adapterFileGrid.listenerDelete = {
            deleteFile(it)
        }
        // show phone
        adapterImage.listenerPlayOnPhone = {
            (activity as MainActivity).fragmentCastImage(listImage, it, false, true)
        }
        adapterFileGrid.listenerPlayOnPhone = {
            (activity as MainActivity).fragmentCastImage(listImage, it, false, true)
        }
        // cast
        adapterImage.listenerCastTo = {
            showCast(it)
        }
        adapterFileGrid.listenerCastTo = {
            showCast(it)
        }
        // add favorite
        adapterImage.listenerAddFavorite = {
            FileUtils.favoriteImageDb(listImage[it], true, db)
            adapterImage.notifyItemChanged(it)
        }
        adapterFileGrid.listenerAddFavorite = {
            FileUtils.favoriteImageDb(listImage[it], true, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        // remove favorite
        adapterImage.listenerRemoveFavorite = {
            FileUtils.favoriteImageDb(listImage[it], false, db)
            adapterImage.notifyItemChanged(it)
        }
        adapterFileGrid.listenerRemoveFavorite = {
            FileUtils.favoriteImageDb(listImage[it], false, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        // change layout
        binding.icOrderLayout.setOnClickListener {
            if (binding.icOrderLayout.drawable.toBitmap()
                    .sameAs(resources.getDrawable(R.drawable.ic_arrange_liner).toBitmap())
            ) {
                PreferencesUtils.putBoolean(Vault.KEY_IMAGE_LAYOUT, false)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
                binding.rclImage.layoutManager = GridLayoutManager(requireContext(), 2)
                viewAdsGrid(true)
                binding.rclImage.adapter = adapterFileGrid
            } else {
                PreferencesUtils.putBoolean(Vault.KEY_IMAGE_LAYOUT, true)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
                binding.rclImage.layoutManager = LinearLayoutManager(requireContext())
                binding.rclImage.adapter = adapterImage
            }
        }
    }

    private fun loadAds() {
        AdmobNativeAdView.getNativeAd(
            requireContext(),
            R.layout.native_admob_item_model,
            object : NativeAdListener() {
                override fun onError() {
                }

                override fun onLoaded(nativeAd: RelativeLayout?) {
                    addAdsToList(nativeAd)
                }

                override fun onClickAd() {
                }
            })
    }

    private fun addAdsToList(nativeAd: RelativeLayout?) {
        val fileModel =
            ImageModel(
                null,
                null,
                0,
                false,
                null, false, nativeAd
            )
        if (listSaver.size >= 2) {
            listSaver.add(2, fileModel)
            listImage.add(2, fileModel)
            viewAdsGrid(true)
            adapterImage.notifyItemInserted(2)
            adapterFileGrid.notifyItemChanged(2)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentImageBinding {
        return LayoutFragmentImageBinding.inflate(inflater, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.imageStoreLiveData.value = null
    }


    // fun delete
    private fun deleteFile(it: Int) {
        FileUtils.showDeleteFile(
            listImage[it],
            getString(R.string.do_you_want_to_delete_image),
            getString(R.string.delete_image), requireContext(),
            requireActivity(), db
        ) { touch ->
            if (touch) {
                requireActivity().runOnUiThread {
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    listSaver.remove(listImage[it])
                    listImage.removeAt(it)
                    adapterImage.notifyItemRemoved(it)
                    adapterFileGrid.notifyItemRemoved(it)
                    checkEmpty()
                }
            } else {
                requireActivity().runOnUiThread {
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    toast(getString(R.string.delete_failed))
                }
            }
        }
    }

    private fun checkEmpty() {
        binding.animationView.isVisible = listImage.isNullOrEmpty()
    }

    private fun showCast(position: Int) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastImage(listImage, position)
        } else {
            (requireActivity() as MainActivity).showDialogScanRouter()
        }
    }

    private fun viewAdsGrid(isAboutTwoItem: Boolean) {
        if (binding.rclImage.layoutManager is GridLayoutManager) {
            (binding.rclImage.layoutManager as GridLayoutManager).spanSizeLookup =
                object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        Log.d("Huy", "getSpanSize: $position ")
                        return if (position == if (isAboutTwoItem) 2 else 1) {
                            2
                        } else {
                            1
                        }
                    }

                }
        }
    }

}