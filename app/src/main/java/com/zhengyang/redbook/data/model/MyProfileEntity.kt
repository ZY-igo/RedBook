package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey
    val id: String = "self",
    val followingCount: String,
    val fansCount: String,
    val likesCount: String
)
