package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_suggestion")
data class PersonSuggestionEntity(
    @PrimaryKey
    val id: String,
    val avatarText: String,
    val name: String,
    val subtitle: String,
    val avatarBackgroundRes: Int,
    val sortOrder: Int
)
