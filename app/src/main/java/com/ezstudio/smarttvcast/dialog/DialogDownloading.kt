package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.databinding.LayoutDialogDownloadingBinding

class DialogDownloading(context: Context) : AlertDialog(context) {
    private lateinit var binding: LayoutDialogDownloadingBinding
    private var keyMode = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setCancelable(false)
        initData()
        initView()
        initListener()
    }

    private fun initData() {

    }

    private fun initView() {
        binding = LayoutDialogDownloadingBinding.inflate(LayoutInflater.from(context))
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

    private fun initListener() {
        object : CountDownTimer(800, 1000000) {
            override fun onTick(millisUntilFinished: Long) {
                when (keyMode) {
                    0 -> {
                        binding.icPhone.setImageResource(R.drawable.ic_phone_connected)
                        binding.dot1.setImageResource(R.drawable.ic_dot_dot_dot_connected)
                        binding.icTv.setImageResource(R.drawable.ic_tv_disconnected)
                        binding.icDot2.setImageResource(R.drawable.ic_dot_dot_dot_disconnected)
                        binding.icWifi.setImageResource(R.drawable.ic_wifi_disconnected)
//                        binding.icPhone.setImageResource(R.drawable.ic_phone_connected)
//                        binding.dot1.setImageResource(R.drawable.ic_dot_dot_dot_connected)
                        keyMode = 1
                    }
                    1 -> {
                        binding.icPhone.setImageResource(R.drawable.ic_phone_disconnected)
                        binding.dot1.setImageResource(R.drawable.ic_dot_dot_dot_disconnected)
                        binding.icTv.setImageResource(R.drawable.ic_tv_connected)
                        binding.icDot2.setImageResource(R.drawable.ic_dot_dot_dot_connected)
                        keyMode = 2

                    }
                    2 -> {
                        binding.icTv.setImageResource(R.drawable.ic_tv_disconnected)
                        binding.icWifi.setImageResource(R.drawable.ic_wifi_connected)
                        keyMode = 0
                    }
                }
            }

            override fun onFinish() {
                this.start()
            }
        }.start()
    }
}