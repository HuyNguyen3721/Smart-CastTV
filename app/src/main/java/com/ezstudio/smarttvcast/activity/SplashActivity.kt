package com.ezstudio.smarttvcast.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import com.ezstudio.smarttvcast.databinding.ActivitySplashBinding
import com.ezteam.baseproject.activity.BaseActivity
import com.ezteam.baseproject.extensions.launchActivity
import com.google.android.gms.ads.ez.AdFactoryListener
import com.google.android.gms.ads.ez.LogUtils
import com.google.android.gms.ads.ez.admob.AdmobOpenAdUtils

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun initView() {
        // Screen full
        setAppActivityFullScreenOver(this)
        //
        openMain()
    }

    override fun initData() {
    }

    override fun initListener() {
    }

    override fun viewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }
    private fun openMain() {
        AdmobOpenAdUtils.getInstance(this).setAdListener(object : AdFactoryListener() {
            override fun onError() {
                LogUtils.logString(SplashActivity::class.java, "onError")
                startAct()
            }

            override fun onLoaded() {
                LogUtils.logString(SplashActivity::class.java, "onLoaded")
                // show ads ngay khi loaded
                AdmobOpenAdUtils.getInstance(this@SplashActivity).showAdIfAvailable(false)
            }

            override fun onDisplay() {
                super.onDisplay()
                LogUtils.logString(SplashActivity::class.java, "onDisplay")
            }

            override fun onDisplayFaild() {
                super.onDisplayFaild()
                LogUtils.logString(SplashActivity::class.java, "onDisplayFaild")
                startAct()
            }

            override fun onClosed() {
                super.onClosed()
                // tam thoi bo viec load lai ads thi dismis
                LogUtils.logString(SplashActivity::class.java, "onClosed")
                startAct()
            }
        }).loadAd()
    }

    private  fun startAct(){
        Handler().postDelayed({
            launchActivity<MainActivity> { }
            finish()
        },100)
    }
}