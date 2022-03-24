package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterQueue
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentCastVideoBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.VideoModel
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException
import kotlin.random.Random

class FragmentCastVideoAudio<T>(
    private val listFile: MutableList<T>,
    private val position: Int,
    private var isNewCast: Boolean = true
) :
    BaseFragment<LayoutFragmentCastVideoBinding>() {

    private val listQueue = mutableListOf<T>()

    //cast
    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var movieMetaData: MediaMetadata? = null

    //
    var localFilePath = ""
    var formattedIpAddress = ""
    private var adapterQueue: AdapterQueue<T>? = null
    private val db by inject<AppDatabase>()
    private var urlLocal: String = ""
    private val viewModelRouter by lazy {
        ViewModelProvider(requireActivity()).get(RouterViewModel::class.java)
    }
    private var isViewCreate = false
    var complete = false

    //controller
    private var durationTimer = 0L
    private var countDownTimer: CountDownTimer? = null
    private var isTouchSeekbar = false
    private var activityMain: MainActivity? = null
    private var isHidingCastMain = false

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

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //
        isHidingCastMain = (activityMain?.isHideViewCastMain()) ?: false
        //
        listQueue.addAll(listFile)
        listQueue.forEach {
            when (it) {
                is VideoModel -> {
                    if (it == listQueue[position]) {
                        binding.txtTitle.text = it.fileName
                        it.isSelected = true
                    } else if (it.isSelected) {
                        it.isSelected = false
                    }
                }
                is AudioModel -> {
                    if (it == listQueue[position]) {
                        binding.txtTitle.text = it.songName
                        it.isSelected = true
                    } else if (it.isSelected) {
                        it.isSelected = false
                    }
                }
            }
        }
        //
        //clear anim
        RecycleViewUtils.clearAnimation(binding.bottomSheet.rclQueue)
        //
        DialogLoadingUtils.showDialogDownloading(requireContext(), true)
        // start video
        if (isNewCast) {
            startVideo(listQueue[position])
        } else {
            when (val data = listQueue[position]) {
                is VideoModel -> {
                    binding.txtTitle.text = data.fileName
                    binding.txtDuration.text = Utils.formatDurationLong(data.duration)
                    binding.seekbarController.max = data.duration.toInt()
                }
                is AudioModel -> {
                    binding.txtTitle.text = data.songName
                    binding.txtDuration.text = Utils.formatDurationLong(data.duration)
                    binding.seekbarController.max = data.duration.toInt()
                }
            }
            DialogLoadingUtils.showDialogDownloading(requireContext(), false)
            isHidingCastMain = false
        }
    }


    @SuppressLint("SimpleDateFormat", "UseCompatLoadingForDrawables")
    override fun initData() {
        setupCastListener()
        //
        mCastContext = CastContext.getSharedInstance(requireContext())
        mCastSession = mCastContext!!.sessionManager.currentCastSession
        // set up mute
        setStateMuteSound()
        // view bottom sheet queue
        adapterQueue = AdapterQueue(requireContext(), listQueue, db)
        binding.bottomSheet.apply {
            txtNumberQueue.text = listQueue.size.toString()
            txtMode.text = getString(R.string.repeat_all)
            icRepeatQueue.setImageResource(R.drawable.ic_repeat)
            rclQueue.adapter = adapterQueue
        }
        viewModelRouter.isConnectedLiveData.value = null
        viewModelRouter.isConnectedLiveData.observe(requireActivity()) { bl ->
            if (isAdded) {
                bl?.let {
                    if (it) {
                        for (item in listQueue) {
                            when (item) {
                                is VideoModel -> {
                                    if (item.isSelected) {
                                        if (binding.seekbarController.progress <= item.duration) {
                                            startVideo(
                                                item,
                                                false,
                                                binding.seekbarController.progress
                                            )
                                        } else {
                                            DialogLoadingUtils.showDialogDownloading(
                                                requireContext(),
                                                true
                                            )
                                            startVideo(item)
                                        }
                                        break
                                    }
                                }
                                is AudioModel -> {
                                    if (item.isSelected) {
                                        if (binding.seekbarController.progress <= item.duration) {
                                            startVideo(
                                                item,
                                                false,
                                                binding.seekbarController.progress
                                            )
                                        } else {
                                            DialogLoadingUtils.showDialogDownloading(
                                                requireContext(),
                                                true
                                            )
                                            startVideo(item)
                                        }
                                        break
                                    }
                                }
                            }
                        }
//                when {
//                    binding.icPlay.drawable.toBitmap()
//                        .sameAs(resources.getDrawable(R.drawable.ic_pause).toBitmap()) -> {
//                        Log.d("Huy", "ic_pause: ")
//                        remoteMediaClient?.pause()
//                    }
//                    else -> {
//                        Log.d("Huy", "play: ")
//                        remoteMediaClient?.play()
//                    }
//                }
                    } else {
                        activityMain?.remoteMediaClient?.pause()
                        binding.icPlay.setImageResource(R.drawable.ic_pause)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    override fun initListener() {
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
        // stop
        binding.layoutStop.setOnClickListener {
            activityMain?.remoteMediaClient?.let {
                it.stop()
                activityMain?.setHideViewCastMain(true)
                // ads
                EzAdControl.getInstance(requireActivity()).showAds()
                //
                requireActivity().onBackPressed()
            }
        }
        // play/pause
        binding.icPlay.setOnClickListener {
            statePlayPause()
        }
        // resends 15 seconds
        binding.icBackward15Seconds.setOnClickListener {
            activityMain?.remoteMediaClient?.let {
                if (durationTimer - 15000 >= 0) {
                    it.seek(durationTimer - 15000)

                } else {
                    it.seek(0)
                }
            }
        }
        binding.icNextward15Seconds.setOnClickListener {
            val data = listQueue[position]
            activityMain?.remoteMediaClient?.let {
                when (data) {
                    is VideoModel -> {
                        if (durationTimer + 15000 >= data.duration) {
                            it.seek(data.duration)
                        } else {
                            it.seek(durationTimer + 15000)
                        }
                    }
                    is AudioModel -> {
                        if (durationTimer + 15000 >= data.duration) {
                            it.seek(data.duration)
                        } else {
                            it.seek(durationTimer + 15000)
                        }
                    }
                }

            }
        }
        // volume
        binding.icUp.setOnTouchListener { v, event ->
            when (event!!.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    countDownTimer = object : CountDownTimer(10000000, 200) {
                        override fun onTick(millisUntilFinished: Long) {
                            val volume = mCastSession?.volume ?: 0.0
                            binding.icVolume.visibility = View.INVISIBLE
                            binding.txtValueVolume.visibility = View.VISIBLE
                            val volumeUp = if (volume + 0.01 > 1) 1.0 else volume + 0.01
                            binding.txtValueVolume.text = "${(volumeUp * 100).toInt()}%"
                            mCastSession?.volume = volumeUp
                        }

                        override fun onFinish() {
                        }
                    }.start()
                }
                MotionEvent.ACTION_UP -> {
                    countDownTimer?.cancel()
                    //
                    val volume = mCastSession?.volume ?: 0.0
                    binding.icVolume.visibility = View.INVISIBLE
                    binding.txtValueVolume.visibility = View.VISIBLE
                    val volumeUp = if (volume + 0.01 > 1) 1.0 else volume + 0.01
                    binding.txtValueVolume.text = "${(volumeUp * 100).toInt()}%"
                    mCastSession?.volume = volumeUp
                    binding.icUp.postDelayed({
                        binding.icVolume.visibility = View.VISIBLE
                        binding.txtValueVolume.visibility = View.INVISIBLE
                    }, 700)
                }
                MotionEvent.ACTION_MOVE -> {

                }
            }
            setStateMuteSound()
            true
        }
        binding.icDown.setOnTouchListener { v, event ->
            when (event!!.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    countDownTimer = object : CountDownTimer(10000000, 200) {
                        override fun onTick(millisUntilFinished: Long) {
                            val volume = mCastSession?.volume ?: 0.0
                            binding.icVolume.visibility = View.INVISIBLE
                            binding.txtValueVolume.visibility = View.VISIBLE
                            val volumeDown = if (volume - 0.01 < 0) 0.0 else volume - 0.01
                            binding.txtValueVolume.text = "${(volumeDown * 100).toInt()}%"
                            mCastSession?.volume = volumeDown
                        }

                        override fun onFinish() {
                        }
                    }.start()
                }
                MotionEvent.ACTION_UP -> {
                    countDownTimer?.cancel()
                    //
                    val volume = mCastSession?.volume ?: 0.0
                    binding.icVolume.visibility = View.INVISIBLE
                    binding.txtValueVolume.visibility = View.VISIBLE
                    val volumeDown = if (volume - 0.01 < 0) 0.0 else volume - 0.01
                    binding.txtValueVolume.text = "${(volumeDown * 100).toInt()}%"
                    mCastSession?.volume = volumeDown
                    binding.icUp.postDelayed({
                        binding.icVolume.visibility = View.VISIBLE
                        binding.txtValueVolume.visibility = View.INVISIBLE
                    }, 700)
                }
                MotionEvent.ACTION_MOVE -> {

                }
            }
            setStateMuteSound()
            true
        }
        // back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // change seekbar
        binding.seekbarController.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekbar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { s ->
                    activityMain?.remoteMediaClient?.seek(s.progress.toLong())
                }
                isTouchSeekbar = false
            }

        })
        binding.seekbarController.setOnClickListener { }

        // queue bottom sheet
        val bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.layoutBottomSheet)
        //
        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        requireActivity().runOnUiThread {
                            val anim = binding.bgBtnSheet.animate()
                            anim.duration = 100
                            anim.alpha(0F)
                            anim.withEndAction {
                                binding.bgBtnSheet.isVisible = false
                            }
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })
        // queue
        binding.layoutQueue.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.bgBtnSheet.apply {
                    animate().alpha(0F).withEndAction {
                        isVisible = false
                    }
                }

            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.bgBtnSheet.apply {
                    isVisible = true
                    animate().alpha(1F)
                }
                for (item in listQueue) {
                    when (item) {
                        is VideoModel -> {
                            if (item.isSelected) {
                                (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                    listQueue.indexOf(item)
                                )
                                break
                            }
                        }
                        is AudioModel -> {
                            if (item.isSelected) {
                                (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                    listQueue.indexOf(item)
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
        binding.bgBtnSheet.setOnClickListener {
            if (binding.bgBtnSheet.alpha == 1F) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.bgBtnSheet.apply {
                    animate().alpha(0F).withEndAction {
                        isVisible = false
                    }
                }
            }
        }
        binding.bottomSheet.icRepeatQueue.setOnClickListener {
            changeMode()
        }
        // delete queue
        adapterQueue?.listenerDelete = {
            when (val data = listQueue[it]) {
                is VideoModel -> {
                    if (data.isSelected) {
                        if (isAdded) {
                            toast(getString(R.string.cant_delete_file_casting))
                        }
                    } else {
                        listQueue.removeAt(it)
                        adapterQueue?.notifyItemRemoved(it)
                        //update number
                        binding.bottomSheet.txtNumberQueue.text = listQueue.size.toString()
                    }
                }
                is AudioModel -> {
                    if (data.isSelected) {
                        if (isAdded) {
                            toast(getString(R.string.cant_delete_file_casting))
                        }
                    } else {
                        listQueue.removeAt(it)
                        adapterQueue?.notifyItemRemoved(it)
                        //update number
                        binding.bottomSheet.txtNumberQueue.text = listQueue.size.toString()
                    }
                }
            }

        }
        //click queue
        adapterQueue?.listenerClick = {
            DialogLoadingUtils.showDialogDownloading(requireContext(), true)
            binding.seekbarController.progress = 0
            val data = listQueue[it]
            for (item in listQueue) {
                if (item == data) {
                    when (item) {
                        is VideoModel -> {
                            if (item.isSelected) {
                                startVideo(item, true)
                            } else {
                                item.isSelected = true
                                startVideo(item)
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(item))
                            }
                        }
                        is AudioModel -> {
                            if (item.isSelected) {
                                startVideo(item, true)
                            } else {
                                item.isSelected = true
                                startVideo(item)
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(item))
                            }
                        }
                    }

                } else {
                    when (item) {
                        is VideoModel -> {
                            if (item.isSelected) {
                                item.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(item))
                            }
                        }
                        is AudioModel -> {
                            if (item.isSelected) {
                                item.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(item))
                            }
                        }
                    }
                }
            }
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        //mode
        binding.layoutMode.setOnClickListener {
            changeMode()
        }
        // previous
        binding.icPrevious.setOnClickListener {
            previousFileCast()
        }
        //next
        binding.icNext.setOnClickListener {
            nextFileCast()
        }
        // mute
        binding.layoutMute.setOnClickListener {
            if (mCastSession?.isMute == true) {
                mCastSession?.isMute = false
                binding.icMute.setImageResource(R.drawable.ic_mute)
                binding.txtMute.text = getString(R.string.mute)
            } else {
                mCastSession?.isMute = true
                binding.icMute.setImageResource(R.drawable.ic_sound)
                binding.txtMute.text = getString(R.string.sound)
            }
        }

    }

    private fun nextFileCast() {
        if (isAdded) {
            DialogLoadingUtils.showDialogDownloading(requireContext(), true)
            Handler().postDelayed({
                for (dataQueue in listQueue) {

                    when (dataQueue) {
                        is VideoModel -> {
                            if (dataQueue.isSelected) {
                                val position = getPositionModeQueue(dataQueue)
                                if (position == listQueue.indexOf(dataQueue)) {
                                    startVideo(listQueue[position], true)
                                } else {
                                    dataQueue.isSelected = false
                                    adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))

                                    //
                                    when (val data = listQueue[position]) {
                                        is VideoModel -> {
                                            data.isSelected = true
                                            startVideo(data)
                                        }
                                        is AudioModel -> {
                                            data.isSelected = true
                                            startVideo(data)
                                        }
                                    }
                                    adapterQueue?.notifyItemChanged(position)

                                    (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                        position
                                    )
                                }
                                break
                            }
                        }
                        is AudioModel -> {
                            if (dataQueue.isSelected) {
                                val position = getPositionModeQueue(dataQueue)
                                if (position == listQueue.indexOf(dataQueue)) {
                                    startVideo(listQueue[position], true)
                                } else {
                                    dataQueue.isSelected = false
                                    adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))
                                    //
                                    when (val data = listQueue[position]) {
                                        is VideoModel -> {
                                            data.isSelected = true
                                            startVideo(data)
                                        }
                                        is AudioModel -> {
                                            data.isSelected = true
                                            startVideo(data)
                                        }
                                    }

                                    adapterQueue?.notifyItemChanged(position)
                                    (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                        position
                                    )
                                }
                                break
                            }
                        }
                    }

                }
            }, 1500)
        }
    }

    private fun previousFileCast() {
        if (isAdded) {
            DialogLoadingUtils.showDialogDownloading(requireContext(), true)
            Handler().postDelayed({
                for (dataQueue in listQueue) {
                    when (dataQueue) {
                        is VideoModel -> {
                            if (dataQueue.isSelected) {
                                var position = listQueue.indexOf(dataQueue) - 1
                                if (position < 0) {
                                    position = 0
                                }
                                dataQueue.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))
                                //
                                when (val data = listQueue[position]) {
                                    is VideoModel -> {
                                        data.isSelected = true
                                        startVideo(listQueue[position])
                                    }
                                    is AudioModel -> {
                                        data.isSelected = true
                                        startVideo(listQueue[position])
                                    }
                                }
                                adapterQueue?.notifyItemChanged(position)

                                (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                    position
                                )
                                break
                            }
                        }
                        is AudioModel -> {
                            if (dataQueue.isSelected) {
                                var position = listQueue.indexOf(dataQueue) - 1
                                if (position < 0) {
                                    position = 0
                                }
                                dataQueue.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))
                                //
                                when (val data = listQueue[position]) {
                                    is VideoModel -> {
                                        data.isSelected = true
                                        startVideo(data)
                                    }
                                    is AudioModel -> {
                                        data.isSelected = true
                                        startVideo(data)
                                    }
                                }
                                adapterQueue?.notifyItemChanged(position)

                                (binding.bottomSheet.rclQueue.layoutManager as LinearLayoutManager).scrollToPosition(
                                    position
                                )
                                break
                            }
                        }
                    }
                }
            }, 1500)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getPositionModeQueue(dataQueue: T): Int {
        when {
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeat_mode).toBitmap()) -> {

                var position = listQueue.indexOf(dataQueue) + 1
                if (position >= listQueue.size) {
                    position = 0
                }
                return position
            }
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeatone_mode).toBitmap()) -> {
                return listQueue.indexOf(dataQueue)
            }
            else -> {
                return Random.nextInt(listQueue.size)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeMode() {
        when {
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeat_mode).toBitmap()) -> {
                binding.icMode.setImageResource(R.drawable.ic_repeatone_mode)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_repeatone)
                    txtMode.text = getString(R.string.repeatone)
                }
                toast(getString(R.string.repeatone))
            }
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeatone_mode).toBitmap()) -> {
                binding.icMode.setImageResource(R.drawable.ic_shuffle_mode)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_shuffle)
                    txtMode.text = getString(R.string.shuffle)
                }
                toast(getString(R.string.shuffle))
            }
            else -> {
                binding.icMode.setImageResource(R.drawable.ic_repeat_mode)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_repeat)
                    txtMode.text = getString(R.string.repeat)
                }
                toast(getString(R.string.repeat))
            }
        }
    }

    private fun statePlayPause() {
        activityMain?.remoteMediaClient?.let {
            if (it.isPlaying) {
                binding.icPlay.setImageResource(R.drawable.ic_pause)
                it.pause()
            } else {
                if ((requireActivity() as MainActivity).isRouterConnected()) {
                    binding.icPlay.setImageResource(R.drawable.ic_play)
                    it.play()
                } else {
                    binding.icPlay.setImageResource(R.drawable.ic_pause)
                    it.pause()
                }
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentCastVideoBinding {
        return LayoutFragmentCastVideoBinding.inflate(LayoutInflater.from(requireContext()))
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
                Log.d("Huy", "onApplicationConnected: ")
            }

            private fun onApplicationDisconnected() {
                Log.d("Huy", "onApplicationDisconnected: ")
            }
        }
    }

    //
    @SuppressLint("SimpleDateFormat")
    private fun startVideo(data: T, isDuplicate: Boolean = false, currentTime: Int = 0) {
        isHidingCastMain = false
        when (data) {
            is VideoModel -> {
                binding.txtTitle.text = data.fileName
                loadVideo(data, isDuplicate)
                (requireActivity() as MainActivity).updateViewCastMain(
                    data.path,
                    null,
                    data.fileName ?: "Unknow",
                    currentTime.toLong(),
                    data.duration,
                    true,
                    false
                )
                binding.txtDuration.text = Utils.formatDurationLong(data.duration)
            }
            is AudioModel -> {
                binding.txtTitle.text = data.songName
                loadAudio(data, isDuplicate)
                (requireActivity() as MainActivity).updateViewCastMain(
                    data.path,
                    data.resId,
                    data.songName ?: "Unknow",
                    currentTime.toLong(),
                    data.duration,
                    true,
                    false, true
                )
                binding.txtDuration.text = Utils.formatDurationLong(data.duration)

            }
        }
    }

    private fun loadVideo(data: VideoModel, isDuplicate: Boolean, currentTime: Long = 0) {
        complete = false
        durationTimer = 0L
        movieMetaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_SUBTITLE, "EZSTUDIO")
            putString(MediaMetadata.KEY_TITLE, data.path?.let { File(it).name } ?: "")
        }
//            movieMetaData.putString(MediaMetadata.KEY_TITLE, "STUDIO")
        if (isDuplicate) {
            movieMetaData?.let {
                activityMain?.let { main ->
                    main.loadRemoteMedia(
                        currentTime,
                        true,
                        main.getMediaInfo(urlLocal, data.duration, it, Utils.TYPE_VIDEO)
                    )
                }
                // callback

            }
        } else {
            localFilePath = data.path ?: ""
            startLocalServer(localFilePath, Utils.TYPE_VIDEO) { urlServer ->
                Log.d("Huy", "initListener: ${urlServer + data.path}")
                urlLocal = urlServer + data.path

                movieMetaData?.let {
                    activityMain?.let { main ->
                        main.loadRemoteMedia(
                            currentTime,
                            true,
                            main.getMediaInfo(urlLocal, data.duration, it, Utils.TYPE_VIDEO)
                        )
                    }
                }
                //
            }
        }
    }

    private fun loadAudio(data: AudioModel, isDuplicate: Boolean, currentTime: Long = 0) {
        complete = false
        durationTimer = 0L
        movieMetaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_SUBTITLE, "EZSTUDIO")
            putString(MediaMetadata.KEY_TITLE, data.path?.let { File(it).name } ?: "")
        }
//            movieMetaData.putString(MediaMetadata.KEY_TITLE, "STUDIO")
        if (isDuplicate) {
            movieMetaData?.let {
                activityMain?.let { main ->
                    main.loadRemoteMedia(
                        currentTime,
                        true,
                        main.getMediaInfo(urlLocal, data.duration, it, Utils.TYPE_AUDIO)
                    )
                }
                // callback
            }
        } else {
            localFilePath = data.path ?: ""
            startLocalServer(localFilePath, Utils.TYPE_AUDIO) { urlServer ->
                Log.d("Huy", "initListener: ${urlServer + data.path}")
                urlLocal = urlServer + data.path

                movieMetaData?.let {
                    activityMain?.let { main ->
                        main.loadRemoteMedia(
                            currentTime,
                            true,
                            main.getMediaInfo(urlLocal, data.duration, it, Utils.TYPE_AUDIO)
                        )
                    }
                }
                //
            }
        }
    }

    fun onStatusUpdated() {
        if (isViewCreate) {
            activityMain?.remoteMediaClient?.let {
                DialogLoadingUtils.showDialogDownloading(requireContext(), false)
                if (it.isPlaying) {
                    binding.icPlay.setImageResource(R.drawable.ic_play)
                } else {
                    binding.icPlay.setImageResource(R.drawable.ic_pause)
                }
                //
                binding.txtDuration.text = Utils.formatDurationLong(it.streamDuration)
                // seekbar
                binding.seekbarController.max = it.streamDuration.toInt()
            }
        }
    }

    fun onProcessListener(currentTime: Long, duration: Long) {
        if (isViewCreate) {
            durationTimer = currentTime
            binding.txtRealtime.text = Utils.formatDurationLong(currentTime)
            // seekbar
            if (!isTouchSeekbar) {
                binding.seekbarController.progress = currentTime.toInt()
            }
            //
            if (currentTime >= 0L) {
                DialogLoadingUtils.showDialogDownloading(requireContext(), false)
            }
            if (currentTime in duration - 55..duration + 55 && duration != (-1).toLong()) {
                if (!complete) {
                    complete = true
                    nextFileCast()
                }
            }
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
                Utils.TYPE_VIDEO -> {
                    activityMain?.server = LocalServer(localFilePath, "videos")
                }
                Utils.TYPE_AUDIO -> {
                    activityMain?.server = LocalServer(localFilePath, "audios")
                }
            }

            activityMain?.server?.let {
                Handler().postDelayed({
                    try {
                        it.start()
                    } catch (ioe: Exception) {
                        DialogLoadingUtils.showDialogDownloading(requireContext(), false)
                        toast(getString(R.string.error_start_server))
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

                }, 2000)
            }
        } else {
//            toast(getString(R.string.error_start_server))
        }
    }

    private fun setStateMuteSound() {
        if (mCastSession?.isMute == true) {
            binding.icMute.setImageResource(R.drawable.ic_sound)
            binding.txtMute.text = getString(R.string.sound)
        } else {
            binding.icMute.setImageResource(R.drawable.ic_mute)
            binding.txtMute.text = getString(R.string.mute)
        }
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

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).setHideViewCastMain(isHidingCastMain)
    }
}