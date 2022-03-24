package com.ezstudio.smarttvcast.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ezstudio.smarttvcast.converters.ConverterBitmap
import com.ezstudio.smarttvcast.converters.ConvertorList
import com.ezstudio.smarttvcast.model.AudioModel
import com.ezstudio.smarttvcast.model.ImageModel
import com.ezstudio.smarttvcast.model.PlayListModel
import com.ezstudio.smarttvcast.model.VideoModel

@Database(
    entities = [AudioModel::class, ImageModel::class, VideoModel::class, PlayListModel::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ConverterBitmap::class, ConvertorList::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): FileModelDAO

    companion object {
        private const val DB_NAME = "smart_cast_tv"
        fun getInstance(context: Context): AppDatabase {
            return Room
                .databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                    }
                })
                .build()
        }
    }
}