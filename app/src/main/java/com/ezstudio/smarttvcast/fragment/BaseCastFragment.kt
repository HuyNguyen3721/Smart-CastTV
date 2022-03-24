package com.ezstudio.smarttvcast.fragment

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.ezteam.baseproject.fragment.BaseFragment
import com.ezteam.baseproject.utils.KeyboardUtils

abstract class BaseCastFragment<B : ViewBinding> : BaseFragment<B>() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtils.autoHideClickView(view, requireActivity())
        binding.root.isClickable = true
        binding.root.isFocusable = true
    }
}