package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterQueueImages
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentCastPhotoBinding
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.server.LocalServer
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.utils.Utils
import com.ezstudio.smarttvcast.viewmodel.RouterViewModel
import com.ezteam.baseproject.fragment.BaseFragment
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException

class FragmentCastImages(
    private val listFile: MutableList<ImageModel>,
    private val position: Int,
    private var isNewCast: Boolean = true, var isShowPhone: Boolean = false
) : BaseFragment<LayoutFragmentCastPhotoBinding>() {

    private val listQueue = mutableListOf<ImageModel>()
    private var adapterQueueImages: AdapterQueueImages? = null

    //cast
    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var movieMetaData: MediaMetadata? = null

    //
    private var isViewCreate = false
    private var activityMain: MainActivity? = null
    private var isHidingCastMain = false

    // server
    var localFilePath = ""
    var formattedIpAddress = ""
    private val db by inject<AppDatabase>()
    private var urlLocal: String = ""
    private val viewModelRouter by lazy {
        ViewModelProvider(requireActivity()).get(RouterViewModel::class.java)
    }

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        isHidingCastMain = (activityMain?.isHideViewCastMain()) ?: false
        //
        listQueue.addAll(listFile)
        listQueue.forEach {
            if (it == listQueue[position]) {
                it.isSelected = true
            } else if (it.isSelected) {
                it.isSelected = false
            }
        }
        adapterQueueImages?.notifyDataSetChanged()
        //clear anim
        RecycleViewUtils.clearAnimation(binding.rclQueueImage)
        // start image
        binding.cardView.post {
            if (isNewCast) {
                startImage(listQueue[position])
            } else {
                Glide.with(requireContext()).load(listQueue[position].path).into(binding.image)
                isHidingCastMain = false
            }
            binding.cardView.invalidate()
        }
    }

    override fun initData() {
        setupCastListener()
        //
        mCastContext = CastContext.getSharedInstance(requireContext())
        mCastSession = mCastContext!!.sessionManager.currentCastSession
        adapterQueueImages = AdapterQueueImages(requireContext(), listQueue, db)
        binding.rclQueueImage.adapter = adapterQueueImages
        (binding.rclQueueImage.layoutManager as LinearLayoutManager).scrollToPosition(position)
        //
        viewModelRouter.isConnectedLiveData.value = null
        viewModelRouter.isConnectedLiveData.observe(requireActivity()) { bl ->
            if (isAdded) {
                bl?.let {
                    if (it) {
                        for (item in listQueue) {
                            if (item.isSelected) {
                                startImage(item)
                                break
                            }
                        }
                    } else {
                        toast(getString(R.string.disconnect))
                    }
                }
            }
        }
    }

    override fun initListener() {
        // click queue
        adapterQueueImages?.listenerClickItem = {
            for (item in listQueue) {
                if (item.isSelected) {
                    if (item != listQueue[it]) {
                        item.isSelected = false
                        listQueue[it].isSelected = true
                        adapterQueueImages?.notifyItemChanged(it)
                        adapterQueueImages?.notifyItemChanged(listQueue.indexOf(item))
                        startImage(listQueue[it])
                    }
                    break
                }
            }
        }
        // ic cast
        binding.icCasting.setOnClickListener {
            if (isAdded) {
                if ((requireActivity() as MainActivity).isRouterConnected()) {
                    (requireActivity() as MainActivity).showDialogDisconnectRouter()
                } else {
                    (requireActivity() as MainActivity).showDialogScanRouter()
                }
            }
        }
        // back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentCastPhotoBinding {
        return LayoutFragmentCastPhotoBinding.inflate(inflater, container, false)
    }


    private fun setupCastListener() {
        mSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {
            }

            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationConnected(castSession: CastSession) {
                mCastSession = castSession
            }

            private fun onApplicationDisconnected() {
                Log.d("Huy", "onApplicationDisconnected: ")
            }
        }
    }

    //
    @SuppressLint("SimpleDateFormat")
    private fun startImage(data: ImageModel) {
        Glide.with(requireContext()).load(data.path).into(binding.image)
        //
        if (activityMain?.isRouterConnected() == true && !isShowPhone) {
            DialogLoadingUtils.showDialogDownloading(requireContext(), true)
            isHidingCastMain = false
            loadImage(data)
            (requireActivity() as MainActivity).updateViewCastMain(
                data.path,
                null,
                data.name ?: "Unknow",
                0,
                0,
                true,
                true
            )
        }
    }

    private fun loadImage(data: ImageModel) {
        movieMetaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_PHOTO).apply {
            putString(MediaMetadata.KEY_SUBTITLE, "EZSTUDIO")
            putString(MediaMetadata.KEY_TITLE, data.path?.let { File(it).name } ?: "")
        }
//            movieMetaData.putString(MediaMetadata.KEY_TITLE, "STUDIO")
        localFilePath = data.path ?: ""
        startLocalServer(localFilePath, Utils.TYPE_IMAGE) { urlServer ->
            Log.d("Huy", "initListener: ${urlServer + data.path}")
            urlLocal = urlServer + data.path

            movieMetaData?.let {
                activityMain?.let { main ->
                    main.loadRemoteMedia(
                        0,
                        true,
                        main.getMediaInfo(urlLocal, 0, it, Utils.TYPE_IMAGE)
                    )
                }
            }
            //
            DialogLoadingUtils.showDialogDownloading(requireContext(), false)
        }
    }

    private fun startLocalServer(
        localFilePath: String,
        type: String,
        complete: ((String) -> Unit)
    ) {
        if (activityMain?.server != null) {
            activityMain?.server?.stop()
        }
        if (isAdded) {
            when (type) {
                Utils.TYPE_IMAGE -> {
                    activityMain?.server = LocalServer(localFilePath, "image")
                }
            }

            activityMain?.server?.let {
                Handler().postDelayed({
                    try {
                        it.start()
                    } catch (ioe: IOException) {
                    }
                    val wifiManager: WifiManager =
                        requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val ipAddress: Int = wifiManager.connectionInfo.ipAddress
                    val formatedIpAddress1 = String.format(
                        "%d.%d.%d.%d", ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff
                    )
                    formattedIpAddress =
                        "http://" + formatedIpAddress1 + ":" + it.listeningPort

                    complete.invoke(formattedIpAddress)
                }, 1000)
            }
        } else {
//            toast(getString(R.string.error_start_server))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreate = true
    }

    override fun onDestroyView() {
        isViewCreate = false
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityMain = requireActivity() as MainActivity
    }

    override fun onResume() {
        mSessionManagerListener?.let {
            mCastContext?.sessionManager?.addSessionManagerListener(
                it, CastSession::class.java
            )
        }
        (requireActivity() as MainActivity).setHideViewCastMain(true)
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mSessionManagerListener?.let {
            mCastContext?.sessionManager?.removeSessionManagerListener(
                it, CastSession::class.java
            )
        }
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).setHideViewCastMain(isHidingCastMain)
    }
}