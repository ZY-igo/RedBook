/**
 * 文件说明： MyRepositoryImpl.kt
 * 作用： 协调不同数据源，并向上层提供统一的仓储实现。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.LocalSeedInitializer
import com.zhengyang.redbook.data.local.AssetSeedDataSource
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileStats
import javax.inject.Inject

class MyRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val localSeedInitializer: LocalSeedInitializer,
    private val assetSeedDataSource: AssetSeedDataSource
) : MyRepository {
    override suspend fun getProfileStats(): MyProfileStats {
        localSeedInitializer.ensureSeeded()
        val profile = requireNotNull(listDao.getMyProfile())
        return MyProfileStats(
            followingCount = profile.followingCount.toString(),
            fansCount = profile.fansCount.toString(),
            likesCount = profile.likesCount.toString()
        )
    }

    override suspend fun getInterestPeople(): List<InterestPersonItem> {
        return assetSeedDataSource.load().interestPeople.map { it.toInterestPersonItem() }
    }

    private fun InterestPersonEntity.toInterestPersonItem(): InterestPersonItem {
        return InterestPersonItem(
            id = id,
            avatarText = avatarText,
            name = name,
            fansText = fansText,
            avatarBackgroundRes = avatarBackgroundRes
        )
    }
}
