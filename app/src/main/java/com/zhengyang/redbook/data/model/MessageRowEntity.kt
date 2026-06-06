package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_row")
data class MessageRowEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val backgroundRes: Int,
    val iconRes: Int?,
    val avatarText: String?,
    val iconSizeDp: Int,
    val showsVerifiedBadge: Boolean,
    val showsRedDot: Boolean,
    val sortOrder: Int
)
