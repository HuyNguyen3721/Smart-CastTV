package com.ezstudio.smarttvcast.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.ezstudio.smarttvcast.databinding.LayoutDialogCreatePlaylistBinding

class DialogCreatePlaylist(
    context: Context,
    var binding: LayoutDialogCreatePlaylistBinding,
    var style: Int
) :
    Dialog(context, style) {
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
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

    private fun initListener() {
        binding.btnNo.setOnClickListener {
            listenerNo?.invoke()
            dismiss()
        }
        binding.btnSave.setOnClickListener {
            listenerYes?.invoke()
        }
    }
}