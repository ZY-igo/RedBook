/**
 * 文件说明：HomeMapper.kt
 * 作用：负责 Home Mapper 相关数据模型之间的转换。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.mapper

import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.ui.home.DiscoverCategoryBucket
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

class HomeMapper @Inject constructor() {

    fun fromDiscoverCategoryEntity(entity: DiscoverCategoryEntity): DiscoverCategory {
        return DiscoverCategory(
            id = entity.id,
            title = entity.title,
            bucket = entity.bucket,
            usesWaterfall = entity.usesWaterfall,
            isDefaultSelected = entity.isDefaultSelected
        )
    }

    fun fromFollowingUserEntity(entity: FollowingUserEntity): FollowingUser {
        return FollowingUser(
            id = entity.id,
            name = entity.name,
            subtitle = entity.subtitle,
            avatarUrl = null,
            avatarColorHex = entity.avatarColorHex,
            badge = entity.badge
        )
    }

    fun fromNoteItem(item: NoteItem): HomeDiscoverItem {
        val resolvedImageUrl = item.imageUrl ?: item.coverUrl
        return HomeDiscoverItem(
            id = item.id,
            title = item.title,
            author = item.author,
            avatarUrl = null,
            likeCount = item.likeCount.toString(),
            badge = item.description,
            coverLabel = item.description,
            coverHeightDp = item.coverHeightDp,
            mediaType = item.mediaType.toMediaType(),
            imageUrls = listOfNotNull(resolvedImageUrl),
            imageUrl = resolvedImageUrl,
            videoUrl = item.videoUrl,
            videoCoverUrl = item.coverUrl,
            startColorHex = "#D8082B",
            endColorHex = "#6D000F",
            avatarColorHex = "#FF8A9F"
        )
    }

    fun fromHomeCardEntity(entity: HomeCardEntity): HomeDiscoverItem {
        return HomeDiscoverItem(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            avatarUrl = null,
            likeCount = entity.likeCount,
            badge = entity.badge,
            coverLabel = entity.coverLabel,
            coverHeightDp = entity.coverHeightDp,
            mediaType = entity.mediaType.toMediaType(),
            imageUrls = listOfNotNull(entity.imageUrl),
            imageUrl = entity.imageUrl,
            videoUrl = entity.videoUrl,
            videoCoverUrl = entity.videoCoverUrl,
            startColorHex = entity.startColorHex,
            endColorHex = entity.endColorHex,
            avatarColorHex = entity.avatarColorHex
        )
    }

    fun toCategoryUiModel(category: DiscoverCategory): DiscoverCategoryItem {
        return DiscoverCategoryItem(
            id = category.id,
            title = category.title,
            bucket = category.bucket.toDiscoverCategoryBucket(),
            usesWaterfall = category.usesWaterfall,
            isDefaultSelected = category.isDefaultSelected
        )
    }

    fun toFollowingUserUiModel(user: FollowingUser): FollowingUserItem {
        return FollowingUserItem(
            id = user.id,
            name = user.name,
            subtitle = user.subtitle,
            avatarUrl = user.avatarUrl,
            avatarColorHex = user.avatarColorHex,
            badge = user.badge
        )
    }

    fun toHomeCardUiModel(item: HomeDiscoverItem): HomeCardItem {
        return HomeCardItem(
            id = item.id,
            title = item.title,
            author = item.author,
            avatarUrl = item.avatarUrl,
            likeCount = item.likeCount,
            badge = item.badge,
            coverLabel = item.coverLabel,
            coverHeightDp = item.coverHeightDp,
            mediaType = when (item.mediaType) {
                MediaType.VIDEO -> HomeCardItem.MediaType.VIDEO
                else -> HomeCardItem.MediaType.IMAGE
            },
            imageUrls = item.imageUrls,
            imageUrl = item.imageUrl,
            videoUrl = item.videoUrl,
            videoCoverUrl = item.videoCoverUrl,
            startColorHex = item.startColorHex,
            endColorHex = item.endColorHex,
            avatarColorHex = item.avatarColorHex
        )
    }

    private fun String.toMediaType(): MediaType {
        return when (uppercase()) {
            MediaType.VIDEO.name -> MediaType.VIDEO
            MediaType.TEXT.name -> MediaType.TEXT
            MediaType.LONG_FORM.name -> MediaType.LONG_FORM
            else -> MediaType.IMAGE
        }
    }

    private fun String.toDiscoverCategoryBucket(): DiscoverCategoryBucket {
        return runCatching { DiscoverCategoryBucket.valueOf(uppercase()) }
            .getOrDefault(DiscoverCategoryBucket.RECOMMEND)
    }
}
