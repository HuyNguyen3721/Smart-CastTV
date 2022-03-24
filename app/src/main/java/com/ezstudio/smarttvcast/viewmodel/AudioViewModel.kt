package com.ezstudio.smarttvcast.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.viewModelScope
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.utils.FileUtils
import com.ezteam.baseproject.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class AudioViewModel(application: Application) : BaseViewModel(application) {
    var audios: MutableLiveData<AudioModel?> = MutableLiveData()

    fun getAudiosRestore(context: Context) {
        val strArr = Environment.getExternalStorageDirectory().absolutePath
        viewModelScope.launch(Dispatchers.IO) {
            val arr = File(strArr).listFiles()
            if (!arr.isNullOrEmpty()) {
                checkFileOfDirectoryAudio(strArr, arr, context)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun checkFileOfDirectoryAudio(temp: String, fileArr: Array<File>, context: Context) {
        for (i in fileArr.indices) {
            if (fileArr[i].isDirectory) {
                val temp_sub = fileArr[i].path
                val mfileArr = File(fileArr[i].path).listFiles()
                if (mfileArr != null && mfileArr.isNotEmpty()) checkFileOfDirectoryAudio(
                    temp_sub,
                    mfileArr, context
                )
            } else {
                if (fileArr[i].path.endsWith(".mp3")
                    || fileArr[i].path.endsWith(".aac")
                    || fileArr[i].path.endsWith(".amr")
                    || fileArr[i].path.endsWith(".m4a")
                    || fileArr[i].path.endsWith(".ogg")
                    || fileArr[i].path.endsWith(".wav")
                    || fileArr[i].path.endsWith(".flac")
                ) {
                    val file = File(fileArr[i].path)
                    var duration: Long = 0
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.path)
                        duration =
                            (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?: "0").toLong()
                        retriever.release()
                    } catch (e: Exception) {
                    }
                    val file_size = file.length().toString().toLong()
                    if (file_size > 10000) {
                        // get author
                        var artist: String? = null
                        var rawArt: ByteArray? = null
                        var art: Bitmap? = null
                        try {
                            val mmr = MediaMetadataRetriever()
                            val fileAudio = File(fileArr[i].path)
                            if (fileAudio.length() > 0 && fileAudio.exists()) {
                                mmr.setDataSource(context, Uri.parse(fileArr[i].path))
                                artist =
                                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                rawArt = mmr.embeddedPicture
                            }
                            if (rawArt != null) {
                                art = BitmapFactory.decodeByteArray(
                                    rawArt,
                                    0,
                                    rawArt.size,
                                    BitmapFactory.Options()
                                )
                            }
                        } catch (e: FileNotFoundException) {
                        } catch (e: IOException) {
                        } catch (e: RuntimeException) {
                        } catch (e: Exception) {
                        }
                        viewModelScope.launch(Dispatchers.Main) {
                            audios.value =
                                AudioModel(
                                    art ?: context.getDrawable(R.drawable.ic_audio)?.toBitmap(),
                                    fileArr[i].name,
                                    artist ?: "Unknown",
                                    fileArr[i].path,
                                    FileUtils.getMediaDuration(fileArr[i], context)
                                )
                        }
                    }
                }
            }
        }
    }

    // recent
    fun updateFileRecent(fileModel: AudioModel, db: AppDatabase, time: Long = 1) {
        val file = db.serverDao().getAudioByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.timeRecent = if (time != 0L) (System.currentTimeMillis()) else 0L
        if (file == null) {
            db.serverDao().insertAudio(fileDb)
        } else {
            db.serverDao().updateAudio(fileDb)
        }
    }
}