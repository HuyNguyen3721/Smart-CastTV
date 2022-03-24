package com.ezstudio.smarttvcast.model

import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "playlist")
data class PlayListModel(

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "list_music")
    var listMusic: List<String>
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @Ignore
    var ads: ViewGroup? = null
}