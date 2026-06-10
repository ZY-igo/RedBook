package com.zhengyang.redbook.ui.home

/**
 * 首页骨架屏数据工厂。
 *
 * 统一生成“发现”和“关注”区域在加载中的占位卡片。
 */
object HomeSkeletonFactory {

    /**
     * 生成“发现”流骨架卡片。
     *
     * @param count 需要生成的骨架数量。
     * @return 用于占位的“发现”卡片列表。
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
     * 生成“关注”流骨架卡片。
     *
     * @param count 需要生成的骨架数量。
     * @return 用于占位的“关注”卡片列表。
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
