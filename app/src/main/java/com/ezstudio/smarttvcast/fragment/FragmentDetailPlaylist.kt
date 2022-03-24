package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterFileGrid
import com.ezstudio.smarttvcast.adapter.AdapterLineaFile
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentDetailPlaylistBinding
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
import org.koin.android.ext.android.inject

class FragmentDetailPlaylist(var name: String) :
    BaseCastFragment<LayoutFragmentDetailPlaylistBinding>() {

    private lateinit var adapterLineaFile: AdapterLineaFile<Any>
    private lateinit var adapterGridFile: AdapterFileGrid<Any>

    //    private lateinit var adapterFileGrid: AdapterFileGrid<*>
    private val listDetailFolder = mutableListOf<Any>()
    private val listSearchDetailFolder = mutableListOf<Any>()
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
        binding.txtTitle.text = name
        //
        layoutLiner = PreferencesUtils.getBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, true)
        adapterLineaFile = AdapterLineaFile(
            requireContext(), listDetailFolder, db,
            isRecent = false,
            isShowingFragmentFavorite = true
        )
        adapterGridFile = AdapterFileGrid(
            requireContext(), listDetailFolder, db,
            isRecent = false,
            isShowingFragmentFavorite = true
        )
        if (layoutLiner) {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
            binding.rclItemList.layoutManager = LinearLayoutManager(requireContext())
            binding.rclItemList.adapter = adapterLineaFile
        } else {
            binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
            binding.rclItemList.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rclItemList.adapter = adapterGridFile
        }
        // clear animation rcl
        RecycleViewUtils.clearAnimation(binding.rclItemList)
    }

    override fun initData() {
        fileViewModel.listDetailPlaylist.observe(requireActivity()) {
            it?.let {
                binding.animationView.isVisible = true
                binding.animationLoading.isVisible = false
                listDetailFolder.clear()
                listSaver.clear()
                listDetailFolder.addAll(it)
                listSaver.addAll(it)
                checkEmpty()
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
        //back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // search
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                TODO("Not yet implemented")
                Log.d("Huy", "beforeTextChanged: ")
                if (listSearchDetailFolder.isNullOrEmpty()) {
                    listSearchDetailFolder.addAll(listDetailFolder)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != "") {
                    listDetailFolder.clear()
                    for (item in listSearchDetailFolder) {
                        when (item) {
                            is VideoModel -> {
                                item.fileName?.let {
                                    if (it.contains(s.toString(), true)) {
                                        listDetailFolder.add(item)
                                    }
                                }
                            }
                            is AudioModel -> {
                                item.songName?.let {
                                    if (it.contains(s.toString(), true)) {
                                        listDetailFolder.add(item)
                                    }
                                }
                            }
                        }
                    }
                    binding.rclItemList.adapter?.notifyDataSetChanged()
                    if (listDetailFolder.isEmpty()) {
                        binding.txtNoResult.visibility = View.VISIBLE
                    } else {
                        binding.txtNoResult.visibility = View.INVISIBLE
                    }
                } else {
                    listDetailFolder.clear()
                    binding.txtNoResult.visibility = View.INVISIBLE
                    listDetailFolder.addAll(listSaver)
                    binding.rclItemList.adapter?.notifyDataSetChanged()
                    listSearchDetailFolder.clear()
                }
            }

        })
        //delete
        adapterLineaFile.listenerDelete = {
            deleteFile(it)
        }
        adapterGridFile.listenerDelete = {
            deleteFile(it)
        }
        // show phone
        adapterLineaFile.listenerPlayOnPhone = {
            showPhonePopMenu(listDetailFolder[it], it)
        }
        adapterGridFile.listenerPlayOnPhone = {
            showPhonePopMenu(listDetailFolder[it], it)
        }
        // cast
        adapterLineaFile.listenerCastTo = {
            showCastPopMenu(listDetailFolder[it])
        }
        adapterGridFile.listenerCastTo = {
            showCastPopMenu(listDetailFolder[it])
        }
        // add favorite
        adapterLineaFile.listenerAddFavorite = {
            favoriteRecentDb(listDetailFolder[it], true, db)
            adapterLineaFile.notifyItemChanged(it)
        }
        adapterGridFile.listenerAddFavorite = {
            favoriteRecentDb(listDetailFolder[it], true, db)
            adapterGridFile.notifyItemChanged(it)
        }
        // remove favorite
        adapterLineaFile.listenerRemoveFavorite = {
            favoriteRecentDb(listDetailFolder[it], false, db)
            adapterLineaFile.notifyItemChanged(it)
        }
        adapterGridFile.listenerRemoveFavorite = {
            favoriteRecentDb(listDetailFolder[it], false, db)
            adapterGridFile.notifyItemChanged(it)
        }

        // change layout
        binding.icOrderLayout.setOnClickListener {
            if (binding.icOrderLayout.drawable.toBitmap()
                    .sameAs(resources.getDrawable(R.drawable.ic_arrange_liner).toBitmap())
            ) {
                PreferencesUtils.putBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, false)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_grid)
                binding.rclItemList.layoutManager = GridLayoutManager(requireContext(), 2)
                binding.rclItemList.adapter = adapterGridFile
            } else {
                PreferencesUtils.putBoolean(Vault.KEY_FAVORITE_FILE_LAYOUT, true)
                binding.icOrderLayout.setImageResource(R.drawable.ic_arrange_liner)
                binding.rclItemList.layoutManager = LinearLayoutManager(requireContext())
                binding.rclItemList.adapter = adapterLineaFile
            }
        }
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentDetailPlaylistBinding {
        return LayoutFragmentDetailPlaylistBinding.inflate(LayoutInflater.from(requireContext()))
    }


    private fun checkEmpty() {
        binding.animationView.isVisible = listDetailFolder.isNullOrEmpty()
    }

    private fun showCastOnClickVideoAudio(position: Int, isOnlyCast: Boolean = false) {
        if ((requireActivity() as MainActivity).isRouterConnected()) {
            var positionCast = 0
            val listVideoAudio = mutableListOf<Any>()
            for (item in listDetailFolder) {
                if (item !is ImageModel) {
                    listVideoAudio.add(item)
                    if (item == listDetailFolder[position]) {
                        positionCast = listVideoAudio.indexOf(item)
                    }
                }
            }
            (requireActivity() as MainActivity).fragmentCastVideoAudio(listVideoAudio, positionCast)
        } else {
            if (isOnlyCast) {
                (requireActivity() as MainActivity).showDialogScanRouter()
            } else {
                showVideoAudioToPhone(listDetailFolder[position], position)
            }
        }
    }

    private fun showVideoAudioToPhone(model: Any, position: Int) {
        // recent
        when (model) {
            is VideoModel -> {
                videoViewModel.updateFileRecent(model, db)
                //show
                (requireActivity() as MainActivity).showVideoAudio(listDetailFolder, position)
            }
            is AudioModel -> {
                audioViewModel.updateFileRecent(model, db)
                //show
                (requireActivity() as MainActivity).showVideoAudio(listDetailFolder, position)
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
        }
    }

    // fun delete
    private fun deleteFile(position: Int) {
        showRemoveRecent(
            getString(R.string.do_you_want_to_remove_from_list),
            getString(R.string.remove_file_from_list), requireContext()
        ) { done ->
            if (done) {
                requireActivity().runOnUiThread {
                    //update db
                    val data = db.serverDao().getPlayListByName(name)
                    data?.listMusic?.let {
                        when (val detail = listDetailFolder[position]) {
                            is VideoModel -> {
                                for (item in it) {
                                    if (item == detail.path) {
                                        (it as MutableList).remove(item)
                                        db.serverDao().insertPlayList(data)
                                    }
                                }
                            }
                            is AudioModel -> {
                                for (item in it) {
                                    if (item == detail.path) {
                                        (it as MutableList).remove(item)
                                        db.serverDao().insertPlayList(data)
                                    }
                                }
                            }
                        }
                    }
                    //
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    listSaver.remove(listDetailFolder[position])
                    listDetailFolder.removeAt(position)
                    adapterGridFile.notifyItemRemoved(position)
                    adapterLineaFile.notifyItemRemoved(position)
                    checkEmpty()
                    fileViewModel.getPlaylist(db)
                }
            } else {
                requireActivity().runOnUiThread {
                    DialogLoadingUtils.showDialogWaiting(requireContext(), false)
                    toast(getString(R.string.delete_failed))
                }
            }
        }
    }

    private fun showPhonePopMenu(data: Any, position: Int) {
        showVideoAudioToPhone(data, position)
    }

    private fun showCastPopMenu(data: Any) {
        when (data) {
            is VideoModel -> {
                showCastOnClickVideoAudio(listDetailFolder.indexOf(data), true)
            }
            is AudioModel -> {
                showCastOnClickVideoAudio(listDetailFolder.indexOf(data), true)
            }
        }
    }

    override fun onDestroy() {
        fileViewModel.listDetailPlaylist.value = null
        super.onDestroy()
    }

}