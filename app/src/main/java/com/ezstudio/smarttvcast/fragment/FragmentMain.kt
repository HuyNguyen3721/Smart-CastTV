package com.ezstudio.smarttvcast.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.databinding.LayoutFragmentMainBinding
import com.ezstudio.smarttvcast.utils.Utils
import com.ezstudio.smarttvcast.viewmodel.RouterViewModel
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.listenner.NativeAdListener
import com.google.android.gms.ads.ez.nativead.AdmobNativeAdView
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.OnTargetListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.Circle

class FragmentMain : BaseCastFragment<LayoutFragmentMainBinding>() {

    var onClickMenu: (() -> Unit)? = null

    private val viewModelRouter by lazy {
        ViewModelProvider(requireActivity()).get(RouterViewModel::class.java)
    }

    companion object {
        var isVisible = false
    }

    override fun initView() {
//        binding.layoutIcTitle.setPadding(0, getHeightStatusBar(requireActivity()), 0, 0)
        //anim sound
        animationSounding()
        //load ads
        loadAds()
        // intro view
        if (PreferencesUtils.getBoolean(Utils.FIRST_INTRO, true)) {
            PreferencesUtils.putBoolean(Utils.FIRST_INTRO, false)
            binding.layoutEnterCast.sounding.post {
                initIntroView()
            }
        }
    }

    private fun loadAds() {
        AdmobNativeAdView.getNativeAd(
            requireContext(),
            R.layout.native_admob_main,
            object : NativeAdListener() {
                override fun onError() {
                }

                override fun onLoaded(nativeAd: RelativeLayout?) {
                    nativeAd?.let {
                        if (it.parent != null) {
                            (it.parent as ViewGroup).removeView(it)
                        }
                        binding.adsView.addView(it)
                    }
                }

                override fun onClickAd() {
                }
            })
    }

    override fun initData() {
        viewModelRouter.nameCastDevice.observe(requireActivity()) {
            it?.let {
                binding.layoutEnterCast.nameDevice.text = it
            }
        }
    }

    override fun initListener() {

        binding.icMenu.setOnClickListener {
            onClickMenu?.invoke()
        }
        binding.video.setOnClickListener {
            (requireActivity() as MainActivity).fragmentVideo()
        }
        binding.images.setOnClickListener {
            (requireActivity() as MainActivity).fragmentImage()
        }
        binding.audios.setOnClickListener {
            (requireActivity() as MainActivity).fragmentAudio()
        }
        binding.mirror.setOnClickListener {
            openScreenMirror()
        }
        binding.layoutEnterCast.layoutSound.setOnClickListener {
            if (!aVoidDoubleClick()) {
                (requireActivity() as MainActivity).handleCastButton()
            }
        }
        //rate
        binding.layoutRate.setOnClickListener {
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.data = Uri.parse("market://details?id=com.ezmobi.office.reader")
//            startActivity(intent)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("market://details?id=com.ezmobi.smarttvcast")
            startActivity(intent)
        }
    }

    private fun openScreenMirror() {
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

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentMainBinding {
        return LayoutFragmentMainBinding.inflate(inflater, container, false)
    }


    private fun animationSounding() {
        binding.layoutEnterCast.sounding.startAnimation(
            AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.sounding
            )
        )
        val handle = Handler()
        handle.postDelayed({
            binding.layoutEnterCast.soundingSecond.startAnimation(
                AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.sounding
                )
            )
        }, 1200)
    }

    override fun onStop() {
        FragmentMain.isVisible = false
        super.onStop()
    }

    override fun onResume() {
        FragmentMain.isVisible = true
        super.onResume()
    }

    private fun initIntroView() {
        val firstRoot = FrameLayout(requireContext())
        val first = layoutInflater.inflate(R.layout.layout_target, firstRoot)
        val firstTarget = Target.Builder()
            .setAnchor(binding.layoutEnterCast.sounding)
            .setShape(Circle(100f))
            .setOverlay(first)
            .setOnTargetListener(object : OnTargetListener {
                override fun onStarted() {
                }

                override fun onEnded() {
                }
            })
            .build()
        // create spotlight
        val spotlight = Spotlight.Builder(requireActivity())
            .setTargets(mutableListOf(firstTarget))
            .setBackgroundColorRes(R.color.spotlightBackground)
            .setDuration(800L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() {
                }

                override fun onEnded() {
                }
            })
            .build()

        spotlight.start()

        first.findViewById<View>(R.id.btn_got_it).setOnClickListener {
            spotlight.finish()
        }
        first.findViewById<View>(R.id.layout_target).setOnClickListener {
            // to do some thing
        }
    }
}