package com.omaster.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return value?.let { gson.fromJson(it, type) }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun fromCameraParams(params: com.omaster.app.model.CameraParams?): String? {
        return params?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCameraParams(json: String?): com.omaster.app.model.CameraParams? {
        val type = object : TypeToken<com.omaster.app.model.CameraParams>() {}.type
        return json?.let { gson.fromJson(it, type) }
    }
}
