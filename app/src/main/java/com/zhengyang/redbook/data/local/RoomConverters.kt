/**
 * 文件说明：RoomConverters.kt
 * 作用：集中声明数据库存取时使用的类型转换逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import androidx.room.TypeConverter
import com.zhengyang.redbook.data.model.NotificationType

/**
 * Room 类型转换器集合
 *
 * 负责处理列表、枚举等 SQLite 原生不支持的类型，
 * 保证实体在入库和出库时能够稳定完成序列化与反序列化。
 */
class RoomConverters {

    /**
     * 将字符串列表转换为单个数据库字段。
     *
     * @param value 需要入库的字符串列表。
     * @return 使用固定分隔符拼接后的字符串。
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(SEPARATOR)
    }

    /**
     * 将数据库中的拼接字符串还原为字符串列表。
     *
     * @param value 数据库中存储的原始字符串。
     * @return 还原后的字符串列表；空白字符串返回空列表。
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(SEPARATOR)
    }

    /**
     * 将通知类型枚举转换为可存储的字符串值。
     *
     * @param value 通知类型枚举。
     * @return 枚举名称字符串。
     */
    @TypeConverter
    fun fromNotificationType(value: NotificationType): String {
        return value.name
    }

    /**
     * 将数据库中的字符串恢复为通知类型枚举。
     *
     * @param value 枚举名称字符串。
     * @return 对应的通知类型枚举。
     * @throws IllegalArgumentException 当字符串与枚举值不匹配时抛出。
     */
    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return NotificationType.valueOf(value)
    }

    private companion object {
        /** 列表字段序列化时使用的分隔符。 */
        const val SEPARATOR = "||"
    }
}
