package com.zhengyang.redbook.data.remote.model

data class RemoteNoteDto(
    val id: String?,
    val title: String?,
    val description: String?,
    val likeCount: Int?,
    val author: String?,
    val mediaType: String?,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val coverHeightDp: Int? = null
)
