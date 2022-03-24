package com.ezstudio.smarttvcast.utils

import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.VideoModel
import com.ezteam.baseproject.activity.BaseActivity
import java.text.SimpleDateFormat

object Utils {
    const val TYPE_VIDEO = "TYPE_VIDEO"
    const val TYPE_AUDIO = "TYPE_AUDIO"
    const val TYPE_IMAGE = "TYPE_IMAGE"
    const val FIRST_INTRO = "FIRST_INTRO"

    fun customStatusBar(window: Window, activity: BaseActivity<*>) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        activity.setWindowFlag(
            activity,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            false
        )
        window.statusBarColor = Color.TRANSPARENT
    }

    fun formatDurationLong(duration: Long): String {
        return if (duration >= 3600000)
            "${SimpleDateFormat("hh:mm:ss").format(duration)}"
        else
            "${SimpleDateFormat("mm:ss").format(duration)}"
    }
    fun formatDurationInt(duration: Int): String {
        return if (duration >= 3600000)
            "${SimpleDateFormat("hh:mm:ss").format(duration)}"
        else
            "${SimpleDateFormat("mm:ss").format(duration)}"
    }

    fun isFavorite(model: Any, db: AppDatabase): Boolean {
        val server = db.serverDao()
        when (model) {
            is VideoModel -> {
                return server.getVideoByPath(model.path)?.isFavorite ?: false
            }
            is ImageModel -> {
                return server.getImageByPath(model.path)?.isFavorite ?: false
            }
            is AudioModel -> {
                return server.getAudioByPath(model.path)?.isFavorite ?: false
            }
        }
        return false

    }
}