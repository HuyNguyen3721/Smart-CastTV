package com.ezstudio.smarttvcast.utils

import android.content.Context
import android.view.Window
import com.ezstudio.smarttvcast.dialog.DialogCast
import com.ezstudio.smarttvcast.dialog.DialogDownloading
import com.ezstudio.smarttvcast.dialog.DialogLoading

object DialogLoadingUtils {
    private var dialogWaiting: DialogLoading? = null
    private var dialogDownloading: DialogDownloading? = null
    private var dialogCast: DialogCast? = null

    fun showDialogWaiting(context: Context, isShowing: Boolean) {
        if (isShowing) {
            dialogWaiting = DialogLoading(context)
            dialogWaiting?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogWaiting?.show()
        } else {
            dialogWaiting?.dismiss()
            dialogWaiting = null
        }
    }

    fun showDialogDownloading(context: Context, isShowing: Boolean) {
        if (isShowing) {
            dialogDownloading = DialogDownloading(context)
            dialogDownloading?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogDownloading?.show()
        } else {
            if (dialogDownloading?.isShowing == true) {
                dialogDownloading?.dismiss()
            }
            dialogDownloading = null
        }
    }

    fun showDialogCast(context: Context, isShowing: Boolean, show: ((Boolean) -> Unit)) {
        if (isShowing) {
            dialogCast = DialogCast(context)
            dialogCast?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogCast?.listenerYes = {
                show.invoke(true)
            }
            dialogCast?.setOnDismissListener {
                show.invoke(false)
            }
            dialogCast?.show()
        } else {
            dialogCast?.dismiss()
            dialogCast = null
        }
    }
}