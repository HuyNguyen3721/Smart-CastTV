package com.ezstudio.smarttvcast.utils

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import androidx.fragment.app.FragmentActivity
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.databinding.LayoutDialogCreatePlaylistBinding
import com.ezstudio.smarttvcast.databinding.LayoutDialogDeleteFileBinding
import com.ezstudio.smarttvcast.dialog.DialogCast
import com.ezstudio.smarttvcast.dialog.DialogCreatePlaylist
import com.ezstudio.smarttvcast.dialog.DialogDeleteFile
import com.ezstudio.smarttvcast.key.Vault
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezteam.baseproject.utils.KeyboardUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object FileUtils {
    private var dialogQuestion: DialogDeleteFile? = null
    private var dialogCreatePlaylist: DialogCreatePlaylist? = null

    @Throws(IllegalArgumentException::class, RuntimeException::class)
    fun getMediaDuration(file: File, context: Context): Long {
        if (!file.exists()) return 0
        val inputStream = FileInputStream(file.absolutePath)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputStream.fd)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return duration?.toLongOrNull() ?: 0
    }

    fun getFileExtension(filePath: String): String {
        var extension = ""
        try {
            extension = filePath.substring(filePath.lastIndexOf(".") + 1)
        } catch (exception: Exception) {
            Log.e("Err", exception.toString() + "")
        }
        return extension
    }
    // file imnage

    fun saveImageBitmap(
        context: Context,
        folderName: String,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        var fos: OutputStream?
        var imageUri: Uri?
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val newFile = File(initFolderParent(folderName), filename)
            if (!newFile.exists()) {
                newFile.createNewFile()
            }
            fos = try {
                FileOutputStream(newFile)
            } catch (ex: Exception) {
                FileOutputStream(newFile.path)
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos?.flush()
            fos?.close()
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                newFile.absolutePath,
                newFile.name,
                newFile.name
            )
            imageUri = Uri.fromFile(newFile)
            context.sendBroadcast(
                Intent(
                    "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    imageUri
                )
            )
        } else {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + folderName
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                context.contentResolver.also { resolver ->
                    imageUri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let {
                        resolver.openOutputStream(it)
                    }
                }

                fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                fos?.close()
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                imageUri?.let { context.contentResolver.update(it, contentValues, null, null) }
                return imageUri
            } catch (ex: Exception) {
                return null
            }

        }
        return null
    }

    private fun initFolderParent(name: String): String? {
        val mediaDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            name
        )
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && !mediaDirectory.isDirectory) {
            mediaDirectory.mkdirs()
        }
        return mediaDirectory.path
    }

    //
    // delete file
    fun showDeleteFile(
        fileModel: Any,
        content: String? = null,
        title: String? = null,
        context: Context,
        activity: FragmentActivity,
        db: AppDatabase,
        done: ((Boolean) -> (Unit))? = null
    ) {
        val bindDialog =
            LayoutDialogDeleteFileBinding.inflate(LayoutInflater.from(context))
        dialogQuestion = DialogDeleteFile(context, bindDialog, R.style.StyleDialogDelete)
        dialogQuestion?.let {
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
            content.let { c -> it.binding.txtContent.text = c }
            title.let { t -> it.binding.txtTitle.text = t }
        }

        dialogQuestion?.listenerYes = {
            DialogLoadingUtils.showDialogWaiting(context, true)
            when (fileModel) {
                is VideoModel -> {
                    fileModel.path?.let {
                        deleteFile(it, activity) { success ->
                            if (success) {
                                // xoa thanh cong thi do something
                                db.serverDao().getVideoByPath(fileModel.path)?.let { video ->
                                    db.serverDao().deleteVideo(video)
                                }
                                done?.invoke(true)
                            } else {
                                done?.invoke(false)
                            }
                        }
                    }
                }
                is AudioModel -> {
                    fileModel.path?.let {
                        deleteFile(it, activity) { success ->
                            if (success) {
                                // xoa thanh cong thi do something
                                db.serverDao().getAudioByPath(fileModel.path)?.let { audio ->
                                    db.serverDao().deleteAudio(audio)
                                }
                                done?.invoke(true)
                            } else {
                                done?.invoke(false)
                            }
                        }
                    }
                }
                is ImageModel -> {
                    fileModel.path?.let {
                        deleteFile(it, activity) { success ->
                            if (success) {
                                // xoa thanh cong thi do something
                                db.serverDao().getImageByPath(fileModel.path)?.let { image ->
                                    db.serverDao().deleteImage(image)
                                }
                                done?.invoke(true)
                            } else {
                                done?.invoke(false)
                            }
                        }
                    }
                }
            }
            dialogQuestion?.dismiss()
        }
        dialogQuestion?.listenerNo = {
            dialogQuestion?.dismiss()
        }
        dialogQuestion?.show()
    }

    // delete file
    fun showRemoveRecent(
        content: String? = null,
        title: String? = null,
        context: Context,
        done: ((Boolean) -> (Unit))? = null
    ) {
        val bindDialog =
            LayoutDialogDeleteFileBinding.inflate(LayoutInflater.from(context))
        dialogQuestion = DialogDeleteFile(context, bindDialog, R.style.StyleDialogDelete)
        dialogQuestion?.let {
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
            content.let { c -> it.binding.txtContent.text = c }
            title.let { t -> it.binding.txtTitle.text = t }
        }

        dialogQuestion?.listenerYes = {
            DialogLoadingUtils.showDialogWaiting(context, true)
            done?.invoke(true)
            dialogQuestion?.dismiss()
        }
        dialogQuestion?.listenerNo = {
            dialogQuestion?.dismiss()
        }
        dialogQuestion?.show()
    }

    // delete
    private fun deleteFile(path: String, activity: FragmentActivity, listener: (Boolean) -> Unit) {
        //        val index = binding.viewPager.currentItem
        MediaScannerConnection.scanFile(
            activity, arrayOf(path), null
        ) { _, uri ->
            try {
                uri?.let {
                    if (activity.contentResolver.delete(it, null, null) != -1) {
                        listener(true)
                    } else {
                        listener(false)
//                    toast(getString(R.string.app_error))
                    }
                }
            } catch (exception: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (exception is RecoverableSecurityException) {
                        val editPendingIntent =
                            MediaStore.createDeleteRequest(activity.contentResolver,
                                arrayOf(uri).map { it })
                        activity.startIntentSenderForResult(
                            editPendingIntent.intentSender, Vault.REQUEST_CODE_DELETE_FILE,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    }
                }
                listener(true)
            }
        }
    }

    //
    fun favoriteAudioDb(fileModel: AudioModel, isFavorite: Boolean, db: AppDatabase) {
        val file = db.serverDao().getAudioByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.isFavorite = isFavorite
        if (file == null) {
            db.serverDao().insertAudio(fileDb)
        } else {
            db.serverDao().updateAudio(fileDb)
        }
    }

    //
    fun favoriteImageDb(fileModel: ImageModel, isFavorite: Boolean, db: AppDatabase) {
        val file = db.serverDao().getImageByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.isFavorite = isFavorite
        if (file == null) {
            db.serverDao().insertImage(fileDb)
        } else {
            db.serverDao().updateImage(fileDb)
        }
    }

    //
    fun favoriteVideoDb(fileModel: VideoModel, isFavorite: Boolean, db: AppDatabase) {
        val file = db.serverDao().getVideoByPath(fileModel.path)
        val fileDb = file ?: fileModel
        fileDb.isFavorite = isFavorite
        if (file == null) {
            db.serverDao().insertVideo(fileDb)
        } else {
            db.serverDao().updateVideo(fileDb)
        }
    }

    // dialog create playlist

    fun showDialogCreatePlaylist(
        activity: FragmentActivity,
        oldName: String? = null,
        title: String? = null,
        save: (String) -> Unit
    ) {
        val bindDialog =
            LayoutDialogCreatePlaylistBinding.inflate(LayoutInflater.from(activity))
        bindDialog.txtTitle.text = title
        oldName?.let {
            bindDialog.edtName.setText(it)
            bindDialog.edtName.selectAll()

        }
        dialogCreatePlaylist = DialogCreatePlaylist(activity, bindDialog, R.style.StyleDialogDelete)
        dialogCreatePlaylist?.listenerYes = {
            save.invoke(bindDialog.edtName.text.toString())
            dialogCreatePlaylist?.dismiss()
        }
        dialogCreatePlaylist?.setOnDismissListener {
            KeyboardUtils.hideSoftKeyboardToggleSoft(activity)
        }
        dialogCreatePlaylist?.show()
    }
}