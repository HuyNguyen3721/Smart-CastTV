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
import com.ezstudio.smarttvcast.databinding.LayoutFragmentAudioBinding
import com.ezstudio.smarttvcast.key.Vault
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.PlaylistUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.viewmodel.AudioViewModel
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject

class FragmentAudio : BaseCastFragment<LayoutFragmentAudioBinding>() {
    private lateinit var adapterAudio: AdapterLineaFile<AudioModel>
    private lateinit var adapterFileGrid: AdapterFileGrid<*>
    private lateinit var adapterPlaylist: AdapterPlaylist
    private val listAudio = mutableListOf<AudioModel>()
    private val listSearchAudio = mutableListOf<AudioModel>()
    private val listSaver = mutableListOf<AudioModel>()
    private val listPlaylist = mutableListOf<PlayListModel>()
    private val viewModel by inject<AudioViewModel>()
    private var layoutLiner: Boolean = true
    private val db by inject<AppDatabase>()
    private var isAddAds = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>


    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        layoutLiner = PreferencesUtils.getBoolean(Vault.KEY_AUDIO_LAYOUT, true)
        adapterAudio = AdapterLineaFile(requireContext(), listAudio, db)
        adapterFileGrid = AdapterFileGrid(requireContext(), listAudio, db)
        if (layoutLiner) {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
            binding.rclAudio.layoutManager = LinearLayoutManager(requireContext())
            binding.rclAudio.adapter = adapterAudio
        } else {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
            binding.rclAudio.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rclAudio.adapter = adapterFileGrid
        }
        // clear animation rcl
        RecycleViewUtils.clearAnimation(binding.rclAudio)
        //
        listPlaylist.clear()
        listPlaylist.addAll(db.serverDao().getPlaylist)
        adapterPlaylist = AdapterPlaylist(requireContext(), listPlaylist, true)
        binding.bottomSheetPlaylist.rclQueue.adapter = adapterPlaylist
    }

    override fun initData() {
        listAudio.clear()
        viewModel.audios.observe(this) {
            it?.let {
                if (!listAudio.any { i -> i.path == it.path }) {
                    listAudio.add((it))
                    listSaver.add((it))
                    adapterAudio.notifyItemInserted(listAudio.size - 1)
                    checkEmpty()
                    if (!isAddAds) {
                        isAddAds = true
                        Handler().postDelayed({
                            loadAds()
                        }, 1500)
                    }
                }
            }
        }
        binding.root.postDelayed({
            viewModel.getAudiosRestore(requireContext())
        }, 200)
        //
        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheetPlaylist.layoutBottomSheet)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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
        adapterAudio.onClickAudio = {
            if (!aVoidDoubleClick()) {
                showCastClickAudio(it)
            }
        }
        adapterFileGrid.listenerOnClickAudio = {
            if (!aVoidDoubleClick()) {
                showCastClickAudio(it)
            }
        }
        //add playlist
        adapterAudio.listenerAddPlaylist = { position ->
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
                if (listSearchAudio.isNullOrEmpty()) {
                    listSearchAudio.addAll(listAudio)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listAudio.clear()
                    for (item in listSearchAudio) {
                        item.songName?.let {
                            if (it.contains(s.toString(), true)) {
                                listAudio.add(item)
                            }
                        }
                    }
                    binding.rclAudio.adapter?.notifyDataSetChanged()
                    if (listAudio.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listAudio.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listAudio.addAll(listSaver)
                    binding.rclAudio.adapter?.notifyDataSetChanged()
                    listSearchAudio.clear()
                }
            }

        })
        //delete
        adapterAudio.listenerDelete = {
            deleteFile(it)
        }
        adapterFileGrid.listenerDelete = {
            deleteFile(it)
        }
        // show phone
        adapterAudio.listenerPlayOnPhone = {
            showAudioToPhone(listAudio[it], it)
        }
        adapterFileGrid.listenerPlayOnPhone = {
            showAudioToPhone(listAudio[it], it)
        }
        // cast
        adapterAudio.listenerCastTo = {
            showCast(it)
        }
        adapterFileGrid.listenerCastTo = {
            showCast(it)
        }
        //add favorite
        adapterAudio.listenerAddFavorite = {
            FileUtils.favoriteAudioDb(listAudio[it], true, db)
            adapterAudio.notifyItemChanged(it)
        }
        adapterFileGrid.listenerAddFavorite = {
            FileUtils.favoriteAudioDb(listAudio[it], true, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        //remove favorite
        adapterAudio.listenerRemoveFavorite = {
            FileUtils.favoriteAudioDb(listAudio[it], false, db)
            adapterAudio.notifyItemChanged(it)
        }
        adapterFileGrid.listenerRemoveFavorite = {
            FileUtils.favoriteAudioDb(listAudio[it], false, db)
            adapterFileGrid.notifyItemChanged(it)
        }
        // change layout
        binding.icOrderLayout.setOnClickListener {
            if (binding.icOrderLayout.drawable.toBitmap()
                    .sameAs(resources.getDrawable(R.drawable.ic_arrange_liner).toBitmap())
            ) {
                PreferencesUtils.putBoolean(Vault.KEY_AUDIO_LAYOUT, false)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
                binding.rclAudio.layoutManager = GridLayoutManager(requireContext(), 2)
                viewAdsGrid(true)
                binding.rclAudio.adapter = adapterFileGrid
            } else {
                PreferencesUtils.putBoolean(Vault.KEY_AUDIO_LAYOUT, true)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
                binding.rclAudio.layoutManager = LinearLayoutManager(requireContext())
                binding.rclAudio.adapter = adapterAudio
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
            listAudio.add(2, fileModel)
            viewAdsGrid(true)
            adapterAudio.notifyItemInserted(2)
            adapterFileGrid.notifyItemChanged(2)
        }
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentAudioBinding {
        return LayoutFragmentAudioBinding.inflate(inflater, container, false)
    }


    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {

        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.audios.value = null
    }

    // fun delete
    private fun deleteFile(it: Int) {
        FileUtils.showDeleteFile(
            listAudio[it],
            getString(R.string.do_you_want_to_delete_audio),
            getString(R.string.delete_audio), requireContext(),
            requireActivity(), db
        ) { touch ->
            if (touch) {
                requireActivity().runOnUiThread {
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    //  update playlist
                    val listPlay = db.serverDao().getPlaylist
                    for (item in listPlay) {
                        for (data in item.listMusic) {
                            if (data == listAudio[it].path) {
                                (item.listMusic as MutableList).remove(data)
                                db.serverDao().insertPlayList(item)
                                break
                            }
                        }
                    }
                    //
                    listSaver.remove(listAudio[it])
                    listAudio.removeAt(it)
                    adapterAudio.notifyItemRemoved(it)
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
        binding.animationView.isVisible = listAudio.isNullOrEmpty()
    }


    private fun showAudioToPhone(audioModel: AudioModel, position: Int) {
        //add recent
        viewModel.updateFileRecent(audioModel, db)
        //show
        (requireActivity() as MainActivity).showVideoAudio(listAudio, position)
    }


    private fun showCastClickAudio(position: Int) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listAudio, position)
        } else {
            showAudioToPhone(listAudio[position], position)
        }
    }

    private fun showCast(position: Int) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listAudio, position)
        } else {
            (requireActivity() as MainActivity).showDialogScanRouter()
        }
    }


    private fun addPlaylist(position: Int) {
        if (db.serverDao().getAudioByPath(listAudio[position].path) == null) {
            db.serverDao().insertAudio(listAudio[position])
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
                    val data = PlayListModel(it, mutableListOf(listAudio[position].path!!))
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
                    if (item == listAudio[position].path) {
                        isCheck = true
                        toast(getString(R.string.file_exits))
                    }
                }
                if (!isCheck) {
                    (list.listMusic as MutableList).add(0, listAudio[position].path!!)
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
        if (binding.rclAudio.layoutManager is GridLayoutManager) {
            (binding.rclAudio.layoutManager as GridLayoutManager).spanSizeLookup =
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