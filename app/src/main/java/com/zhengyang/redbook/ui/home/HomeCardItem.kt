package com.zhengyang.redbook.ui.home

data class HomeCardItem(
    val id: String,
    val title: String,
    val author: String,
    val likeCount: String,
    val badge: String,
    val coverLabel: String,
    val coverHeightDp: Int,
    val mediaType: MediaType = MediaType.IMAGE,
    val imageUrls: List<String> = emptyList(),
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val videoCoverUrl: String? = null,
    val startColorHex: String,
    val endColorHex: String,
    val avatarColorHex: String
) {
    enum class MediaType {
        IMAGE,
        VIDEO
    }
}
