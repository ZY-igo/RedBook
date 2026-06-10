/**
 * 文件说明：RedBookDatabase.kt
 * 作用：定义应用本地 SQLite 数据库的结构和版本信息。
 * 备注：使用 Room 框架管理数据库，是所有本地数据表实体的统一入口。
 */
package com.zhengyang.redbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zhengyang.redbook.data.model.CollectionEntity
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.DraftEntity
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.PersonSuggestionEntity
import com.zhengyang.redbook.data.model.PlaybackProgressEntity
import com.zhengyang.redbook.data.model.SearchGuessEntity
import com.zhengyang.redbook.data.model.SearchHistoryEntity
import com.zhengyang.redbook.data.model.SearchResultEntity

/**
 * Room 数据库抽象类。
 *
 * 继承自 RoomDatabase，应用通过此类获取 DAO 实例进而操作数据库。
 * @TypeConverters 声明了本数据库使用的类型转换器集合，用于处理复杂类型（如列表、枚举等）的序列化与反序列化。
 *
 * @see RoomConverters
 */
@TypeConverters(RoomConverters::class)

/**
 * 数据库配置注解。
 *
 * entities：声明本数据库包含的所有数据表对应的实体类。
 * version：数据库版本号，用于迁移管理。当实体结构变更时需递增此版本号。
 * exportSchema：是否导出数据库 schema 文件，建议生产环境开启以支持更安全的迁移。
 */
@Database(
    entities = [
        NoteItem::class,
        DiscoverCategoryEntity::class,
        HomeCardEntity::class,
        FollowingUserEntity::class,
        MessageRowEntity::class,
        PersonSuggestionEntity::class,
        MyProfileEntity::class,
        InterestPersonEntity::class,
        SearchGuessEntity::class,
        SearchHistoryEntity::class,
        SearchResultEntity::class,
        CollectionEntity::class,
        PlaybackProgressEntity::class,
        DraftEntity::class
    ],
    version = 6,
    exportSchema = true
)

/**
 * 红书应用数据库基类。
 *
 * 提供了 listDao() 抽象方法，用于获取 ListDao 实例。
 * ListDao 封装了所有表的 CRUD 操作，是数据访问的统一入口。
 *
 * @see ListDao
 */
abstract class RedBookDatabase : RoomDatabase() {

    /**
     * 获取数据访问对象实例。
     *
     * @return ListDao 实例，提供所有数据库表的增删改查方法。
     */
    abstract fun listDao(): ListDao
}