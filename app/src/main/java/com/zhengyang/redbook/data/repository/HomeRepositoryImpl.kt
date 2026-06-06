package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.LocalSeedInitializer
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.remote.ListContentApiService
import com.zhengyang.redbook.data.remote.mapper.RemoteNoteMapper
import com.zhengyang.redbook.ui.home.DiscoverCategoryBucket
import com.zhengyang.redbook.ui.home.DiscoverCategoryItem
import com.zhengyang.redbook.ui.home.FollowingUserItem
import com.zhengyang.redbook.ui.home.HomeCardItem
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val httpService: ListContentApiService,
    private val remoteNoteMapper: RemoteNoteMapper,
    private val localDataService: ListDao,
    private val localSeedInitializer: LocalSeedInitializer
) : HomeRepository {

    override suspend fun getListContent(): List<NoteItem> {
        val localItems = localDataService.getAll()
        return if (localItems.isNotEmpty()) {
            localItems
        } else {
            remoteNoteMapper.mapToDomain(httpService.getListContent()).also { remoteItems ->
                if (remoteItems.isNotEmpty()) {
                    localDataService.insertAll(remoteItems)
                }
            }
        }
    }

    override suspend fun getCategories(): List<DiscoverCategoryItem> {
        localSeedInitializer.ensureSeeded()
        return localDataService.getDiscoverCategories().map { it.toCategoryItem() }
    }

    override suspend fun getDiscoverItems(category: DiscoverCategoryItem): List<HomeCardItem> {
        localSeedInitializer.ensureSeeded()
        return if (category.bucket == DiscoverCategoryBucket.RECOMMEND) {
            getListContent().map { it.toHomeCardItem() }
        } else {
            localDataService.getHomeCardsBySection(category.id).map { it.toHomeCardItem() }
        }
    }

    override suspend fun getSuggestedFollowingUsers(): List<FollowingUserItem> {
        localSeedInitializer.ensureSeeded()
        return localDataService.getFollowingUsers().map { it.toFollowingUserItem() }
    }

    override suspend fun getFollowingFeedItems(): List<HomeCardItem> {
        localSeedInitializer.ensureSeeded()
        return localDataService.getHomeCardsBySection("following").map { it.toHomeCardItem() }
    }

    private fun NoteItem.toHomeCardItem(): HomeCardItem {
        return HomeCardItem(
            id = id,
            title = title,
            author = author,
            likeCount = likeCount.toString(),
            badge = description,
            coverLabel = description,
            coverHeightDp = coverHeightDp,
            mediaType = if (mediaType.equals("video", ignoreCase = true)) {
                HomeCardItem.MediaType.VIDEO
            } else {
                HomeCardItem.MediaType.IMAGE
            },
            imageUrls = listOfNotNull(imageUrl ?: coverUrl),
            imageUrl = imageUrl ?: coverUrl,
            videoUrl = videoUrl,
            videoCoverUrl = coverUrl,
            startColorHex = "#D8082B",
            endColorHex = "#6D000F",
            avatarColorHex = "#FF8A9F"
        )
    }

    private fun DiscoverCategoryEntity.toCategoryItem(): DiscoverCategoryItem {
        return DiscoverCategoryItem(
            id = id,
            title = title,
            bucket = DiscoverCategoryBucket.valueOf(bucket),
            usesWaterfall = usesWaterfall,
            isDefaultSelected = isDefaultSelected
        )
    }

    private fun HomeCardEntity.toHomeCardItem(): HomeCardItem {
        return HomeCardItem(
            id = id,
            title = title,
            author = author,
            likeCount = likeCount,
            badge = badge,
            coverLabel = coverLabel,
            coverHeightDp = coverHeightDp,
            mediaType = if (mediaType == "VIDEO") HomeCardItem.MediaType.VIDEO else HomeCardItem.MediaType.IMAGE,
            imageUrls = listOfNotNull(imageUrl),
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            videoCoverUrl = videoCoverUrl,
            startColorHex = startColorHex,
            endColorHex = endColorHex,
            avatarColorHex = avatarColorHex
        )
    }

    private fun FollowingUserEntity.toFollowingUserItem(): FollowingUserItem {
        return FollowingUserItem(
            id = id,
            name = name,
            subtitle = subtitle,
            avatarColorHex = avatarColorHex,
            badge = badge
        )
    }
}
