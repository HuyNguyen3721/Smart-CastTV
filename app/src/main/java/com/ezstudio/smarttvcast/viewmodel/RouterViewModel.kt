package com.ezstudio.smarttvcast.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.ezteam.baseproject.viewmodel.BaseViewModel

class RouterViewModel(application: Application) : BaseViewModel(application) {
    var nameCastDevice: MutableLiveData<String?> = MutableLiveData()
    var isConnectedLiveData: MutableLiveData<Boolean?> = MutableLiveData()
    var isConnectedFromShowPhone: MutableLiveData<Boolean> = MutableLiveData()
    var isChangeStateWifi :  MutableLiveData<Boolean> = MutableLiveData()
}