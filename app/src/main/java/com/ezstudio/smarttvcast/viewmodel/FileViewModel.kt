package com.ezstudio.smarttvcast.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezteam.baseproject.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileViewModel(application: Application) : BaseViewModel(application) {
    var listFileRecentViewModel: MutableLiveData<MutableList<Any>?> = MutableLiveData()
    var listFileFavoriteViewModel: MutableLiveData<MutableList<Any>?> = MutableLiveData()
    var listPlaylist: MutableLiveData<MutableList<PlayListModel>?> = MutableLiveData()
    var listDetailPlaylist: MutableLiveData<MutableList<Any>?> = MutableLiveData()

    fun getFileRecent(db: AppDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            val listFileRecent = mutableListOf<Any>()
            listFileRecent.addAll(db.serverDao().getRecentVideo)
            listFileRecent.addAll(db.serverDao().getRecentAudio)
            listFileRecent.addAll(db.serverDao().getRecentImage)

            if (listFileRecent.size > 2) {
                listFileRecent.sortWith { o1, o2 ->
                    val time_1 =
                        when (o1) {
                            is VideoModel -> {
                                o1.timeRecent
                            }
                            is AudioModel -> {
                                o1.timeRecent

                            }
                            is ImageModel -> {
                                o1.timeRecent
                            }
                            else -> 0L
                        }

                    val time_2 = when (o2) {
                        is VideoModel -> {
                            o2.timeRecent
                        }
                        is AudioModel -> {
                            o2.timeRecent

                        }
                        is ImageModel -> {
                            o2.timeRecent
                        }
                        else -> {
                            0L
                        }
                    }

                    when {
                        time_1 > time_2 -> {
                            -1
                        }
                        time_1 < time_2 -> {
                            1
                        }
                        else -> {
                            0
                        }
                    }
                }
                if (listFileRecent.size > 20) {
                    for (i in 0 until listFileRecent.size) {
                        if (i > 20) {
                            listFileRecent.removeAt(i)
                        }
                    }
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                listFileRecentViewModel.value = listFileRecent
            }
        }
    }

    fun getFileFavorite(db: AppDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            val listFavoriteFile = mutableListOf<Any>()
            listFavoriteFile.addAll(db.serverDao().getFavoriteVideo)
            listFavoriteFile.addAll(db.serverDao().getFavoriteAudio)
            listFavoriteFile.addAll(db.serverDao().getFavoriteImage)

            if (listFavoriteFile.size > 2) {
                listFavoriteFile.sortWith { o1, o2 ->
                    val time_1 =
                        when (o1) {
                            is VideoModel -> {
                                o1.timeRecent
                            }
                            is AudioModel -> {
                                o1.timeRecent

                            }
                            is ImageModel -> {
                                o1.timeRecent
                            }
                            else -> 0L
                        }

                    val time_2 = when (o2) {
                        is VideoModel -> {
                            o2.timeRecent
                        }
                        is AudioModel -> {
                            o2.timeRecent

                        }
                        is ImageModel -> {
                            o2.timeRecent
                        }
                        else -> {
                            0L
                        }
                    }

                    when {
                        time_1 > time_2 -> {
                            -1
                        }
                        time_1 < time_2 -> {
                            1
                        }
                        else -> {
                            0
                        }
                    }
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                listFileFavoriteViewModel.value = listFavoriteFile
            }
        }
    }

    fun getPlaylist(db: AppDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<PlayListModel>()
            list.addAll(db.serverDao().getPlaylist)
            viewModelScope.launch(Dispatchers.Main) {
                listPlaylist.value = list
            }
        }
    }

    fun getDetailPlaylist(db: AppDatabase, listPath: MutableList<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<Any>()
            for (data in listPath) {
                val video = db.serverDao().getVideoByPath(data)
                val audio = db.serverDao().getAudioByPath(data)
                if (video != null) {
                    list.add(video)
                } else if (audio != null) {
                    list.add(audio)
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                listDetailPlaylist.value = list
            }
        }
    }
}