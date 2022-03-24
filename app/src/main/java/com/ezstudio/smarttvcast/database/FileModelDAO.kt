package com.ezstudio.smarttvcast.database

import androidx.room.*
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel

@Dao
interface FileModelDAO {

    @Query("SELECT * FROM video WHERE path =:path")
    fun getVideoByPath(path: String?): VideoModel?

    @Query("SELECT * FROM audio WHERE path =:path")
    fun getAudioByPath(path: String?): AudioModel?

    @Query("SELECT * FROM image WHERE path =:path")
    fun getImageByPath(path: String?): ImageModel?

    // recent
    @get:Query("SELECT * FROM video WHERE time_recent > 0 ORDER BY time_recent DESC LIMIT 20")
    val getRecentVideo: MutableList<VideoModel>

    @get:Query("SELECT * FROM audio WHERE time_recent > 0 ORDER BY time_recent DESC LIMIT 20")
    val getRecentAudio: MutableList<AudioModel>

    @get:Query("SELECT * FROM image WHERE time_recent > 0 ORDER BY time_recent DESC LIMIT 20")
    val getRecentImage: MutableList<ImageModel>

    // favorite
    @get:Query("SELECT * FROM video WHERE is_favorite = 1 ")
    val getFavoriteVideo: MutableList<VideoModel>

    @get:Query("SELECT * FROM audio WHERE is_favorite = 1 ")
    val getFavoriteAudio: MutableList<AudioModel>

    @get:Query("SELECT * FROM image WHERE is_favorite = 1 ")
    val getFavoriteImage: MutableList<ImageModel>

    // video
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideo(fileModel: VideoModel?)

    @Update
    fun updateVideo(fileInfo: VideoModel?)

    @Delete
    fun deleteVideo(fileModel: VideoModel?)

    //audio
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAudio(fileModel: AudioModel?)

    @Update
    fun updateAudio(fileInfo: AudioModel?)

    @Delete
    fun deleteAudio(fileModel: AudioModel?)

    //image
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(fileModel: ImageModel?)

    @Update
    fun updateImage(fileInfo: ImageModel?)

    @Delete
    fun deleteImage(fileModel: ImageModel?)

    // playlist
    @get:Query("SELECT * FROM playlist")
    val getPlaylist: MutableList<PlayListModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlayList(playListModel: PlayListModel)

    @Delete
    fun deletePlaylist(playListModel: PlayListModel)

    @Query("SELECT * FROM playlist WHERE name =:name")
    fun getPlayListByName(name: String?): PlayListModel?


}