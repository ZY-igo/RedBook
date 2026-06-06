package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "following_user")
data class FollowingUserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarColorHex: String,
    val badge: String?,
    val sortOrder: Int
)
