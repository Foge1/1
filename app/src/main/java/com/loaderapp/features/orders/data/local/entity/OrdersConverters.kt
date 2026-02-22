package com.loaderapp.features.orders.data.local.entity

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class OrdersConverters {

    @TypeConverter
    fun fromTags(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toTags(value: String): List<String> {
        val json = JSONArray(value)
        return buildList(json.length()) {
            repeat(json.length()) { index -> add(json.getString(index)) }
        }
    }

    @TypeConverter
    fun fromMeta(value: Map<String, String>): String = JSONObject(value).toString()

    @TypeConverter
    fun toMeta(value: String): Map<String, String> {
        val json = JSONObject(value)
        return buildMap(json.length()) {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, json.optString(key))
            }
        }
    }
}
