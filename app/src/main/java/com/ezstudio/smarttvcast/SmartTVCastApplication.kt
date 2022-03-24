package com.ezstudio.smarttvcast

import android.app.Application
import com.ezstudio.smarttvcast.di.appModule
import com.ezteam.baseproject.utils.PreferencesUtils
import com.google.android.gms.ads.ez.EzApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SmartTVCastApplication  : EzApplication(){
    override fun onCreate() {
        super.onCreate()
        PreferencesUtils.init(this)
        setupKoin()
    }
    private fun setupKoin() {
        startKoin {
            androidContext(this@SmartTVCastApplication)
            modules(
                appModule
            )
        }
    }
}