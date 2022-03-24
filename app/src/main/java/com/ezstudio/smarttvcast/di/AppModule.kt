package com.ezstudio.smarttvcast.di

import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.viewmodel.AudioViewModel
import com.ezstudio.smarttvcast.viewmodel.FileViewModel
import com.ezstudio.smarttvcast.viewmodel.ImageViewModel
import com.ezstudio.smarttvcast.viewmodel.VideoViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidApplication()) }
    single { VideoViewModel(androidApplication()) }
    single { ImageViewModel(androidApplication()) }
    single { AudioViewModel(androidApplication()) }
    single { FileViewModel(androidApplication()) }
}