package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import javax.inject.Inject

/**
 * 首页“发现”卡片展示策略。
 *
 * 用于判断一条发现流数据是否具备最基本的展示条件。
 */
class HomeDiscoverItemDisplayPolicy @Inject constructor() {

    /**
     * 判断某条发现流数据是否允许展示。
     *
     * @param item 待判断的数据项。
     * @return `true` 表示该项可正常渲染。
     */
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
