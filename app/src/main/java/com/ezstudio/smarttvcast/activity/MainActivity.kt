package com.ezstudio.smarttvcast.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.broadcast.BroadCastChangeWifi
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.ActivityMainBinding
import com.ezstudio.smarttvcast.dialog.DialogDisConnectedRouter
import com.ezstudio.smarttvcast.dialog.DialogScanRouter
import com.ezstudio.smarttvcast.dialog.DialogWrongConnection
import com.ezstudio.smarttvcast.fragment.*
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.server.LocalServer
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezstudio.smarttvcast.utils.Utils
import com.ezstudio.smarttvcast.utils.Utils.customStatusBar
import com.ezstudio.smarttvcast.utils.WifiUtils
import com.ezstudio.smarttvcast.viewmodel.*
import com.ezteam.baseproject.activity.BaseActivity
import com.ezteam.baseproject.dialog.rate.DialogRating
import com.ezteam.baseproject.dialog.rate.DialogRatingState
import com.ezteam.baseproject.extensions.getHeightStatusBar
import com.ezteam.baseproject.fragment.BaseFragment
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import org.koin.android.ext.android.inject


class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private val mSessionManagerListener: SessionManagerListener<CastSession> =
        MySessionManagerListener()
    private var mediaRouteMenuItem: MenuItem? = null

    //    private var mQueueMenuItem: MenuItem? = null
    private var mIntroductoryOverlay: IntroductoryOverlay? = null
    private var mCastStateListener: CastStateListener? = null
    private var mMediaRouteSelector: MediaRouteSelector? = null
    private var mMediaRouter: MediaRouter? = null
    private lateinit var callBackRouter: MediaRouter.Callback

    //call back
    private var mediaClientCallback: RemoteMediaClient.Callback? = null
    private var mediaClientProcess: RemoteMediaClient.ProgressListener? = null
    private var isCastImage = false

    // router
    private var mediaRouterList: MediaRouter? = null
    private lateinit var nameRouterSelected: String

    // dialog
    private var dialogWrongConnection: DialogWrongConnection? = null
    private var dialogDisConnectionRouter: DialogDisConnectedRouter? = null
    private var dialogScanRouter: DialogScanRouter? = null

    // view model
    private val viewModelVideo by inject<VideoViewModel>()
    private val viewModelImage by inject<ImageViewModel>()
    private val viewModelAudio by inject<AudioViewModel>()
    private val fileViewModel by inject<FileViewModel>()
    private val viewModelRoute by lazy {
        ViewModelProvider(this).get(RouterViewModel::class.java)
    }
    var remoteMediaClient: RemoteMediaClient? = null
    private val db by inject<AppDatabase>()

    //server
    var server: LocalServer? = null

    //fragment
    private var fragmentRouter: FragmentRouter? = null
    private var fragmentCastVideoAudio: FragmentCastVideoAudio<*>? = null
    private var fragmentCastImage: FragmentCastImages? = null

    //broadcast
    private val broadCastChangeWifi = BroadCastChangeWifi()

    // list queue call back cast for main
    var listQueueMain: MutableList<*>? = null

    inner class MySessionManagerListener : SessionManagerListener<CastSession> {
        override fun onSessionEnded(session: CastSession, error: Int) {
            viewModelRoute.nameCastDevice
            if (session === mCastSession) {
                mCastSession = null
            }
            invalidateOptionsMenu()
            viewModelRoute.nameCastDevice.value = getString(R.string.cast_tv)
            Log.d("Huy", "onSessionEnded: ")

        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            mCastSession = session
            invalidateOptionsMenu()
            nameRouterSelected = mMediaRouter?.selectedRoute?.name ?: getString(R.string.cast_tv)
            viewModelRoute.nameCastDevice.value = nameRouterSelected
            //
            val myFragmentRouter = supportFragmentManager.findFragmentByTag("ROUTER")
            if (myFragmentRouter != null && myFragmentRouter.isVisible) {
                onBackPressed()
                // ads
                EzAdControl.getInstance(this@MainActivity).showAds()
            }
            dialogScanRouter?.dismiss()
            toast(getString(R.string.device_is_connected))
            viewModelRoute.isConnectedLiveData.value = true
        }


        override fun onSessionStarted(session: CastSession, sessionId: String) {
            toast(getString(R.string.device_is_connected))
            viewModelRoute.isConnectedLiveData.value = true
            mCastSession = session
            val myFragmentRouter = supportFragmentManager.findFragmentByTag("ROUTER")
            if (myFragmentRouter != null && myFragmentRouter.isVisible) {
//                fragmentMainReplace()
                onBackPressed()
                // ads
                EzAdControl.getInstance(this@MainActivity).showAds()
            }
            //
            val myFragmentShowPhone = supportFragmentManager.findFragmentByTag("SHOW_PHONE")
            if (myFragmentShowPhone != null && myFragmentShowPhone.isVisible) {
                viewModelRoute.isConnectedFromShowPhone.value = true
            }

            dialogScanRouter?.dismiss()
            invalidateOptionsMenu()
            //
            nameRouterSelected = mMediaRouter?.selectedRoute?.name ?: getString(R.string.cast_tv)
            viewModelRoute.nameCastDevice.value = nameRouterSelected
            Log.d("Huy", "onSessionStarted: ")
        }

        override fun onSessionStarting(session: CastSession) {
            viewModelRoute.nameCastDevice.value = getString(R.string.connecting)
            Log.d("Huy", "onSessionStarting: ")
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            viewModelRoute.isConnectedLiveData.value = false
            showDialogWrongConnection()
            fragmentRouter?.updateItem()
            dialogScanRouter?.updateItem()
            viewModelRoute.nameCastDevice.value = getString(R.string.cast_tv)
            Log.d("Huy", "onSessionStartFailed: ")
        }

        override fun onSessionEnding(session: CastSession) {
            viewModelRoute.nameCastDevice.value = getString(R.string.disconnecting)
            viewModelRoute.isConnectedLiveData.value = false
            Log.d("Huy", "onSessionEnding: ")
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            Log.d("Huy", "onSessionResuming: ")
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            showDialogWrongConnection()
            Log.d("Huy", "onSessionResumeFailed: ")
            viewModelRoute.nameCastDevice.value = getString(R.string.cast_tv)
            nameRouterSelected = getString(R.string.cast_tv)
            viewModelRoute.isConnectedLiveData.value = false
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.d("Huy", "onSessionSuspended: ")
        }

    }


    override fun initView() {
        customStatusBar(window, this)
        binding.layout.setPadding(0, getHeightStatusBar(), 0, 0)
        mCastStateListener =
            CastStateListener { newState ->
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay()
                }
            }
        mCastContext = CastContext.getSharedInstance(this)
        //
        fragmentMain()
        //souding enter
        binding.nameCast
    }

    override fun initData() {
        registerBroadCastWifi()
        //
        nameRouterSelected = getString(R.string.cast_tv)
        //    // media call back
        callBackRouter = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                super.onRouteAdded(router, route)
                updateScanRouter(router)
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                super.onRouteChanged(router, route)
                updateScanRouter(router)
            }

            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            }

            override fun onRouteUnselected(
                router: MediaRouter?,
                route: MediaRouter.RouteInfo?,
                reason: Int
            ) {
            }
        }
        //
        mediaClientCallback = object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                fragmentCastVideoAudio?.onStatusUpdated()
                if (!isCastImage) {
                    updateViewStateCastMain(remoteMediaClient?.isPlaying ?: false)
                }
            }
        }
        //
        mediaClientProcess = RemoteMediaClient.ProgressListener { p0, p1 ->
            fragmentCastVideoAudio?.onProcessListener(p0, p1)
            if (!isCastImage) {
                updateCurrentTimeCastMain(p0, p1)
                if (p0 in p1 - 51..p1) {
                    if (!isHideViewCastMain()) {
                        setHideViewCastMain(true)
                    }
                }
            }
        }
        mMediaRouter = MediaRouter.getInstance(applicationContext)
        // scan device
        startScan()
    }

    private fun registerBroadCastWifi() {
        broadCastChangeWifi.listenerStateChange = {
            dialogScanRouter?.updateStateWifi()
        }
        val intent = IntentFilter()
        intent.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intent.addAction(WifiManager.RSSI_CHANGED_ACTION)
        registerReceiver(broadCastChangeWifi, intent)
    }

    @SuppressLint("WrongConstant", "UseCompatLoadingForDrawables")
    override fun initListener() {

        binding.icPlayCast.setOnClickListener {
            when {
                binding.icPlayCast.drawable.toBitmap()
                    .sameAs(getDrawable(R.drawable.ic_stop_cast)?.toBitmap()) -> {
                    remoteMediaClient?.stop()
                    setHideViewCastMain(true)
                }
                else -> {
                    remoteMediaClient?.let {
                        if (it.isPlaying) {
                            binding.icPlayCast.setImageResource(R.drawable.ic_play_cast)
                            it.pause()
                        } else {
                            if (isRouterConnected()) {
                                binding.icPlayCast.setImageResource(R.drawable.ic_pause_cast)
                                it.play()
                            } else {
                                binding.icPlayCast.setImageResource(R.drawable.ic_play_cast)
                                it.pause()
                            }
                        }
                    }
                }
            }
        }
        //
        binding.layoutControl.setOnClickListener {
            listQueueMain?.let {
                for (item in it) {
                    when (item) {
                        is VideoModel -> {
                            if (item.path == binding.txtPath.text) {
                                fragmentCastVideoAudio(it, it.indexOf(item), isMainTo = true)
                                break
                            }
                        }
                        is AudioModel -> {
                            if (item.path == binding.txtPath.text) {
                                fragmentCastVideoAudio(it, it.indexOf(item), isMainTo = true)
                                break
                            }
                        }
                        is ImageModel -> {
                            if (item.path == binding.txtPath.text) {
                                fragmentCastImage(
                                    it as MutableList<ImageModel>,
                                    it.indexOf(item),
                                    isMainTo = true
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateScanRouter(router: MediaRouter) {
        mediaRouterList = router
        fragmentRouter?.mediaRouter = mediaRouterList
        dialogScanRouter?.mediaRouter = mediaRouterList
        fragmentRouter?.initData()
        dialogScanRouter?.initData()
    }

    fun startScan() {
        mMediaRouteSelector = MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.app_id)))
            .build()
        mMediaRouter?.addCallback(
            mMediaRouteSelector!!,
            callBackRouter,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
        )
    }

    fun removeCallBackScan() {
        mMediaRouter?.removeCallback(callBackRouter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.browse, menu)
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu!!,
            R.id.media_route_menu_item
        )
//
//        val mediaRouteActionProvider: MediaRouteActionProvider =
//            MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider
//        mMediaRouteSelector?.let {
//            mediaRouteActionProvider.routeSelector = it
//        }
//
//        //
//        mQueueMenuItem = menu.findItem(R.id.action_show_queue)
        showIntroductoryOverlay()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        menu?.findItem(R.id.action_show_queue)?.isVisible =
//            mCastSession != null && mCastSession?.isConnected ?: false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.media_route_menu_item_custom) {
//            handleCastButton()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return (mCastContext!!.onDispatchVolumeKeyEventBeforeJellyBean(event!!) or super.dispatchKeyEvent(
            event
        ))
    }

    override fun onResume() {
        mCastContext?.addCastStateListener(mCastStateListener!!)
        mCastContext?.sessionManager?.addSessionManagerListener(
            mSessionManagerListener, CastSession::class.java
        )
        intentToJoin()
        if (mCastSession == null) {
            mCastSession = CastContext.getSharedInstance(this).sessionManager.currentCastSession
        }
        if (isRouterConnected()) {
            nameRouterSelected = mMediaRouter?.selectedRoute?.name ?: getString(R.string.cast_tv)
            viewModelRoute.nameCastDevice.value = nameRouterSelected
        } else {
            viewModelRoute.nameCastDevice.value = getString(R.string.cast_tv)
        }
//        if (mQueueMenuItem != null) {
//            mQueueMenuItem?.isVisible = mCastSession != null && mCastSession!!.isConnected
//        }


        super.onResume()
    }

    override fun onPause() {
        mCastContext?.removeCastStateListener(mCastStateListener!!)
        mCastContext?.sessionManager?.removeSessionManagerListener(
            mSessionManagerListener, CastSession::class.java
        )
        removeCallBackScan()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        unregisterReceiver(broadCastChangeWifi)
        unRegisterCallBackRemoteClient()
    }

    private fun intentToJoin() {
        val intent: Intent = intent
        val intentToJoinUri: Uri = Uri.parse("https://castvideos.com/cast/join")

        if (intent.data != null && (intent.data == intentToJoinUri)) {
            mCastContext?.sessionManager?.startSession(intent)

        }
    }

    override fun viewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(LayoutInflater.from(this))
    }

    private fun showIntroductoryOverlay() {
        mIntroductoryOverlay?.let {
            it.remove()
        }
        if (mediaRouteMenuItem != null && mediaRouteMenuItem!!.isVisible) {
            Handler().post {
                mIntroductoryOverlay = IntroductoryOverlay.Builder(
                    this, mediaRouteMenuItem!!
                )
                    .setTitleText(getString(R.string.introducing_cast))
                    .setOverlayColor(R.color.color_03A9F4)
                    .setSingleTime()
                    .setOnOverlayDismissedListener { mIntroductoryOverlay = null }
                    .build()
                mIntroductoryOverlay?.show()
            }
        }
    }

    fun handleCastButton() {
        val fm = supportFragmentManager
        if (mCastSession != null && mCastSession!!.isConnected) {
//            val fragment = MediaRouteDialogFactory.getDefault().onCreateControllerDialogFragment()
//            fragment.show(fm, "androidx.mediarouter.app:MediaRouteChooserDialogFragment")
            showDialogDisconnectRouter()
        } else {
//            val f = MediaRouteDialogFactory.getDefault().onCreateChooserDialogFragment()
//            f.routeSelector = mMediaRouteSelector
//            f.show(fm, "androidx.mediarouter.app:MediaRouteChooserDialogFragment")
            if (WifiUtils.checkEnable(this)) {
                if (!WifiUtils.isConnected(this)) {
                    showDialogScanRouter()
                } else {
                    fragmentRouter()
                }
            } else {
                showDialogScanRouter()
            }
        }
    }

    fun showDialogDisconnectRouter() {
        dialogDisConnectionRouter ?: let {
            dialogDisConnectionRouter =
                DialogDisConnectedRouter(this, R.style.StyleDialog, nameRouterSelected)
        }
        dialogDisConnectionRouter?.listenerYes = {
            mCastContext?.sessionManager?.endCurrentSession(true)
        }
        dialogDisConnectionRouter?.show()
    }

    fun showDialogScanRouter() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        dialogScanRouter = DialogScanRouter(this, mMediaRouter, displayMetrics.heightPixels)

        dialogScanRouter?.listenerYes = {

        }
        dialogScanRouter?.listenerRescan = {
            removeCallBackScan()
            startScan()
        }
        dialogScanRouter?.show()
        dialogScanRouter?.setOnDismissListener {
            dialogScanRouter = null
        }
    }

    fun isRouterConnected(): Boolean {
        return mCastSession?.isConnected ?: false
    }

    private fun createDialogChooserRouter() {

    }

    private fun showDialogWrongConnection() {
        dialogWrongConnection =
            DialogWrongConnection(this)
        dialogWrongConnection?.setOnDismissListener {
            dialogWrongConnection = null
        }
        dialogWrongConnection?.show()
    }

    @SuppressLint("WrongConstant")
    private fun fragmentMain(isReplace: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        val fragment = FragmentMain()
        fragment.onClickMenu = {
            replaceFragment(FragmentMenu(), "Menu")
        }
        if (isReplace) {
            transaction.replace(R.id.layout_fragment, fragment, "MAIN")
            transaction.commit()
        } else {
            transaction.add(R.id.layout_fragment, fragment, "MAIN")
            transaction.commitAllowingStateLoss()
        }
    }

    fun fragmentVideo() {
        if (!aVoidDoubleClick()) {
            requestPermission({
                if (it) {
                    viewModelVideo.videos.value = (null)
                    replaceFragment(FragmentVideo(), "VIDEO", true)
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    }

    fun fragmentImage() {
        if (!aVoidDoubleClick()) {
            requestPermission({
                if (it) {
                    viewModelImage.imageStoreLiveData.value = (null)
                    replaceFragment(FragmentImage(), "IMAGE", true)
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    }

    fun fragmentAudio() {
        if (!aVoidDoubleClick()) {
            requestPermission({
                if (it) {
                    viewModelAudio.audios.value = (null)
                    replaceFragment(FragmentAudio(), "AUDIO", true)
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    }

    fun fragmentRecentFile() {
        if (!aVoidDoubleClick()) {
            requestPermission({
                if (it) {
                    fileViewModel.getFileRecent(db)
                    replaceFragment(FragmentRecentFile(), "RECENT")
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun fragmentFavoriteFile() {
        if (!aVoidDoubleClick()) {
            requestPermission({
                if (it) {
                    fileViewModel.getFileFavorite(db)
                    replaceFragment(FragmentFavoriteFile(), "FAVORITE")
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun fragmentPlayListFile() {
        if (!aVoidDoubleClick()) {
            fileViewModel.getPlaylist(db)
            replaceFragment(FragmentPlayList(), "PLAYLIST")
        }
    }

    fun fragmentDetailPlaylist(list: MutableList<String>, name: String) {
        if (!aVoidDoubleClick()) {
            fileViewModel.getDetailPlaylist(db, list)
            replaceFragment(FragmentDetailPlaylist(name), "DETAIL_PLAYLIST")
        }
    }


    fun <T> fragmentCastVideoAudio(
        listVideoAudio: MutableList<T>,
        position: Int,
        isMainTo: Boolean = false,
        isBackStack: Boolean = true
    ) {
        if (!aVoidDoubleClick()) {
            // add recent file
            when (val data = listVideoAudio[position]) {
                is VideoModel -> {
                    viewModelVideo.updateFileRecent(data, db)
                }
                is AudioModel -> {
                    viewModelAudio.updateFileRecent(data, db)
                }
            }
            if (!isMainTo) {
                listQueueMain?.clear()
                listQueueMain = listVideoAudio.toMutableList()
                fragmentCastVideoAudio = FragmentCastVideoAudio(listVideoAudio, position)
            } else {
                fragmentCastVideoAudio = FragmentCastVideoAudio(listVideoAudio, position, false)
            }
            if (isBackStack) {
                replaceFragment(fragmentCastVideoAudio!!, "CAST_VIDEO")
            } else {
                replaceFragment(fragmentCastVideoAudio!!, "CAST_VIDEO_NOT_BACK")
            }
        }

    }

    fun fragmentCastImage(
        listImage: MutableList<ImageModel>,
        position: Int,
        isMainTo: Boolean = false, isShowPhone: Boolean = false
    ) {
        if (!aVoidDoubleClick()) {
            // add recent
            viewModelImage.updateFileRecent(listImage[position], db)
            //show
            if (!isMainTo) {
                listQueueMain?.clear()
                listQueueMain = listImage.toMutableList()
                fragmentCastImage = FragmentCastImages(listImage, position, true, isShowPhone)
            } else {
                fragmentCastImage = FragmentCastImages(listImage, position, false, isShowPhone)
            }
            replaceFragment(fragmentCastImage!!, "CAST_IMAGE")
        }
    }

    private fun fragmentRouter() {
        fragmentRouter ?: let {
            fragmentRouter = FragmentRouter(mediaRouterList)
        }
        fragmentRouter?.let {
            startScan()
            replaceFragment(it, "ROUTER", true)
        }
    }


    fun <T> showVideoAudio(listVideoAudio: MutableList<T>, position: Int) {
        if (!aVoidDoubleClick()) {
            replaceFragment(FragmentShowPhoneVideoAudio(listVideoAudio, position), "SHOW_PHONE")
        }
//        val intent = Intent(this, ShowVideoAudio::class.java)
//        intent.putExtra(Vault.KEY_VIDEO_AUDIO_SHOW_PHONE, path)
//        startActivity(intent)
    }


    private fun replaceFragment(
        fragment: BaseFragment<*>,
        tag: String,
        backStack: Boolean = true
    ) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.apply {
            setCustomAnimations(
                R.anim.pull_in_right,
                R.anim.push_out_left,
                R.anim.pull_in_left,
                R.anim.push_out_right
            )
            add(R.id.layout_fragment, fragment, tag)
            if (backStack) addToBackStack(tag)
            commit()
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateViewCastMain(
        path: String?,
        img: Bitmap?,
        name: String,
        currentTime: Long,
        duration: Long,
        isPlaying: Boolean,
        isImage: Boolean = false,
        isAudio: Boolean = false
    ) {
        isCastImage = isImage
        Glide.with(this).load(if (isAudio) img ?: R.drawable.ic_audio else path)
            .into(binding.imgFileCast)
        binding.nameCast.text = name
        binding.txtPath.text = path ?: ""
        if (isImage) {
            Log.d("Huy", "updateViewCastMain: View Image")
            binding.content.text = mMediaRouter?.selectedRoute?.name ?: getString(R.string.cast_tv)
            binding.icPlayCast.setImageResource(R.drawable.ic_stop_cast)
        } else {
            binding.content.text =
                "${Utils.formatDurationLong(currentTime)}/${Utils.formatDurationLong(duration)}"
            binding.icPlayCast.setImageResource(if (isPlaying) R.drawable.ic_pause_cast else R.drawable.ic_play_cast)
        }
    }

    fun updateViewStateCastMain(isPlaying: Boolean) {
        binding.icPlayCast.setImageResource(if (isPlaying) R.drawable.ic_pause_cast else R.drawable.ic_play_cast)
    }

    @SuppressLint("SetTextI18n")
    fun updateCurrentTimeCastMain(currentTime: Long, duration: Long) {
        binding.content.text =
            "${Utils.formatDurationLong(currentTime)}/${Utils.formatDurationLong(duration)}"
    }

    fun updateDurationCastMain() {

    }

    fun setHideViewCastMain(isHide: Boolean) {
        binding.layoutControl.isVisible = !isHide
    }

    fun isHideViewCastMain(): Boolean {
        return !binding.layoutControl.isShown
    }

    override fun onBackPressed() {
        val myFragmentCast = supportFragmentManager.findFragmentByTag("CAST_VIDEO_NOT_BACK")
        if (myFragmentCast != null && myFragmentCast.isVisible) {
            fragmentMain(true)
        } else {
//            val myFragmentMain = supportFragmentManager.findFragmentByTag("MAIN")
            supportFragmentManager.backStackEntryCount
            if (supportFragmentManager.backStackEntryCount == 0) {
                showAppRating(true) {
                    finishAffinity()
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun registerCallBackRemoteClient() {
        //
        mediaClientProcess?.let {
            remoteMediaClient?.addProgressListener(it, 50)
        }
        mediaClientCallback?.let {
            remoteMediaClient?.registerCallback(it)
        }
    }

    private fun unRegisterCallBackRemoteClient() {
        //
        mediaClientProcess?.let {
            remoteMediaClient?.removeProgressListener(it)
        }
        mediaClientCallback?.let {
            remoteMediaClient?.registerCallback(it)
        }
    }

    fun loadRemoteMedia(currentTime: Long, autoPlay: Boolean, mediaInfo: MediaInfo) {
        if (mCastSession == null) {
            return
        }
        remoteMediaClient = mCastSession?.remoteMediaClient ?: return
        remoteMediaClient?.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {

//                remoteMediaClient?.unregisterCallback(this)
            }
        })
        remoteMediaClient?.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(autoPlay)
                .setCurrentTime(currentTime).build()
        )
        //registerCallBackRemoteClient
        registerCallBackRemoteClient()
    }

    fun getMediaInfo(
        urlLocal: String,
        duration: Long,
        movieMetaData: MediaMetadata,
        type: String
    ): MediaInfo {
        var contentType = ""
        when (type) {
            Utils.TYPE_VIDEO -> {
                contentType = "videos/${FileUtils.getFileExtension(urlLocal)}"
            }
            Utils.TYPE_AUDIO -> {
                contentType = "audios/${FileUtils.getFileExtension(urlLocal)}"
            }
            Utils.TYPE_IMAGE -> {
                contentType = "image/${FileUtils.getFileExtension(urlLocal)}"
            }
        }
        return MediaInfo.Builder(urlLocal)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(movieMetaData)
//            .setStreamDuration(duration)
//            .setCustomData(jsonObj)
            .build()
    }

    private fun showAppRating(isHardShow: Boolean, complete: (Boolean) -> Unit) {
        DialogRating.ExtendBuilder(this)
            .setHardShow(isHardShow)
            .setListener { status ->
                when (status) {
                    DialogRatingState.RATE_BAD -> {
                        toast(resources.getString(R.string.thank_for_rate))
                        complete(false)
                    }
                    DialogRatingState.RATE_GOOD -> {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data =
                            Uri.parse("market://details?id=$packageName")
                        startActivity(intent)
                        complete(true)
                    }
                    DialogRatingState.COUNT_TIME -> complete(false)
                }
            }
            .build()
            .show()
    }


}