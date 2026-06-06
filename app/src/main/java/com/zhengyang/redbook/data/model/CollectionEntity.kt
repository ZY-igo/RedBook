package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val noteId: String,
    val noteTitle: String,
    val noteCoverUrl: String,
    val noteAuthor: String,
    val collectedAt: Long
)
