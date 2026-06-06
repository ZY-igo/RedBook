package com.zhengyang.redbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.PersonSuggestionEntity

@Database(
    entities = [
        NoteItem::class,
        DiscoverCategoryEntity::class,
        HomeCardEntity::class,
        FollowingUserEntity::class,
        MessageRowEntity::class,
        PersonSuggestionEntity::class,
        MyProfileEntity::class,
        InterestPersonEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RedBookDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
}
