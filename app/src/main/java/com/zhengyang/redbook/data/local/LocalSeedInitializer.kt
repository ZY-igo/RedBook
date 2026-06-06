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
        if (listDao.getFollowingUserCount() == 0) {
            listDao.insertFollowingUsers(seed.followingUsers)
        }
        if (listDao.getMessageRowCount() == 0) {
            listDao.insertMessageRows(seed.messageRows)
        }
        if (listDao.getPersonSuggestionCount() == 0) {
            listDao.insertPersonSuggestions(seed.personSuggestions)
        }
        if (listDao.getMyProfileCount() == 0) {
            listDao.insertMyProfile(seed.myProfile)
        }
        if (listDao.getInterestPersonCount() == 0) {
            listDao.insertInterestPeople(seed.interestPeople)
        }
    }
}
