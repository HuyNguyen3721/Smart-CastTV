package com.ezstudio.smarttvcast.utils

import android.app.Activity
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ezstudio.smarttvcast.R
import com.ezstudio.smarttvcast.database.AppDatabase
import com.ezstudio.smarttvcast.model.PlayListModel

object PlaylistUtils {

    fun createANewPlayList(data : PlayListModel, db: AppDatabase) {
        db.serverDao().insertPlayList(data)
    }

    fun isDuplicateName(
        name: String,
        db: AppDatabase,
        activity: FragmentActivity
    ): Boolean {
        val list = db.serverDao().getPlaylist
        for (item in list) {
            if (item.name == name) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.name_already_exists),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }
        return false
    }
}