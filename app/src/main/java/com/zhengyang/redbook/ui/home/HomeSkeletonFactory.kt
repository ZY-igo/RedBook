/**
 * 文件说明：HomeSkeletonFactory.kt
 * 作用：集中创建首页骨架屏占位数据。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页骨架屏数据工厂
 *
 * 负责为首页不同内容区块生成占位卡片，
 * 以便列表在首屏加载和刷新期间仍保持稳定布局。
 */
object HomeSkeletonFactory {

    /**
     * 生成发现流骨架数据。
     *
     * @param count 需要生成的骨架卡片数量。
     * @return 发现流骨架卡片列表。
     */
    fun discover(count: Int = 6): List<HomeCardItem> {
        return List(count) { index ->
            HomeCardItem(
                id = "discover-skeleton-$index",
                title = "",
                author = "",
                likeCount = "",
                badge = "",
                coverLabel = "",
                coverHeightDp = if (index % 2 == 0) 244 else 212,
                startColorHex = "#F2F3F5",
                endColorHex = "#F2F3F5",
                avatarColorHex = "#E7E9EE",
                isSkeleton = true
            )
        }
    }

    /**
     * 生成关注流骨架数据。
     *
     * @param count 需要生成的骨架卡片数量。
     * @return 关注流骨架卡片列表。
     */
    fun following(count: Int = 4): List<HomeCardItem> {
        return List(count) { index ->
            HomeCardItem(
                id = "following-skeleton-$index",
                title = "",
                author = "",
                likeCount = "",
                badge = "",
                coverLabel = "",
                coverHeightDp = if (index % 2 == 0) 220 else 236,
                startColorHex = "#F2F3F5",
                endColorHex = "#F2F3F5",
                avatarColorHex = "#E7E9EE",
                isSkeleton = true
            )
        }
    }
}
