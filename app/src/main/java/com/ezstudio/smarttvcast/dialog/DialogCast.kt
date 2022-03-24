package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import com.ezstudio.smarttvcast.databinding.LayoutDialogCastBinding
import com.ezstudio.smarttvcast.databinding.LayoutDialogDeleteFileBinding
import com.ezstudio.smarttvcast.databinding.LayoutDialogDownloadingBinding

class DialogCast(
    context: Context
) :
    AlertDialog(context) {
    private lateinit var binding: LayoutDialogCastBinding
    var listenerNo: (() -> Unit)? = null
    var listenerYes: (() -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setCancelable(true)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
    }

    private fun initView() {
        binding = LayoutDialogCastBinding.inflate(LayoutInflater.from(context))
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

    private fun initListener() {
        binding.btnNo.setOnClickListener {
            listenerNo?.invoke()
            dismiss()
        }
        binding.btnYes.setOnClickListener {
            listenerYes?.invoke()
        }
    }
}