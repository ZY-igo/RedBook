package com.zhengyang.redbook.data.model

data class PersonSuggestionEntity(
    val id: String,
    val avatarText: String,
    val name: String,
    val subtitle: String,
    val avatarBackgroundRes: Int,
    val sortOrder: Int
)
