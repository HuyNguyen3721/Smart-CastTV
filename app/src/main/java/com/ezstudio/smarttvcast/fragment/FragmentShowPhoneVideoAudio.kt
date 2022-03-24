package com.ezstudio.smarttvcast.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.activity.MainActivity
import com.ezstudio.smarttvcast.adapter.AdapterQueue
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutFragmentVideoAudioBinding
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.DialogLoadingUtils
import com.ezstudio.smarttvcast.utils.RecycleViewUtils
import com.ezstudio.smarttvcast.utils.Utils
import com.ezstudio.smarttvcast.viewmodel.RouterViewModel
import com.google.android.gms.ads.ez.EzAdControl
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject
import kotlin.random.Random

class FragmentShowPhoneVideoAudio<T>(
    private val listFile: MutableList<T>,
    private val position: Int
) :
    BaseCastFragment<LayoutFragmentVideoAudioBinding>() {

    private var countDownTimer: CountDownTimer? = null
    private val listQueue = mutableListOf<T>()
    private var adapterQueue: AdapterQueue<T>? = null
    private var isHidingCastMain = false
    private var activityMain: MainActivity? = null
    private val db by inject<AppDatabase>()
    private val viewModelRouter by lazy {
        ViewModelProvider(requireActivity()).get(RouterViewModel::class.java)
    }

    override fun initView() {
        // ads
        EzAdControl.getInstance(requireActivity()).showAds()
        //start video audio
        startVideoAudio(listFile[position])
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
    }

    override fun initData() {
        // view bottom sheet queue
        adapterQueue = AdapterQueue(requireContext(), listQueue, db)
        binding.bottomSheet.apply {
            txtNumberQueue.text = listQueue.size.toString()
            txtMode.text = getString(R.string.repeat_all)
            icRepeatQueue.setImageResource(R.drawable.ic_repeat)
            rclQueue.adapter = adapterQueue
        }
//        countDownTimer
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val currentPosition = binding.video.currentPosition
                val duration = binding.video.duration
                binding.seekbarController.progress = currentPosition
                binding.txtRealtime.text = Utils.formatDurationInt(currentPosition)
                binding.txtDuration.text = Utils.formatDurationInt(duration)
            }

            override fun onFinish() {
            }
        }
// connected
        viewModelRouter.isConnectedFromShowPhone.observe(requireActivity()) {
            if (it) {
                showDialogCast()
                viewModelRouter.isConnectedFromShowPhone.value = false
            }
        }

    }

    override fun initListener() {
        //complete
        binding.video.setOnCompletionListener {
            binding.icPlay.setImageResource(R.drawable.ic_pause)
            countDownTimer?.onFinish()
            nextFile()
        }
        //prepared
        binding.video.setOnPreparedListener {
            binding.icPlay.setImageResource(R.drawable.ic_play)
            binding.seekbarController.max = it.duration
            binding.txtDuration.text = Utils.formatDurationInt(it.duration)
            countDownTimer?.start()
        }
        // play pause
        binding.icPlay.setOnClickListener {
            if (binding.video.isPlaying) {
                binding.video.pause()
                binding.icPlay.setImageResource(R.drawable.ic_pause)
            } else {
                binding.video.start()
                binding.icPlay.setImageResource(R.drawable.ic_play)
            }
        }
        // Previous.
        binding.icPrevious.setOnClickListener {

        }
        // next
        binding.icNext.setOnClickListener {
            nextFile()
        }
        // seekbar
        // change seekbar
        binding.seekbarController.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { s ->
                    binding.video.seekTo((s.progress))
                }
            }

        })
        // ic cast
        binding.icCasting.setOnClickListener {
            if (isAdded) {
                if ((requireActivity() as MainActivity).isRouterConnected()) {
//                    (requireActivity() as MainActivity).showDialogDisconnectRouter()
                    showDialogCast()
                } else {
                    (requireActivity() as MainActivity).showDialogScanRouter()
                }
            }
        }
        // back
        binding.icBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

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
        //
        binding.icQueuePhone.setOnClickListener {
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
            binding.seekbarController.progress = 0
            val data = listQueue[it]
            for (item in listQueue) {
                if (item == data) {
                    when (item) {
                        is VideoModel -> {
                            if (item.isSelected) {
                                startVideoAudio(item)
                            } else {
                                item.isSelected = true
                                startVideoAudio(item)
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(item))
                            }
                        }
                        is AudioModel -> {
                            if (item.isSelected) {
                                startVideoAudio(item)
                            } else {
                                item.isSelected = true
                                startVideoAudio(item)
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
        binding.icMode.setOnClickListener {
            changeMode()
        }
        //
        //
        binding.seekbarController.setOnClickListener {

        }
    }

    private fun showDialogCast() {
        DialogLoadingUtils.showDialogCast(requireContext(), true) {
            if (it) {
                for (item in listQueue) {
                    when (item) {
                        is VideoModel -> {
                            if (item.isSelected) {
                                activityMain?.fragmentCastVideoAudio(
                                    listQueue,
                                    listQueue.indexOf(item), false, false
                                )
                                break
                            }
                        }
                        is AudioModel -> {
                            if (item.isSelected) {
                                activityMain?.fragmentCastVideoAudio(
                                    listQueue,
                                    listQueue.indexOf(item), false, false
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        activityMain = requireActivity() as MainActivity
        super.onAttach(context)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutFragmentVideoAudioBinding {
        return LayoutFragmentVideoAudioBinding.inflate(LayoutInflater.from(requireContext()))
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeMode() {
        when {
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeat_mode_phone).toBitmap()) -> {
                binding.icMode.setImageResource(R.drawable.ic_repeatone_mode_phone)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_repeatone)
                    txtMode.text = getString(R.string.repeatone)
                }
                toast(getString(R.string.repeatone))
            }
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeatone_mode_phone).toBitmap()) -> {
                binding.icMode.setImageResource(R.drawable.ic_shuffle_mode_phone)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_shuffle)
                    txtMode.text = getString(R.string.shuffle)
                }
                toast(getString(R.string.shuffle))
            }
            else -> {
                binding.icMode.setImageResource(R.drawable.ic_repeat_mode_phone)
                binding.bottomSheet.apply {
                    icRepeatQueue.setImageResource(R.drawable.ic_repeat)
                    txtMode.text = getString(R.string.repeat)
                }
                toast(getString(R.string.repeat))
            }
        }
    }

    private fun <T> startVideoAudio(data: T) {
        when (data) {
            is VideoModel -> {
                binding.icAudio.visibility = View.INVISIBLE
                binding.video.setVideoPath(data.path)
                binding.txtTitle.text = data.fileName
            }
            is AudioModel -> {
                binding.icAudio.visibility = View.VISIBLE
                Glide.with(requireContext()).load(data.resId ?: R.drawable.ic_audio)
                    .into(binding.icAudio)
                binding.video.setVideoPath(data.path)
                binding.txtTitle.text = data.songName
            }
        }
        binding.video.start()
    }

    private fun nextFile() {
        if (isAdded) {
            for (dataQueue in listQueue) {
                when (dataQueue) {
                    is VideoModel -> {
                        if (dataQueue.isSelected) {
                            val position = getPositionModeQueue(dataQueue)
                            if (position == listQueue.indexOf(dataQueue)) {
                                startVideoAudio(listQueue[position])
                            } else {
                                dataQueue.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))
                                //
                                when (val data = listQueue[position]) {
                                    is VideoModel -> {
                                        data.isSelected = true
                                        startVideoAudio(data)
                                    }
                                    is AudioModel -> {
                                        data.isSelected = true
                                        startVideoAudio(data)
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
                                startVideoAudio(listQueue[position])
                            } else {
                                dataQueue.isSelected = false
                                adapterQueue?.notifyItemChanged(listQueue.indexOf(dataQueue))
                                //
                                when (val data = listQueue[position]) {
                                    is VideoModel -> {
                                        data.isSelected = true
                                        startVideoAudio(data)
                                    }
                                    is AudioModel -> {
                                        data.isSelected = true
                                        startVideoAudio(data)
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
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getPositionModeQueue(dataQueue: T): Int {
        when {
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeat_mode_phone).toBitmap()) -> {
                var position = listQueue.indexOf(dataQueue) + 1
                if (position >= listQueue.size) {
                    position = 0
                }
                return position
            }
            binding.icMode.drawable.toBitmap()
                .sameAs(resources.getDrawable(R.drawable.ic_repeatone_mode_phone).toBitmap()) -> {
                return listQueue.indexOf(dataQueue)
            }
            else -> {
                return Random.nextInt(listQueue.size)
            }
        }
    }

    override fun onDestroy() {
        viewModelRouter.isConnectedFromShowPhone.value = false
        super.onDestroy()
    }
}