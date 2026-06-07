package com.zhengyang.redbook.ui.home

object HomeSkeletonFactory {
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
