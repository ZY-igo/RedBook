package com.zhengyang.redbook.data.local

import androidx.room.TypeConverter
import com.zhengyang.redbook.data.model.NotificationType

class RoomConverters {

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(SEPARATOR)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(SEPARATOR)
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String {
        return value.name
    }

    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return NotificationType.valueOf(value)
    }

    private companion object {
        const val SEPARATOR = "||"
    }
}
