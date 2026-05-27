package com.omaster.app.model.entities

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorProfile

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCameraParams(params: CameraParams?): String? {
        return gson.toJson(params)
    }

    @TypeConverter
    fun toCameraParams(json: String?): CameraParams? {
        val type = object : TypeToken<CameraParams>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toFloatList(json: String?): List<Float>? {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(json, type)
    }
}
