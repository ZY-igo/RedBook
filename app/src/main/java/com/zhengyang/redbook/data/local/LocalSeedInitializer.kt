/**
 * 文件说明： LocalSeedInitializer.kt
 * 作用： 封装本地数据访问能力，例如 Room、预置资源读取和初始化逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
