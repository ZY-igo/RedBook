/**
 * 文件说明：HomeDiscoverItemDisplayPolicy.kt
 * 作用：定义 Home Discover Item Display Policy 场景使用的规则判断与策略逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import javax.inject.Inject

class HomeDiscoverItemDisplayPolicy @Inject constructor() {

    fun isDisplayable(item: HomeDiscoverItem): Boolean {
        if (item.id.isBlank() || item.title.isBlank() || item.author.isBlank()) {
            return false
        }
        return when (item.mediaType) {
            MediaType.VIDEO -> !item.videoUrl.isNullOrBlank() || !item.videoCoverUrl.isNullOrBlank()
            else -> item.imageUrls.isNotEmpty() || !item.imageUrl.isNullOrBlank()
        }
    }
}
