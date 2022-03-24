package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import com.ezstudio.smarttvcast.databinding.LayoutDialogWrongConnectionBinding

class DialogWrongConnection(context: Context) :
    AlertDialog(context) {
    private val binding by lazy {
        LayoutDialogWrongConnectionBinding.inflate(LayoutInflater.from(context))
    }
    var listenerHelp: (() -> Unit)? = null
    var listenerOK: (() -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        this.setCancelable(true)
        initData()
        initView()
        initListener()
    }

    private fun initData() {

    }

    private fun initView() {
        //
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun initListener() {
        binding.btnHelp.setOnClickListener {
            listenerHelp?.invoke()
            dismiss()
        }
        binding.btnOk.setOnClickListener {
//            listenerOK?.invoke()
            dismiss()
        }

    }
}