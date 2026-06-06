package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.LocalSeedInitializer
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileStats
import javax.inject.Inject

class MyRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val localSeedInitializer: LocalSeedInitializer
) : MyRepository {
    override suspend fun getProfileStats(): MyProfileStats {
        localSeedInitializer.ensureSeeded()
        val profile = requireNotNull(listDao.getMyProfile())
        return MyProfileStats(
            followingCount = profile.followingCount,
            fansCount = profile.fansCount,
            likesCount = profile.likesCount
        )
    }

    override suspend fun getInterestPeople(): List<InterestPersonItem> {
        localSeedInitializer.ensureSeeded()
        return listDao.getInterestPeople().map { it.toInterestPersonItem() }
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
