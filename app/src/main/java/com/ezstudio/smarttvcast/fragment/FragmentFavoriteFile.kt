package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
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
import com.ezstudio.smarttvcast.databinding.LayoutFragmentFavoriteFileBinding
import com.ezstudio.smarttvcast.key.Vault
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.FileUtils.showRemoveRecent
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.viewmodel.AudioViewModel
import com.ezstudio.smarttvcast.viewmodel.FileViewModel
import com.ezstudio.smarttvcast.viewmodel.VideoViewModel
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import org.koin.android.ext.android.inject

class FragmentFavoriteFile : BaseCastFragment<LayoutFragmentFavoriteFileBinding>() {

    private lateinit var adapterLineaFile: AdapterLineaFile<Any>
    private lateinit var adapterGridFile: AdapterFileGrid<Any>

    //    private lateinit var adapterFileGrid: AdapterFileGrid<*>
    private val listFavoriteFile = mutableListOf<Any>()
    private val listSearchFavoriteFile = mutableListOf<Any>()
    private val listSaver = mutableListOf<Any>()
    private val videoViewModel by inject<VideoViewModel>()
    private val audioViewModel by inject<AudioViewModel>()
    private val fileViewModel by inject<FileViewModel>()

    private val db by inject<AppDatabase>()
    private var layoutLiner: Boolean = true

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        layoutLiner = PreferencesUtils.getBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, true)
        adapterLineaFile = AdapterLineaFile(
            requireContext(), listFavoriteFile, db,
            isRecent = false,
            isShowingFragmentFavorite = true
        )
        adapterGridFile = AdapterFileGrid(
            requireContext(), listFavoriteFile, db,
            isRecent = false,
            isShowingFragmentFavorite = true
        )
        if (layoutLiner) {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
            binding.rclFavorite.layoutManager = LinearLayoutManager(requireContext())
            binding.rclFavorite.adapter = adapterLineaFile
        } else {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
            binding.rclFavorite.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rclFavorite.adapter = adapterGridFile
        }
        // clear animation rcl
        RecycleViewUtils.clearAnimation(binding.rclFavorite)
    }

    override fun initData() {
        fileViewModel.listFileFavoriteViewModel.observe(requireActivity()) {
            it?.let {
                binding.animationView.isVisible = true
                binding.animationLoading.isVisible = false
                listFavoriteFile.clear()
                listSaver.clear()
                listFavoriteFile.addAll(it)
                listSaver.addAll(it)
                checkEmpty()
                loadAds()
                adapterLineaFile.notifyDataSetChanged()
                adapterGridFile.notifyDataSetChanged()
            }
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun initListener() {
        checkEmpty()
        //video
        adapterLineaFile.onClickVideo = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideoAudio(it)
            }
        }
        adapterGridFile.listenerOnClickVideo = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideoAudio(it)
            }
        }

        // audio
        adapterLineaFile.onClickAudio = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideoAudio(it)
            }
        }
        adapterGridFile.listenerOnClickAudio = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideoAudio(it)
            }
        }
        // image
        adapterLineaFile.onClickImage = {
            if (!aVoidDoubleClick()) {
                showCastImage(it)
            }
        }
        adapterGridFile.listenerOnClickImage = {
            if (!aVoidDoubleClick()) {
                showCastImage(it)
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
                Log.d("Huy", "beforeTextChanged: ")
                if (listSearchFavoriteFile.isNullOrEmpty()) {
                    listSearchFavoriteFile.addAll(listFavoriteFile)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listFavoriteFile.clear()
                    for (item in listSearchFavoriteFile) {
                        when (item) {
                            is VideoModel -> {
                                item.fileName?.let {
                                    if (it.contains(s.toString(), true)) {
                                        listFavoriteFile.add(item)
                                    }
                                }
                            }
                            is AudioModel -> {
                                item.songName?.let {
                                    if (it.contains(s.toString(), true)) {
                                        listFavoriteFile.add(item)
                                    }
                                }
                            }
                            is ImageModel -> {
                                item.name?.let {
                                    if (it.contains(s.toString(), true)) {
                                        listFavoriteFile.add(item)
                                    }
                                }
                            }
                        }
                    }
                    binding.rclFavorite.adapter?.notifyDataSetChanged()
                    if (listFavoriteFile.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listFavoriteFile.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listFavoriteFile.addAll(listSaver)
                    binding.rclFavorite.adapter?.notifyDataSetChanged()
                    listSearchFavoriteFile.clear()
                }
            }

        })
        //delete
        adapterLineaFile.listenerRemoveRecent = {
            deleteFile(it)
        }
        adapterGridFile.listenerRemoveRecent = {
            deleteFile(it)
        }
        // show phone
        adapterLineaFile.listenerPlayOnPhone = {
            showPhonePopMenu(listFavoriteFile[it])
        }
        adapterGridFile.listenerPlayOnPhone = {
            showPhonePopMenu(listFavoriteFile[it])
        }
        // cast
        adapterLineaFile.listenerCastTo = {
            showCastPopMenu(listFavoriteFile[it])
        }
        adapterGridFile.listenerCastTo = {
            showCastPopMenu(listFavoriteFile[it])
        }
        // add favorite
        adapterLineaFile.listenerAddFavorite = {
            favoriteRecentDb(listFavoriteFile[it], true, db)
            adapterLineaFile.notifyItemChanged(it)
        }
        adapterGridFile.listenerAddFavorite = {
            favoriteRecentDb(listFavoriteFile[it], true, db)
            adapterGridFile.notifyItemChanged(it)
        }
        // remove favorite
        adapterLineaFile.listenerRemoveFavorite = {
            favoriteRecentDb(listFavoriteFile[it], false, db)
            adapterLineaFile.notifyItemChanged(it)
        }
        adapterGridFile.listenerRemoveFavorite = {
            favoriteRecentDb(listFavoriteFile[it], false, db)
            adapterGridFile.notifyItemChanged(it)
        }

        // change layout
        binding.icOrderLayout.setOnClickListener {
            if (binding.icOrderLayout.drawable.toBitmap()
                    .sameAs(resources.getDrawable(R.drawable.ic_arrange_liner).toBitmap())
            ) {
                PreferencesUtils.putBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, false)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
                binding.rclFavorite.layoutManager = GridLayoutManager(requireContext(), 2)
                viewAdsGrid(true)
                binding.rclFavorite.adapter = adapterGridFile
            } else {
                PreferencesUtils.putBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, true)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
                binding.rclFavorite.layoutManager = LinearLayoutManager(requireContext())
                binding.rclFavorite.adapter = adapterLineaFile
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
                    Log.d("Huy", "initListener: load Ads1")
                    addAdsToList(nativeAd)
                }

                override fun onClickAd() {
                }
            })
    }

    private fun addAdsToList(nativeAd: RelativeLayout?) {
        val fileModel =
            AudioModel(
                null,
                null,
                null,
                null,
                0, false, 0, false, null, nativeAd
            )
        if (listSaver.size >= 2) {
            listSaver.add(2, fileModel)
            listFavoriteFile.add(2, fileModel)
            viewAdsGrid(true)
            adapterLineaFile.notifyItemInserted(2)
            adapterGridFile.notifyItemChanged(2)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentFavoriteFileBinding {
        return LayoutFragmentFavoriteFileBinding.inflate(LayoutInflater.from(requireContext()))
    }

    private fun showCastImage(position: Int, isShowPhone: Boolean = false) {
        var positionCast = 0
        val listImage = mutableListOf<ImageModel>()
        for (item in listFavoriteFile) {
            if (item is ImageModel) {
                listImage.add(item)
                if (item == listFavoriteFile[position]) {
                    positionCast = listImage.indexOf(item)
                }
            }
        }
        if (isShowPhone) {
            (activity as MainActivity).fragmentCastImage(listImage, positionCast, false, true)
        } else {
            (activity as MainActivity).fragmentCastImage(listImage, positionCast)
        }
    }

    private fun checkEmpty() {
        binding.animationView.isVisible = listFavoriteFile.isNullOrEmpty()
    }

    private fun showCastOnClickVideoAudio(position: Int, isOnlyCast: Boolean = false) {
        var positionCast = 0
        val listVideoAudio = mutableListOf<Any>()
        for (item in listFavoriteFile) {
            if (item !is ImageModel) {
                listVideoAudio.add(item)
                if (item == listFavoriteFile[position]) {
                    positionCast = listVideoAudio.indexOf(item)
                }
            }
        }
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listVideoAudio, positionCast)
        } else {
            if (isOnlyCast) {
                (requireActivity() as MainActivity).showDialogScanRouter()
            } else {
                showVideoAudioToPhone(listVideoAudio[positionCast], positionCast, listVideoAudio)
            }
        }
    }

    private fun showVideoAudioToPhone(model: Any, position: Int, list: MutableList<Any>) {
        // recent
        when (model) {
            is VideoModel -> {
                videoViewModel.updateFileRecent(model, db)
                //show
                (requireActivity() as MainActivity).showVideoAudio(list, position)
            }
            is AudioModel -> {
                audioViewModel.updateFileRecent(model, db)
                //show
                (requireActivity() as MainActivity).showVideoAudio(list, position)
            }
        }
    }

    // favorite
    private fun favoriteRecentDb(fileModel: Any, isFavorite: Boolean, db: AppDatabase) {
        when (fileModel) {
            is VideoModel -> {
                FileUtils.favoriteVideoDb(fileModel, isFavorite, db)
            }
            is AudioModel -> {
                FileUtils.favoriteAudioDb(fileModel, isFavorite, db)
            }
            is ImageModel -> {
                FileUtils.favoriteImageDb(fileModel, isFavorite, db)
            }
        }
    }

    // fun delete
    private fun deleteFile(it: Int) {
        showRemoveRecent(
            getString(R.string.do_you_want_to_remove_recent),
            getString(R.string.remove_recent_file), requireContext()
        ) { done ->
            if (done) {
                requireActivity().runOnUiThread {
                    //update db
                    when (val data = listFavoriteFile[it]) {
                        is VideoModel -> {
                            data.timeRecent = 0L
                            db.serverDao().updateVideo(data)
                        }
                        is AudioModel -> {
                            data.timeRecent = 0L
                            db.serverDao().updateAudio(data)
                        }
                        is ImageModel -> {
                            data.timeRecent = 0L
                            db.serverDao().updateImage(data)
                        }
                    }
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    listSaver.remove(listFavoriteFile[it])
                    listFavoriteFile.removeAt(it)
                    adapterGridFile.notifyItemRemoved(it)
                    adapterLineaFile.notifyItemRemoved(it)
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

    private fun showPhonePopMenu(data: Any) {
        var positionCast = 0
        val listVideoAudio = mutableListOf<Any>()
        for (item in listFavoriteFile) {
            if (item !is ImageModel) {
                listVideoAudio.add(item)
                if (item == data) {
                    positionCast = listVideoAudio.indexOf(item)
                }
            }
        }
        when (data) {
            is VideoModel -> {
                showVideoAudioToPhone(data, positionCast, listVideoAudio)
            }
            is AudioModel -> {
                showVideoAudioToPhone(data, positionCast, listVideoAudio)
            }
            is ImageModel -> {
                showCastImage(listFavoriteFile.indexOf(data), true)
            }
        }
    }

    private fun showCastPopMenu(data: Any) {
        when (data) {
            is VideoModel -> {
                showCastOnClickVideoAudio(listFavoriteFile.indexOf(data), true)
            }
            is AudioModel -> {
                showCastOnClickVideoAudio(listFavoriteFile.indexOf(data), true)
            }
            is ImageModel -> {
                showCastImage(listFavoriteFile.indexOf(data), true)
            }
        }
    }

    override fun onDestroy() {
        fileViewModel.listFileFavoriteViewModel.value = null
        super.onDestroy()
    }

    private fun viewAdsGrid(isAboutTwoItem: Boolean) {
        if (binding.rclFavorite.layoutManager is GridLayoutManager) {
            (binding.rclFavorite.layoutManager as GridLayoutManager).spanSizeLookup =
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