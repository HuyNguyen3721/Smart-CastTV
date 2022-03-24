package com.ezstudio.smarttvcast.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.databinding.LayoutFragmentMenuBinding


class FragmentMenu : BaseCastFragment<LayoutFragmentMenuBinding>() {

    override fun initView() {
//        binding.layoutTitle.setPadding(0, getHeightStatusBar(requireActivity()), 0, 0)
    }

    override fun initData() {
    }

    override fun initListener() {
        /// back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // recent
        binding.layoutRecent.setOnClickListener {
            (requireActivity() as MainActivity).fragmentRecentFile()
        }
        // favorite
        binding.layoutFavorite.setOnClickListener {
            (requireActivity() as MainActivity).fragmentFavoriteFile()
        }
        // playlist
        binding.layoutPlaylist.setOnClickListener {
            (requireActivity() as MainActivity).fragmentPlayListFile()
        }
        //rate us
        binding.layoutRate.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("market://details?id=com.ezmobi.smarttvcast")
            startActivity(intent)
        }
        //
        binding.layoutMirror.setOnClickListener {
            try {
                startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
            } catch (e: ActivityNotFoundException) {
                try {
                    startActivity(Intent("com.samsung.wfd.LAUNCH_WFD_PICKER_DLG"))
                } catch (e2: Exception) {
                    startActivity(Intent("android.settings.CAST_SETTINGS"))
                    Toast.makeText(
                        requireActivity(),
                        "Device not supported",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        //
        binding.layoutMoreApp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("https://play.google.com/store/apps/developer?id=EZ+MOBI+CO.,+LTD")
            startActivity(intent)
        }

    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentMenuBinding {
        return LayoutFragmentMenuBinding.inflate(inflater, container, false)
    }
}