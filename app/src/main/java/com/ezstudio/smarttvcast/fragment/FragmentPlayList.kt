package com.ezstudio.smarttvcast.fragment

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterPlaylist
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentPlaylistBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.PlaylistUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.viewmodel.FileViewModel
import com.ezteam.baseproject.utils.KeyboardUtils
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import org.koin.android.ext.android.inject

class FragmentPlayList : BaseCastFragment<LayoutFragmentPlaylistBinding>() {

    private var adapterPlaylist: AdapterPlaylist? = null
    private var listPlaylist = mutableListOf<PlayListModel>()
    private val listSearchPlaylist = mutableListOf<PlayListModel>()
    private val listSaver = mutableListOf<PlayListModel>()

    //
    private val fileViewModel by inject<FileViewModel>()
    private val db by inject<AppDatabase>()

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        adapterPlaylist = AdapterPlaylist(requireContext(), listPlaylist)
        binding.rclPlaylist.adapter = adapterPlaylist

        // remove anim
        RecycleViewUtils.clearAnimation(binding.rclPlaylist)
    }

    override fun initData() {
        fileViewModel.listPlaylist.observe(requireActivity()) {
            it?.let {
                binding.layoutCreateNewPlaylist.isVisible = true
                binding.animationLoading.isVisible = false
                listPlaylist.clear()
                listSaver.clear()
                listPlaylist.addAll(it)
                listSaver.addAll(it)
                checkEmpty()
                loadAds()
                adapterPlaylist?.notifyDataSetChanged()
            }
        }
    }

    override fun initListener() {
        //back
        binding.icBack.setOnClickListener {
            (requireActivity() as MainActivity).onBackPressed()
        }
        //add
        binding.icAdd.setOnClickListener {
            FileUtils.showDialogCreatePlaylist(
                requireActivity(), null,
                getString(R.string.create_a_new_playlist)
            ) {
                if (!PlaylistUtils.isDuplicateName(it, db, requireActivity())) {
                    val data = PlayListModel(it, mutableListOf())
                    PlaylistUtils.createANewPlayList(data, db)
                    listPlaylist.add(0, data)
                    adapterPlaylist?.notifyItemInserted(0)
                    checkEmpty()
                    // ads
                    EzAdControl.getInstance(requireActivity()).showAds()
                }
            }
        }
        binding.btnCreatePlaylist.setOnClickListener {
            FileUtils.showDialogCreatePlaylist(
                requireActivity(),
                null,
                getString(R.string.create_a_new_playlist)
            ) {
                if (!PlaylistUtils.isDuplicateName(it, db, requireActivity())) {
                    val data = PlayListModel(it, mutableListOf())
                    PlaylistUtils.createANewPlayList(data, db)
                    listPlaylist.add(0, data)
                    adapterPlaylist?.notifyItemInserted(0)
                    checkEmpty()
                    // ads
                    EzAdControl.getInstance(requireActivity()).showAds()
                }
            }
        }
        //click
        adapterPlaylist?.listenerOnClickItem = {
            val data = listPlaylist[it]
            (requireActivity() as MainActivity).fragmentDetailPlaylist(
                data.listMusic.toMutableList(),
                data.name
            )
        }
        //delete
        adapterPlaylist?.listenerDelete = {
            db.serverDao().deletePlaylist(listPlaylist[it])
            listPlaylist.removeAt(it)
            adapterPlaylist?.notifyItemRemoved(it)
            checkEmpty()
        }
        //rename
        adapterPlaylist?.listenerRename = {
            val data = listPlaylist[it]
            KeyboardUtils.showSoftKeyboard(activity)
            FileUtils.showDialogCreatePlaylist(
                requireActivity(),
                data.name,
                getString(R.string.rename_playlist)
            ) { name ->
                data.name = name
                db.serverDao().insertPlayList(data)
                adapterPlaylist?.notifyItemChanged(it)
                // ads
                EzAdControl.getInstance(requireActivity()).showAds()
            }
        }
        // search
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                TODO("Not yet implemented")
                Log.d("Huy", "beforeTextChanged: ")
                if (listSearchPlaylist.isNullOrEmpty()) {
                    listSearchPlaylist.addAll(listPlaylist)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listPlaylist.clear()
                    for (item in listSearchPlaylist) {
                        item.name.let {
                            if (it.contains(s.toString(), true)) {
                                listPlaylist.add(item)
                            }
                        }
                    }
                    binding.rclPlaylist.adapter?.notifyDataSetChanged()
                    if (listPlaylist.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listPlaylist.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listPlaylist.addAll(listSaver)
                    binding.rclPlaylist.adapter?.notifyDataSetChanged()
                    listSearchPlaylist.clear()
                }
            }

        })
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
            PlayListModel(
                "",
                mutableListOf()
            )
        fileModel.ads = nativeAd

        if (listSaver.size == 1) {
            listSaver.add(1, fileModel)
            listPlaylist.add(1, fileModel)
            adapterPlaylist?.notifyItemInserted(1)
        } else if (listSaver.size >= 2) {
            listSaver.add(2, fileModel)
            listPlaylist.add(2, fileModel)
            adapterPlaylist?.notifyItemInserted(2)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentPlaylistBinding {
        return LayoutFragmentPlaylistBinding.inflate(LayoutInflater.from(requireContext()))
    }

    private fun checkEmpty() {
        binding.layoutCreateNewPlaylist.isVisible = listPlaylist.isNullOrEmpty()
    }

    override fun onDestroy() {
        fileViewModel.listPlaylist.value = null
        super.onDestroy()
    }

}