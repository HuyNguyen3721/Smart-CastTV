package com.ezstudio.smarttvcast.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class ConvertorList {
    @TypeConverter
    fun fromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toList(list: String): List<String> {
        return Gson().fromJson(list, object : TypeToken<List<String?>?>() {}.type)
    }
}