package com.ezstudio.smarttvcast.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.databinding.LayoutItemMainBinding

@SuppressLint("CustomViewStyleable", "Recycle")
class CustomViewItemMain(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    lateinit var binding: LayoutItemMainBinding
    var name: String? = null
    var src = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ItemMain)
        name = typedArray.getString(R.styleable.ItemMain_name)
        src = typedArray.getResourceId(R.styleable.ItemMain_src, 0)
        initView()
        initData()
    }

    private fun initData() {
        Glide.with(context).load(src).into(binding.icItem)
        binding.txtName.text = name
    }

    private fun initView() {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_item_main, this, true)
        binding = LayoutItemMainBinding.bind(view)
    }
}