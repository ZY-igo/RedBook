package com.zhengyang.redbook.ui.home

/**
 * 首页卡片 UI 模型。
 */
data class HomeCardItem(
    val id: String,
    val title: String,
    val author: String,
    val avatarUrl: String? = null,
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
    val avatarColorHex: String,
    val isSkeleton: Boolean = false
) {
    fun primaryCoverUrl(): String? = imageUrl ?: videoCoverUrl

    enum class MediaType {
        IMAGE,
        VIDEO
    }
}
