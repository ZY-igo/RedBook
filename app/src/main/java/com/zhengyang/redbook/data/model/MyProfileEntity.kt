package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String?,
    val followingCount: Int,
    val fansCount: Int,
    val likesCount: Int,
    val noteCount: Int
)
