/**
 * 文件说明： RedBookDatabase.kt
 * 作用： 封装本地数据访问能力，例如 Room、预置资源读取和初始化逻辑。
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
    abstract fun listDao(): ListDao
}
