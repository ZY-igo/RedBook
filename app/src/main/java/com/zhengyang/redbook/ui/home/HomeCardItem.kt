package com.zhengyang.redbook.ui.home

/**
 * 首页卡片 UI 模型。
 *
 * @property id 卡片唯一标识。
 * @property title 卡片标题。
 * @property author 作者名称。
 * @property avatarUrl 作者头像地址。
 * @property likeCount 点赞数字文案。
 * @property badge 卡片角标文案。
 * @property coverLabel 封面补充标签。
 * @property coverHeightDp 封面建议高度，单位为 dp。
 * @property mediaType 卡片媒体类型。
 * @property imageUrls 多图内容的图片列表。
 * @property imageUrl 主图片地址。
 * @property videoUrl 视频地址。
 * @property videoCoverUrl 视频封面地址。
 * @property startColorHex 封面渐变起始色。
 * @property endColorHex 封面渐变结束色。
 * @property avatarColorHex 头像兜底背景色。
 * @property isSkeleton 当前卡片是否是骨架屏占位项。
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
    /**
     * 首页卡片媒体类型。
     */
    enum class MediaType {
        /** 图文内容。 */
        IMAGE,

        /** 视频内容。 */
        VIDEO
    }
}
