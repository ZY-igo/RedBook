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
