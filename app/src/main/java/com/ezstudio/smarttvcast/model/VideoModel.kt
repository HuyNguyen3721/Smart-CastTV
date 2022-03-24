package com.ezstudio.smarttvcast.model

import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "video")
data class VideoModel(
    @ColumnInfo(name = "fileName")
    var fileName: String? =null,
    @ColumnInfo(name = "duration")
    var duration: Long = 0,
    @ColumnInfo(name = "path")
    var path: String? = null,
    @ColumnInfo(name = "created")
    var created: Long = 0,
    @ColumnInfo(name = "time_recent")
    var timeRecent: Long = 0,
    @ColumnInfo(name = "is_favorite")
    var isFavorite :Boolean = false,
    @ColumnInfo(name = "isSelected")
    var isSelected: Boolean = false,
    @ColumnInfo(name = "password")
    var password: String? = null,
    @Ignore
    var ads: ViewGroup? = null
){
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}