package com.example.vesta.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.datetime.Instant

class Converters {
    
    @TypeConverter
    fun fromInstant(instant: Instant?): String? {
        return instant?.toString()
    }
    
    @TypeConverter
    fun toInstant(instantString: String?): Instant? {
        return instantString?.let { Instant.parse(it) }
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
}
