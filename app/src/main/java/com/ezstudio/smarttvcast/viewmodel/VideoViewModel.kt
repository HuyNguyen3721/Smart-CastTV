package com.ezstudio.smarttvcast.viewmodel

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezteam.baseproject.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class VideoViewModel(application: Application) : BaseViewModel(application) {
    var videos: MutableLiveData<VideoModel?> = MutableLiveData()

    fun getVideoGallery(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("Huy", "getVideoGallery:")
            val uri = MediaStore.Video.Media.DATA
            // if GetImageFromThisDirectory is the name of the directory from which image will be retrieved
            val projection = arrayOf(
                uri, MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE
            )
            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
                )
                if (cursor != null) {
                    val isDataPresent = cursor.moveToFirst()
                    if (isDataPresent) {
                        do {
                            Log.d("Huy", "getVideoGallery:111 ")
                            val file = File(cursor.getString(cursor.getColumnIndex(uri)))
                            try {
                                val video = VideoModel(
                                    file.name,
                                    FileUtils.getMediaDuration(file, context),
                                    file.absolutePath, file.lastModified()
                                )
                                viewModelScope.launch(Dispatchers.Main) {
                                    videos.value = (video)
                                }
                                Log.d("Huy", "getVideoGallery:${file.name} ")
                            } catch (e: IllegalArgumentException) {
                            } catch (r: RuntimeException) {
                            }
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    // recent
    fun updateFileRecent(fileModel: VideoModel, db: AppDatabase, time: Long = 1) {
        val file = db.serverDao().getVideoByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.timeRecent = if (time != 0L) (System.currentTimeMillis()) else 0L
        if (file == null) {
            db.serverDao().insertVideo(fileDb)
        } else {
            db.serverDao().updateVideo(fileDb)
        }
    }

}