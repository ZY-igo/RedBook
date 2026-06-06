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
    exportSchema = false
)
abstract class RedBookDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
}
