package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import com.ezstudio.smarttvcast.databinding.LayoutDialogDisconnectRouterBinding

class DialogDisConnectedRouter(
    context: Context,
    var style: Int, var name: String
) :
    AlertDialog(context, style) {
    private lateinit var binding: LayoutDialogDisconnectRouterBinding

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
        binding = LayoutDialogDisconnectRouterBinding.inflate(LayoutInflater.from(context))
        binding.name.text = name
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

    private fun initListener() {
        binding.btnNo.setOnClickListener {
            dismiss()
        }
        binding.btnYes.setOnClickListener {
            listenerYes?.invoke()
            dismiss()
        }
    }
}