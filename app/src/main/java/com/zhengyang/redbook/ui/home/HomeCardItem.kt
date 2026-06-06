/**
 * 文件说明： HomeCardItem.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
