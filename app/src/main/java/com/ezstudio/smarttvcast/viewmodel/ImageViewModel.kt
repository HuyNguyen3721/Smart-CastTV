package com.ezstudio.smarttvcast.viewmodel

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezteam.baseproject.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageViewModel(application: Application) : BaseViewModel(application) {
    var imageStoreLiveData: MutableLiveData<String?> = MutableLiveData()

    fun getImagesGallery(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = MediaStore.Images.Media.DATA
            // if GetImageFromThisDirectory is the name of the directory from which image will be retrieved
            val projection = arrayOf(
                uri, MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )
            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
                )
                if (cursor!=null) {
                    val isDataPresent = cursor.moveToFirst()
                    if (isDataPresent) {
                        do {
                            val file = File(cursor.getString(cursor.getColumnIndex(uri)))
                            if (file.length() > 10000) {
                                viewModelScope.launch(Dispatchers.Main) {
                                        imageStoreLiveData.value = (file.absolutePath)
                                }
                            }
                        } while (cursor.moveToNext())
                        cursor.close()
                    }

                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    // recent
    fun updateFileRecent(fileModel: ImageModel, db: AppDatabase, time: Long = 1) {
        val file = db.serverDao().getImageByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.timeRecent = if (time != 0L) (System.currentTimeMillis()) else 0L
        if (file == null) {
            db.serverDao().insertImage(fileDb)
        } else {
            db.serverDao().updateImage(fileDb)
        }
    }
}