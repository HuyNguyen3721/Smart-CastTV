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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterFileGrid
import com.ezstudio.smarttvcast.adapter.AdapterLineaFile
import com.ezstudio.smarttvcast.adapter.AdapterPlaylist
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentVideoBinding
import com.ezstudio.smarttvcast.key.Vault
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.FileUtils.showDeleteFile
import com.ezstudio.smarttvcast.utils.PlaylistUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.viewmodel.VideoViewModel
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject

class FragmentVideo : BaseCastFragment<LayoutFragmentVideoBinding>() {
    private lateinit var adapterVideo: AdapterLineaFile<VideoModel>
    private lateinit var adapterFileGrid: AdapterFileGrid<*>
    private lateinit var adapterPlaylist: AdapterPlaylist
    private val listVideo = mutableListOf<VideoModel>()
    private val listSearchVideo = mutableListOf<VideoModel>()
    private val listSaver = mutableListOf<VideoModel>()
    private val listPlaylist = mutableListOf<PlayListModel>()
    private val viewModel by inject<VideoViewModel>()
    private val db by inject<AppDatabase>()
    private var layoutLiner: Boolean = true
    private var isAddAds = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        layoutLiner = PreferencesUtils.getBoolean(Vault.KEY_VIDEO_LAYOUT, true)
        adapterVideo = AdapterLineaFile(requireContext(), listVideo, db)
        adapterFileGrid = AdapterFileGrid(requireContext(), listVideo, db)
        if (layoutLiner) {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
            binding.rclVideo.layoutManager = LinearLayoutManager(requireContext())
            binding.rclVideo.adapter = adapterVideo
        } else {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
            binding.rclVideo.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rclVideo.adapter = adapterFileGrid
        }
        // clear animation rcl
        RecycleViewUtils.clearAnimation(binding.rclVideo)
        //
        listPlaylist.clear()
        listPlaylist.addAll(db.serverDao().getPlaylist)
        adapterPlaylist = AdapterPlaylist(requireContext(), listPlaylist, true)
        binding.bottomSheetPlaylist.rclQueue.adapter = adapterPlaylist
    }

    override fun initData() {
        listVideo.clear()
        listSaver.clear()
        viewModel.videos.observe(this) {
            it?.let {
                listVideo.add((it))
                listSaver.add((it))
                adapterVideo.notifyItemInserted(listVideo.size - 1)
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
            viewModel.getVideoGallery(requireContext())
        }, 200)
        //
        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheetPlaylist.layoutBottomSheet)
    }

    @SuppressLint("SimpleDateFormat", "UseCompatLoadingForDrawables")
    override fun initListener() {
        //bottom sheet
        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // close
                        binding.bgBtnSheet.animate().alpha(0F)
                    }
                }

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })

        binding.bottomSheetPlaylist.layoutBottomSheet.setOnClickListener {
            closeBtnSheet()
        }
        //
        adapterVideo.onClickVideo = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideo(it)
            }
        }
        adapterFileGrid.listenerOnClickVideo = {
            if (!aVoidDoubleClick()) {
                showCastOnClickVideo(it)
            }
        }
        //add playlist
        adapterVideo.listenerAddPlaylist = { position ->
            addPlaylist(position)
        }
        adapterFileGrid.listenerAddPlaylist = { position ->
            addPlaylist(position)
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
                if (listSearchVideo.isNullOrEmpty()) {
                    listSearchVideo.addAll(listVideo)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listVideo.clear()
                    for (item in listSearchVideo) {
                        item.fileName?.let {
                            if (it.contains(s.toString(), true)) {
                                listVideo.add(item)
                            }
                        }
                    }
                    binding.rclVideo.adapter?.notifyDataSetChanged()
                    if (listVideo.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listVideo.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listVideo.addAll(listSaver)
                    binding.rclVideo.adapter?.notifyDataSetChanged()
                    listSearchVideo.clear()
                }
            }

        })
        //delete
        adapterVideo.listenerDelete = {
            deleteFile(it)
        }
        adapterFileGrid.listenerDelete = {
            deleteFile(it)
        }
        // show phone
        adapterVideo.listenerPlayOnPhone = {
            showVideoToPhone(listVideo[it], it)
        }
        adapterFileGrid.listenerPlayOnPhone = {
            showVideoToPhone(listVideo[it], it)
        }
        // cast
        adapterVideo.listenerCastTo = {
            showCast(it)
        }
        adapterFileGrid.listenerCastTo = {
            showCast(it)
        }
        // add favorite
        adapterVideo.listenerAddFavorite = {
            FileUtils.favoriteVideoDb(listVideo[it], true, db)
            adapterVideo.notifyItemChanged(it)
        }
        adapterFileGrid.listenerAddFavorite = {
            FileUtils.favoriteVideoDb(listVideo[it], true, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        // remove favorite
        adapterVideo.listenerRemoveFavorite = {
            FileUtils.favoriteVideoDb(listVideo[it], false, db)
            adapterVideo.notifyItemChanged(it)
        }
        adapterFileGrid.listenerRemoveFavorite = {
            FileUtils.favoriteVideoDb(listVideo[it], false, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        // change layout
        binding.icOrderLayout.setOnClickListener {
            if (binding.icOrderLayout.drawable.toBitmap()
                    .sameAs(resources.getDrawable(R.drawable.ic_arrange_liner).toBitmap())
            ) {
                PreferencesUtils.putBoolean(Vault.KEY_VIDEO_LAYOUT, false)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
                binding.rclVideo.layoutManager = GridLayoutManager(requireContext(), 2)
                viewAdsGrid(true)
                binding.rclVideo.adapter = adapterFileGrid
            } else {
                PreferencesUtils.putBoolean(Vault.KEY_VIDEO_LAYOUT, true)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
                binding.rclVideo.layoutManager = LinearLayoutManager(requireContext())
                binding.rclVideo.adapter = adapterVideo
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
            VideoModel(
                null,
                0,
                null,
                0,
                0, false, false, null, nativeAd
            )
        if (listSaver.size >= 2) {
            listSaver.add(2, fileModel)
            listVideo.add(2, fileModel)
            viewAdsGrid(true)
            adapterVideo.notifyItemInserted(2)
            adapterFileGrid.notifyItemChanged(2)
        }
    }

    private fun showVideoToPhone(videoModel: VideoModel, position: Int) {
        // recent
        viewModel.updateFileRecent(videoModel, db)
        //show
        (requireActivity() as MainActivity).showVideoAudio(listVideo, position)
    }

    private fun checkEmpty() {
        binding.animationView.isVisible = listVideo.isNullOrEmpty()
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentVideoBinding {
        return LayoutFragmentVideoBinding.inflate(inflater, container, false)
    }


    // fun delete
    private fun deleteFile(it: Int) {
        showDeleteFile(
            listVideo[it],
            getString(R.string.do_you_want_to_delete_video),
            getString(R.string.delete_video), requireContext(),
            requireActivity(), db
        ) { touch ->
            if (touch) {
                requireActivity().runOnUiThread {
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    //  update playlist
                    val listPlay = db.serverDao().getPlaylist
                    for (item in listPlay) {
                        for (data in item.listMusic) {
                            if (data == listVideo[it].path) {
                                (item.listMusic as MutableList).remove(data)
                                db.serverDao().insertPlayList(item)
                                break
                            }
                        }
                    }
                    //
                    listSaver.remove(listVideo[it])
                    listVideo.removeAt(it)
                    adapterVideo.notifyItemRemoved(it)
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

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.videos.value = null
    }

    private fun showCastOnClickVideo(position: Int) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listVideo, position)
        } else {
            showVideoToPhone(listVideo[position], position)
        }
    }

    private fun showCast(position: Int) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listVideo, position)
        } else {
            (requireActivity() as MainActivity).showDialogScanRouter()
        }
    }

    private fun addPlaylist(position: Int) {
        if (db.serverDao().getVideoByPath(listVideo[position].path) == null) {
            db.serverDao().insertVideo(listVideo[position])
        }
        listPlaylist.clear()
        listPlaylist.addAll(db.serverDao().getPlaylist)
        adapterPlaylist.notifyDataSetChanged()
        expandBtnSheet()
        //
        // create play list
        binding.bottomSheetPlaylist.itemAdd.setOnClickListener {
            closeBtnSheet()
            FileUtils.showDialogCreatePlaylist(
                requireActivity(),
                null,
                getString(R.string.create_a_playlist)
            ) {
                if (!PlaylistUtils.isDuplicateName(it, db, requireActivity())) {
                    val data = PlayListModel(it, mutableListOf(listVideo[position].path!!))
                    PlaylistUtils.createANewPlayList(data, db)
                    // ads
                    EzAdControl.getInstance(requireActivity()).showAds()
                }
            }
        }
        //add
        adapterPlaylist.listenerOnClickItemQueue = {
            closeBtnSheet()
            val list = db.serverDao().getPlayListByName(listPlaylist[it].name)
            list?.let {
                var isCheck = false
                for (item in list.listMusic) {
                    if (item == listVideo[position].path) {
                        isCheck = true
                        toast(getString(R.string.file_exits))
                    }
                }
                if (!isCheck) {
                    (list.listMusic as MutableList).add(0, listVideo[position].path!!)
                    db.serverDao().insertPlayList(list)
                }
            }
            //
        }
    }

    private fun closeBtnSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.bgBtnSheet.animate().alpha(0F)
    }

    private fun expandBtnSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.bgBtnSheet.animate().alpha(1F)
    }

    private fun viewAdsGrid(isAboutTwoItem: Boolean) {
        if (binding.rclVideo.layoutManager is GridLayoutManager) {
            (binding.rclVideo.layoutManager as GridLayoutManager).spanSizeLookup =
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