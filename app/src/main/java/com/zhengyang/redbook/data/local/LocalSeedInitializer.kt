package com.zhengyang.redbook.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSeedInitializer @Inject constructor(
    private val listDao: ListDao,
    private val assetSeedDataSource: AssetSeedDataSource
) {
    suspend fun ensureSeeded() {
        val seed = assetSeedDataSource.load()
        if (listDao.getDiscoverCategoryCount() == 0) {
            listDao.insertDiscoverCategories(seed.discoverCategories)
        }
        if (listDao.getHomeCardCount() == 0) {
            listDao.insertHomeCards(seed.homeCards)
        }
        if (listDao.getMessageRowCount() == 0) {
            listDao.insertMessageRows(seed.messageRows)
        }
        if (listDao.getMyProfileCount() == 0) {
            listDao.insertMyProfile(seed.myProfile)
        }
    }
}
