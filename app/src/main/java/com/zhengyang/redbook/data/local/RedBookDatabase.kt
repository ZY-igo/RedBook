/**
 * 文件说明：RedBookDatabase.kt
 * 作用：声明应用数据库实例、数据表注册与版本信息。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zhengyang.redbook.data.model.CollectionEntity
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.DraftEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.PlaybackProgressEntity
import com.zhengyang.redbook.data.model.SearchHistoryEntity

/**
 * 应用数据库定义
 *
 * 统一声明当前应用使用的 Room 表结构、数据库版本和类型转换器，
 * 并向仓储层暴露访问本地缓存数据的 DAO 入口。
 */
@TypeConverters(RoomConverters::class)
@Database(
    entities = [
        NoteItem::class,
        DiscoverCategoryEntity::class,
        HomeCardEntity::class,
        MessageRowEntity::class,
        MyProfileEntity::class,
        SearchHistoryEntity::class,
        CollectionEntity::class,
        PlaybackProgressEntity::class,
        DraftEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class RedBookDatabase : RoomDatabase() {

    /**
     * 获取本地聚合数据访问对象
     *
     * @return 用于操作首页、消息、个人资料和搜索历史等本地表的 DAO 实例。
     */
    abstract fun listDao(): ListDao
}
